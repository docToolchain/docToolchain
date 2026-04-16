// Driver: convert a partial Confluence page tree (a root page and all its
// descendants) to AsciiDoc, fetching pages and attachments directly via the
// Confluence REST API v1.
//
// Shared conversion logic lives in scripts/confluenceConverter.groovy.
// This file is responsible for:
//   - reading configuration (base URL, credentials, rootPageId/rootPageTitle, destDir)
//   - walking the page tree under rootPageId via GET /rest/api/content/{id}/child/page
//   - fetching each page's storage body via GET /rest/api/content/{id}?expand=body.storage
//   - listing and downloading attachments via GET /rest/api/content/{id}/child/attachment
//   - building the pages/attachments/space/users maps expected by the shared converter
//   - delegating each page's on-disk write to writePage() from the converter
//
// Unlike the XML-export driver, this task does NOT require Space Admin rights on
// Confluence - ordinary read access to the chosen root page and its descendants
// is enough. It also does not require a pre-prepared XML export.

import groovy.json.JsonSlurper

// --- locate and evaluate the shared converter ---
def converterFile = binding.hasVariable('projectDir')
        ? new File(projectDir as File, 'scripts/confluenceConverter.groovy')
        : new File('scripts/confluenceConverter.groovy')
if (!converterFile.exists()) {
    throw new FileNotFoundException("Cannot locate confluenceConverter.groovy (looked at ${converterFile.absolutePath})")
}
evaluate(converterFile)

// --- configuration resolution ---
// The Gradle task sets `args` like ["destDir=...", "rootPageId=..."]. Any
// remaining settings are pulled from `config.confluence` (resolved by the
// Gradle task wrapper before we get here).

Map parseApiArgs(String[] args) {
    def parsed = [:]
    args.each {
        def idx = it.indexOf('=')
        if (idx >= 0) parsed[it.substring(0, idx).trim()] = it.substring(idx + 1).trim()
    }
    return parsed
}

def apiArgs = parseApiArgs(args)

String baseUrl = (config.confluence.api ?: '') as String
if (!baseUrl) {
    throw new IllegalStateException("config.confluence.api is not set - configure the Confluence base URL (e.g. https://<site>.atlassian.net/wiki)")
}
baseUrl = baseUrl.replaceAll('/+$', '')

String bearerToken = (config.confluence.bearerToken ?: '') as String
String basicCredentials = (config.confluence.credentials ?: '') as String
if (!bearerToken && !basicCredentials) {
    throw new IllegalStateException("No Confluence credentials configured. Set -PconfluenceBearerToken=... OR -PconfluenceUser=... -PconfluencePass=...")
}

String destDirPath = apiArgs.destDir ?: (config.confluence.export.destDir as String)
if (!destDirPath) {
    throw new IllegalStateException("destDir is not configured (set confluence.export.destDir or pass -Pconfluence.export.destDir=...)")
}
File destDir = new File(destDirPath)
destDir.mkdirs()

String rootPageId = apiArgs.rootPageId ?: null
String rootPageTitle = apiArgs.rootPageTitle ?: null
String spaceKey = apiArgs.spaceKey ?: null
if (!rootPageId && !rootPageTitle) {
    throw new IllegalStateException("rootPageId is required (set confluence.export.api.rootPageId or pass -Pconfluence.export.api.rootPageId=...). Alternatively pass rootPageTitle + spaceKey.")
}

int pageLimit = (apiArgs.pageLimit ?: config.confluence.pageLimit ?: 100) as int
double rateLimitPerSecond = (config.confluence.rateLimit ?: 10) as double
boolean downloadAttachments = (apiArgs.downloadAttachments ?: 'true').toString().toBoolean()
boolean saveRawXhtml         = (apiArgs.saveRawXhtml ?: 'false').toString().toBoolean()
// Override the shared converter's default (true). This must be set on the
// binding (no `def`) so the fixBody closure in confluenceConverter.groovy sees it.
stripChapterNumbering = (apiArgs.stripChapterNumbering ?: 'true').toString().toBoolean()

println "API:                   ${baseUrl}"
println "destDir:               ${destDir.canonicalPath}"
println "rootPageId:            ${rootPageId ?: '(resolving from title)'}"
println "rootTitle:             ${rootPageTitle ?: '(not set)'}"
println "spaceKey:              ${spaceKey ?: '(not set)'}"
println "saveRawXhtml:          ${saveRawXhtml}"
println "stripChapterNumbering: ${stripChapterNumbering}"

// --- tiny authenticated REST helper (stdlib only, no new deps) ---
// Throttles to config.confluence.rateLimit (default 10/s) across all calls.
def lastRequestNanos = [0L]  // boxed so the closure can mutate it
def minIntervalNanos = (1_000_000_000L / rateLimitPerSecond) as long

def authHeader = bearerToken
        ? "Bearer ${bearerToken}"
        : "Basic ${basicCredentials}"

def throttle = {
    long now = System.nanoTime()
    long delta = now - lastRequestNanos[0]
    if (delta < minIntervalNanos) {
        Thread.sleep(((minIntervalNanos - delta) / 1_000_000L) as long)
    }
    lastRequestNanos[0] = System.nanoTime()
}

// Build an absolute URL. Relative inputs starting with '/' are appended to baseUrl;
// anything starting with 'http' is used verbatim (e.g. attachment download links
// returned by the API may be absolute or site-relative).
def resolveUrl = { String pathOrUrl, Map<String, String> query = [:] ->
    String absolute
    if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
        absolute = pathOrUrl
    } else if (pathOrUrl.startsWith('/')) {
        // some installations return download URLs that already include the context
        // (e.g. /wiki/download/...); strip a duplicated context defensively
        absolute = baseUrl + pathOrUrl
    } else {
        absolute = baseUrl + '/' + pathOrUrl
    }
    if (query) {
        def qs = query.collect { k, v -> URLEncoder.encode(k, 'UTF-8') + '=' + URLEncoder.encode(v.toString(), 'UTF-8') }.join('&')
        absolute += (absolute.contains('?') ? '&' : '?') + qs
    }
    return new URL(absolute)
}

def apiPath = { String suffix -> "${baseUrl}/rest/api${suffix}" }

def restGet = { String url ->
    throttle()
    def conn = (new URL(url)).openConnection() as HttpURLConnection
    conn.setRequestProperty('Authorization', authHeader)
    conn.setRequestProperty('X-Atlassian-Token', 'no-check')
    conn.setRequestProperty('Accept', 'application/json')
    conn.setRequestProperty('User-Agent', 'docToolchain-exportConfluenceFromApi')
    int code
    try {
        code = conn.responseCode
    } catch (IOException e) {
        throw new RuntimeException("GET ${url} failed: ${e.message}", e)
    }
    if (code == 429 || code >= 500) {
        // one simple retry with backoff honouring Retry-After
        def retryAfter = conn.getHeaderField('Retry-After')
        long waitMs = retryAfter ? (retryAfter.isNumber() ? (retryAfter as long) * 1000L : 2000L) : 2000L
        println "WARNING: HTTP ${code} on ${url}; retrying after ${waitMs}ms"
        Thread.sleep(waitMs)
        conn.disconnect()
        conn = (new URL(url)).openConnection() as HttpURLConnection
        conn.setRequestProperty('Authorization', authHeader)
        conn.setRequestProperty('X-Atlassian-Token', 'no-check')
        conn.setRequestProperty('Accept', 'application/json')
        code = conn.responseCode
    }
    if (code < 200 || code > 299) {
        def body = conn.errorStream ? conn.errorStream.getText('UTF-8') : conn.responseMessage
        conn.disconnect()
        throw new RuntimeException("HTTP ${code} ${conn.responseMessage} from ${url}: ${body}")
    }
    def text = conn.inputStream.getText('UTF-8')
    conn.disconnect()
    return new JsonSlurper().parseText(text)
}

def downloadBinary = { String urlOrPath, File destFile ->
    throttle()
    def url = resolveUrl(urlOrPath)
    def conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty('Authorization', authHeader)
    conn.setRequestProperty('X-Atlassian-Token', 'no-check')
    conn.setRequestProperty('User-Agent', 'docToolchain-exportConfluenceFromApi')
    conn.instanceFollowRedirects = true
    int code = conn.responseCode
    // Confluence Cloud often redirects attachment downloads to a pre-signed S3 URL;
    // HttpURLConnection follows same-protocol redirects automatically but drops the
    // Authorization header across hosts (which is what we want for S3 signed URLs).
    if (code < 200 || code > 299) {
        def body = conn.errorStream ? conn.errorStream.getText('UTF-8') : conn.responseMessage
        conn.disconnect()
        throw new RuntimeException("Download ${url} failed: HTTP ${code} - ${body}")
    }
    destFile.parentFile?.mkdirs()
    destFile.withOutputStream { os -> conn.inputStream.with { is -> os << is } }
    conn.disconnect()
}

// --- tree walk ---

// Resolve rootPageTitle -> rootPageId via CQL if needed.
if (!rootPageId && rootPageTitle) {
    def cql = "type=page AND title=\"${rootPageTitle.replace('"', '\\"')}\""
    if (spaceKey) cql += " AND space.key=\"${spaceKey}\""
    def searchUrl = apiPath("/content/search") + "?cql=${URLEncoder.encode(cql, 'UTF-8')}&limit=2"
    def searchResult = restGet(searchUrl)
    def hits = searchResult.results ?: []
    if (hits.isEmpty()) {
        throw new IllegalStateException("No page found with title '${rootPageTitle}'${spaceKey ? " in space '${spaceKey}'" : ''}")
    }
    if (hits.size() > 1) {
        throw new IllegalStateException("Multiple pages found with title '${rootPageTitle}' (ids: ${hits.collect { it.id }}); please pass rootPageId instead, or set spaceKey")
    }
    rootPageId = hits[0].id as String
    println "Resolved rootPageTitle '${rootPageTitle}' to rootPageId '${rootPageId}'"
}

// Fetch a single page (with body.storage, version metadata, space, ancestors,
// and contributor history so the `contributors` macro can render names).
def fetchPage = { String id ->
    def expand = [
            'body.storage',
            'version',
            'space',
            'ancestors',
            'history.createdBy',
            'history.contributors.publishers.users'
    ].join(',')
    def url = apiPath("/content/${id}") + "?expand=${expand}"
    return restGet(url)
}

// Fetch one "page" of direct children, returning [results, nextStart, hasMore].
def fetchDirectChildrenOnce = { String parentId, int start ->
    def url = apiPath("/content/${parentId}/child/page") + "?limit=${pageLimit}&start=${start}"
    def r = restGet(url)
    def results = r.results ?: []
    // v1 REST sometimes exposes next via _links.next, but it's safer to compare sizes
    return [results, start + results.size(), results.size() >= pageLimit]
}

def fetchAllDirectChildren = { String parentId ->
    def all = []
    int start = 0
    boolean more = true
    while (more) {
        def (chunk, nextStart, hasMore) = fetchDirectChildrenOnce(parentId, start)
        all.addAll(chunk)
        start = nextStart
        more = hasMore
    }
    return all
}

def fetchAttachmentsForPage = { String pageId ->
    def all = []
    int start = 0
    boolean more = true
    while (more) {
        def url = apiPath("/content/${pageId}/child/attachment") + "?limit=${pageLimit}&start=${start}&expand=version"
        def r = restGet(url)
        def chunk = r.results ?: []
        all.addAll(chunk)
        start += chunk.size()
        more = chunk.size() >= pageLimit
    }
    return all
}

// --- walk the tree breadth-first, collecting pages + attachments ---

println "\nfetching root page ${rootPageId}..."
def rootPage = fetchPage(rootPageId)
if (!rootPage) throw new IllegalStateException("Root page ${rootPageId} not found or not readable")
def spaceInfo = rootPage.space ?: [:]

Map pages = [:]
Map attachments = [:]
Map users = [:]          // API driver doesn't currently resolve user profiles
Map space = [
        name    : spaceInfo.name ?: '',
        key     : spaceInfo.key ?: '',
        homePage: rootPageId
]

// queue entries: [id, parentId-as-String-or-0, position]
def queue = new LinkedList()
queue << [id: rootPageId, parentId: 0, position: 0]
Map<String, String> bodies = [:]

// avoid cycles (shouldn't happen in Confluence but defensive)
Set<String> visited = [] as Set

while (!queue.isEmpty()) {
    def entry = queue.poll()
    String pid = entry.id
    if (!visited.add(pid)) continue

    def pageData = (pid == rootPageId) ? rootPage : fetchPage(pid)
    if (!pageData) {
        println "WARNING: could not fetch page ${pid}, skipping"
        continue
    }
    String title = pageData.title
    String filename = sanitizeFilename(title)
    String body = pageData.body?.storage?.value ?: ''
    // Collect display names for the `contributors` macro. createdBy first,
    // then any additional publishers (de-duplicated, original order preserved).
    def contribNames = []
    def creator = pageData.history?.createdBy?.displayName
    if (creator) contribNames << creator
    pageData.history?.contributors?.publishers?.users?.each { user ->
        def dn = user?.displayName
        if (dn && !contribNames.contains(dn)) contribNames << dn
    }
    pages[pid] = [
            title       : title,
            parentId    : entry.parentId,
            filename    : filename,
            position    : entry.position.toString(),
            status      : 'current',
            contributors: contribNames
    ]
    bodies[pid] = body
    println "page: ${pid} - ${title}"

    // attachments for this page
    def attList = fetchAttachmentsForPage(pid)
    attList.each { att ->
        String aid = att.id as String
        String aTitle = att.title as String
        String version = (att.version?.number ?: 1).toString()
        String downloadLink = att._links?.download ?: ''
        attachments[aid] = [
                filename   : aTitle,
                id         : aid,
                version    : version,
                pageId     : pid,
                originalId : '',
                downloadUrl: downloadLink
        ]
    }

    // direct children -> enqueue
    def children = fetchAllDirectChildren(pid)
    children.eachWithIndex { child, idx ->
        queue << [id: child.id as String, parentId: pid, position: idx]
    }
}

println "\nfetched ${pages.size()} pages and ${attachments.size()} attachments"

// --- download attachments to images/<folderStructure>/<version>_<filename> ---
if (downloadAttachments && !attachments.isEmpty()) {
    println "\ndownloading attachments..."
    attachments.each { attachmentId, attachment ->
        def folderStructure = getFolderStructure(pages, attachment.pageId)
        def deepFilename = folderStructure.join("/") + "/" + attachment.version + "_" + (attachment.filename.replaceAll(":", "_"))
        def destFile = new File(new File(destDir, 'images'), deepFilename)
        if (!attachment.downloadUrl) {
            println "  [skip, no download link] ${attachment.filename}"
            return
        }
        try {
            downloadBinary(attachment.downloadUrl as String, destFile)
        } catch (Exception e) {
            println "  [error] ${attachment.filename}: ${e.message}"
        }
    }
}

// --- write pages ---
lucidInfoFile = new File(destDir, "lucidinfos.txt")
lucidInfoFile.write("", 'utf-8')

// precompute children-by-parent for writePage
def childrenByParent = [:].withDefault { [] }
pages.each { pid, info ->
    if (info.parentId && info.parentId != 0) {
        childrenByParent[info.parentId.toString()] << pid
    }
}

def allUnknownTags = [] as Set
pages.each { pid, info ->
    // optionally save the raw Confluence storage-format XHTML for later
    // inspection / diagnosing of conversion artifacts.
    if (saveRawXhtml) {
        def folderStructure = getFolderStructure(pages, pid)
        File rawDir = folderStructure.size() >= 1
                ? new File(destDir, folderStructure.join("/"))
                : destDir
        rawDir.mkdirs()
        new File(rawDir, "${info.filename}.xhtml").write(bodies[pid] ?: '', 'utf-8')
    }
    def childIds = childrenByParent[pid.toString()] ?: []
    def uTags = writePage(pid, bodies[pid] ?: '', childIds, pages, attachments, space, users, destDir)
    if (uTags) allUnknownTags.addAll(uTags)
}

new File(destDir, '_config.adoc').write("""
++++
<style>
div.ulist ul {
margin-left: 1em !important;
}
</style>
++++

include::_menu.adoc[]

++++
<!-- endtoc -->
++++

""", 'utf-8')

new File(destDir, '_menu.adoc').write(createMenu(pages, 0), 'utf-8')

println ""
println "pages converted: ${pages.size()}"
println "unknown tags:    ${allUnknownTags}"
unknownTagsStats.sort { a, b -> b.value.size() <=> a.value.size() }.each { tag, tagpages ->
    println tagpages.size().toString().padLeft(5) + " : " + tag
    tagpages.each { pid ->
        println " " * 10 + "- " + pages[pid]?.title
    }
}
