// Shared Confluence -> AsciiDoc converter.
//
// This file is NOT a standalone driver. It is evaluated (via evaluate()) by:
//   - scripts/confluenceToAsciiDoc.groovy      (XML space-export driver)
//   - scripts/confluenceApiToAsciiDoc.groovy   (REST-API partial-export driver)
//
// After evaluation, the caller can call these closures via its binding:
//   getFolderStructure(pages, pageId) -> String[]
//   findRootIdByTitle(pages, title)   -> String
//   filterToSubtree(pages, attachments, rootPageId) -> [filteredPages, filteredAttachments]
//   fixBody(pageId, body, users, pages, space)      -> [html, unknownTagsList]
//   writePage(pageId, rawBody, childIds, pages, attachments, space, users, destDir) -> unknownTagsList
//   createMenu(pages, startPageId)    -> String
//
// The caller is responsible for setting binding properties before invoking writePage:
//   lucidInfoFile     - File where discovered LucidChart references are logged
//   lucidChartsIframe - Boolean, true to emit an iframe (false -> image reference)
//
// unknownTagsStats is maintained by fixBody and can be consulted by the driver afterwards.

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.nodes.Document

// shared mutable state, only initialise if the caller hasn't already set it
if (!binding.variables.containsKey('unknownTagsStats')) {
    unknownTagsStats = [:]
}
if (!binding.variables.containsKey('lucidChartsIframe')) {
    lucidChartsIframe = false
}

// get the folder structure for the current page through the page structure information
getFolderStructure = { Map pages, String pageId ->
    def parentId = pages[pageId]?.parentId
    if (parentId && parentId != "null" && parentId != 0) {
        if (pages[parentId]) {
            return getFolderStructure(pages, parentId) + pages[parentId].filename
        } else {
            println "parent page not found: " + parentId + " for " + pages[pageId].filename
            return []
        }
    } else {
        return []
    }
}

// resolve a page title to its id; throws if not found or ambiguous
findRootIdByTitle = { Map pages, String title ->
    def matches = pages.findAll { it.value.title == title }
    if (matches.isEmpty()) {
        throw new IllegalArgumentException("No page found with title '${title}'")
    }
    if (matches.size() > 1) {
        throw new IllegalArgumentException("Multiple pages found with title '${title}' (ids: ${matches.keySet()}); use rootPageId instead")
    }
    return matches.keySet().first()
}

// restrict pages and attachments to the subtree rooted at rootPageId
filterToSubtree = { Map pages, Map attachments, String rootPageId ->
    if (!pages.containsKey(rootPageId)) {
        throw new IllegalArgumentException("rootPageId '${rootPageId}' not found in export")
    }
    def childrenByParent = [:].withDefault { [] }
    pages.each { pageId, info ->
        def parentId = info.parentId?.toString()
        if (parentId) {
            childrenByParent[parentId] << pageId
        }
    }
    def descendants = [] as Set
    def queue = new LinkedList<String>()
    queue << rootPageId
    while (!queue.isEmpty()) {
        def current = queue.poll()
        if (!descendants.add(current)) continue
        childrenByParent[current].each { queue << it }
    }
    def filteredPages = pages.findAll { descendants.contains(it.key) }
    filteredPages[rootPageId].parentId = 0
    def filteredAttachments = attachments.findAll { descendants.contains(it.value.pageId) }
    return [filteredPages, filteredAttachments]
}

// sanitize a page title into a filename usable on any filesystem (matches the
// XML driver's original logic so file layouts stay byte-compatible).
sanitizeFilename = { String title ->
    return title
            .replaceAll("[Ää]", "ae")
            .replaceAll("[Üü]", "ue")
            .replaceAll("[Öö]", "oe")
            .replaceAll("[^a-zA-Z0-9]", "_")
            .replaceAll("_+", "_")
}

// takes confluence xHTML storage format and fixes some issues to be better converted by pandoc
fixBody = { String pageId, String body, Map users, Map pages, Map space ->
    body = body
    // it seems to be a bug how CDATA sections are closed in the xHTML
            .replaceAll("]] ><", "]]><")
    // remove empty headings
            .replaceAll("<h[1-9]> </h[1-9]>", "")
    // remove unecessary colspan attrib
            .replaceAll('colspan="1"', "")
    // fix colspan=2 for pandoc
            .replaceAll('colspan="2"(.*?)</(t[dh])>', '$1</$2><$2></$2>')

    Document dom = Jsoup.parse(body, 'utf-8', Parser.xmlParser())
    dom.outputSettings().prettyPrint(false)
    dom.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
    dom.outputSettings().charset("UTF-8")
    def acTags = []
    dom.select("*").each { element ->
        if (element.tagName().startsWith("ac:")) {
            acTags << element.tagName().replace(":", "|")
        }
    }
    // fix links which still point to confluence
    dom.select("a").each { element ->
        def href = element.attr('href')
        def regexp = href =~ '/spaces/([^/]+)/pages/([0-9]+)/'
        if (regexp.size() == 1) {
            def targetSpace = regexp[0][1]
            if (space.key != targetSpace) {
                println "WARNING: can't rewrite links between different spaces (source: $space.key, target: $targetSpace)"
            } else {
                def targetPage = regexp[0][2]
                def targetFilename = (pages[targetPage]?.filename) ?: ''
                def folderStructureTarget = getFolderStructure(pages, targetPage).join("/")
                def folderStructureSource = getFolderStructure(pages, pageId)
                def targetLink = "../" * folderStructureSource.size() + folderStructureTarget + "/" + targetFilename + ".html"
                element.attr('href', targetLink)
            }
        }
    }
    dom.select("span").each { element ->
        if (element.attr("class").startsWith("css-")) {
            element.unwrap()
        }
        if (element.attr("class").startsWith("loader-wrapper")) {
            element.unwrap()
        }
        if (element.attr("class").contains("smart-link-title-wrapper")) {
            element.unwrap()
        }
    }
    acTags = acTags.unique()
    def unknownTags = []
    if (acTags.size() > 0) {
        dom.select(acTags.join(", ")).each { element ->
            def name = element.tagName()
            switch (name) {
                case "ac:image":
                    def alignment = element.attr("ac:align")
                    def width = element.attr("ac:width")
                    def filename = element.select(["ri|attachment"]).attr("ri:filename")
                    def version = element.select(["ri|attachment"]).attr("ri:version-at-save")
                    element.before("<img src='{filepath}/${(version ? version + "_" : "1_") + (filename.replaceAll(":", "_"))}' align='${alignment ?: ''}' width='${width ?: ''}' />")
                    element.remove()
                    break
                case "ac:link":
                    if (element.children().size() > 0) {
                        def anchor = element.attr("ac:anchor")
                        def targetPage = element.select("ri|page").attr("ri:content-title")
                        def linkText = element.select("ac|plain-text-link-body").text()
                        if (linkText.trim() == "") {
                            linkText = element.select("ac|link-body").text()
                        }
                        targetPage = pages.find { _pageId, pageInfo -> pageInfo.title == targetPage }
                        if (targetPage) {
                            def folderStructureTarget = getFolderStructure(pages, targetPage.key).join("/")
                            def folderStructureSource = getFolderStructure(pages, pageId)
                            def targetLink = "../" * folderStructureSource.size() + folderStructureTarget + "/" + targetPage.value.filename + ".adoc"
                            if (anchor) {
                                targetLink += "#" + anchor
                            }
                            element.before(" xref:${targetLink}[${linkText}] ")
                            element.remove()
                        } else {
                            //<ac:link><ri:attachment ri:filename="attachment.png" ri:version-at-save="1" /></ac:link>
                        }
                    }
                    break
                case "ac:plain-text-link-body":
                    // ignore, part of link
                    break
                case "ac:inline-comment-marker":
                    // ignore - not supported
                    break
                case ["ac:layout",
                      "ac:layout-section",
                      "ac:layout-cell",
                      "ac:placeholder"
                ]:
                    // multi-column layouts are not easily converted - ignore
                    break
                case "ac:structured-macro":
                    def macroName = element.attr("ac:name")
                    switch (macroName) {
                        case ["drawio",
                              "excerpt-include",
                              "panel",
                        ]:
                            // ignore - not supported
                            break
                        case ["expand",
                              "expandable-comment",
                        ]:
                            break
                        case ["view-file"]:
                            def filename = element.select(["ri|attachment"]).attr("ri:filename")
                            def version = element.select(["ri|attachment"]).attr("ri:version-at-save")
                            def height = element.select("ac|parameter[ac:name=height]") ?: '400'
                            def filepath = "images/" + getFolderStructure(pages, pageId).join("/")
                            filepath = "../" * getFolderStructure(pages, pageId).size() + filepath
                            if (filename.toLowerCase().endsWith('.pdf')) {
                                element.before("""
<div>
++++%%CRLF%%
&lt;iframe name="${filename.replaceAll(":", "_")}" allowfullscreen frameborder="0" src='${filepath}/${(version ? version + "_" : "1_") + (filename.replaceAll(":", "_"))}' width='100%' height='${height}' >&lt;/iframe>%%CRLF%%
++++%%CRLF%%
</div>
""")
                            } else if (filename[-4..-1].toLowerCase() in ['.jpg', '.png']) {
                                element.before("<img src='${filepath}/${(version ? version + "_" : "1_") + (filename.replaceAll(":", "_"))}'  />")
                            } else {
                                element.before("<a href='${filepath}/${(version ? version + "_" : "1_") + (filename.replaceAll(":", "_"))}'  >$filename</a>")
                            }
                            element.remove()
                            break
                        case 'attachments':
                            // handled later in writePage (we don't know attachments here)
                            element.before("%%attachments%%")
                            element.remove()
                            break
                        case 'profile':
                            def userkey = element.select("ri|user").attr("ri:userkey")
                            if (users[userkey]) {
                                element.before("""
User:: ${users[userkey].name}%%CRLF%%
// ${users[userkey].atlassianAccountId}%%CRLF%%
""")
                                element.remove()
                            }
                            break
                        case 'lucidchart':
                            def documentId = element.select("ac|parameter[ac:name=documentId]").text()
                            def lucidInfos = """
// lucidChart
// localId: ${element.attr("ac:local-id")}
// macroId: ${element.attr("ac:macro-id")}
"""
                            element.select("ac|parameter").each { parameter ->
                                def pname = parameter.attr("ac:name")
                                def pvalue = parameter.text()
                                lucidInfos += "// ${pname}: ${pvalue}\n"
                            }
                            def chart = ""
                            if (lucidChartsIframe) {
                                chart = """
++++%%CRLF%%
&lt;iframe allowfullscreen frameborder="0" style="width:640px; height:480px" src="https://lucid.app/documents/embedded/${documentId}" >&lt;/iframe>%%CRLF%%
++++%%CRLF%%
"""
                            } else {
                                def folderStructure = getFolderStructure(pages, pageId)
                                chart = """
%%CRLF%%
image::${folderStructure.join("/")}/${documentId}.png[]%%CRLF%%
%%CRLF%%
"""
                                lucidInfoFile.append("""\
images/${folderStructure.join("/")}/${documentId}.png
""".toString())
                            }
                            element.before("""
    <div class="lucidchart-wrapper">
${lucidInfos.replaceAll("\n", "%%CRLF%%")}
    $chart
    https://lucid.app/lucidchart/${documentId}/edit[edit lucidchart]
    </div>
    """)
                            element.remove()
                            break
                        case 'toc':
                            // handled by asciidoctor
                            element.remove()
                            break
                        case 'children':
                            element.remove()
                            break
                        case ['tip',
                              'info',
                              'warning',
                              'note'
                        ]:
                            body = element.select("ac|rich-text-body").html()
                            def type = [
                                    'info'   : 'NOTE',
                                    'warning': 'WARNING',
                                    'note'   : 'CAUTION',
                                    'tip'    : 'TIP'][macroName]
                            element.html("""
    <div class="admonition-wrapper">
    [${type}]%%CRLF%%
    ====%%CRLF%%
    ${body.replaceAll("<h([1-9])>", "<h\$1>[discrete]")}%%CRLF%%
    ====%%CRLF%%
    </div>
    """)
                            element.unwrap()
                            break
                        case 'anchor':
                            def anchor = element.select("ac|parameter").text()
                            element.html("\n<span>[[${anchor}]]<span>\n")
                            element.unwrap()
                            break
                        case ['code', 'paste-code-macro']:
                            def language = element.select("ac|parameter[ac:name=language]").text()
                            def code = element.select("ac|plain-text-body").text()
                            element.html("""
    <div class="code-wrapper">
    [source, $language]%%CRLF%%
    ----%%CRLF%%
    ${code.replaceAll("\n", "%%CRLF%%")}%%CRLF%%
    ----%%CRLF%%
    </div>
    """)
                            element.unwrap()
                            break
                        default:
                            unknownTags = (unknownTags << "ac:structured-macro $macroName").unique()
                            if (!unknownTagsStats["ac:structured-macro $macroName"]) {
                                unknownTagsStats["ac:structured-macro $macroName"] = []
                            }
                            unknownTagsStats["ac:structured-macro $macroName"] << pageId
                    }
                    break
                case ["ac:parameter",
                      "ac:rich-text-body",
                      "ac:plain-text-body"
                ]:
                    // ignore, part of structured-macro
                    break
                default:
                    unknownTags = (unknownTags << name).unique()
                    if (!unknownTagsStats[name]) {
                        unknownTagsStats[name] = []
                    }
                    unknownTagsStats[name] << pageId
            }
        }
    }
    def html = dom.html()
    // some last dirty hacks
            .replaceAll("<a ", " <a ")
            .replaceAll("[/][#][/]", '/\\\\#/')
            .replaceAll("<p></p><strong><br />", "<strong>")
            .replaceAll("<strong><br /></strong>", "<br />")
            .replaceAll("<div><div class=\"title\">([^<]+)</div></div>", ".\$1")
            .replaceAll("<strong><br />[.]([^<]+)</strong>", ".\$1")
    return [html, unknownTags]
}

// Convert and write one page to disk (HTML via fixBody, then pandoc to AsciiDoc,
// then post-processing + file header + child includes + attachment links).
// Returns the list of unknown tags encountered while processing this page.
writePage = { String pageId, String rawBody, List<String> childIds,
              Map pages, Map attachments, Map space, Map users, File destDir ->
    def metaData = pages[pageId]
    def folderStructure = getFolderStructure(pages, pageId)
    def deepFilename = folderStructure.join("/") + "/" + metaData.filename.toString()

    if (folderStructure.size() >= 1) {
        new File(destDir, folderStructure.join("/")).mkdirs()
    }
    def outFile = new File(destDir, deepFilename + ".html")
    def (String body, List uTags) = fixBody(pageId, rawBody ?: '', users, pages, space)

    outFile.write(body, 'utf-8')

    def outFilename = outFile.canonicalPath[0..-6] + ".adoc"
    def command = "pandoc --wrap preserve -f html -t asciidoc -s ${outFile.canonicalPath} -o ${outFilename}"
    def process = command.execute()
    process.waitForProcessOutput(System.out, System.err)
    if (process.exitValue() > 1) {
        println "couldn't convert ${outFile.canonicalPath}"
        return uTags
    }

    def outFileAdoc = new File(outFilename)
    def weightedChildren = []
    childIds.each { child ->
        if (pages[child] == null) return
        weightedChildren << [weight : ((pages[child]?.position ?: "-1") as Integer),
                             include: "include::" + pages[pageId].filename + "/" + pages[child].filename + ".adoc[levelOffset=+1]",
                             menu   : "include::" + pages[child].filename + "/_menu.adoc[]"]
    }
    def childIncludes = ""
    if (weightedChildren.size() > 0) {
        childIncludes = """
    ifdef::includeChildren[]
    ${weightedChildren.sort { it.weight }.collect { it.include }.join("\n")}
    endif::includeChildren[]
    """
    }
    println deepFilename
    def fileHeader = """
:jbake-menu: ${folderStructure.size() > 0 ? folderStructure[0] : '-'}
:jbake-deep-menu: ${folderStructure.join("/")}
:jbake-status: published
:jbake-type: page_custom_menu
:jbake-order: ${metaData.position ?: '0'}
:jbake-root: ${"../" * (folderStructure.size())}
:filename: ${metaData.filename.toString()}.adoc
:filepath: ${folderStructure.join("/")}
include::{jbake-root}_config.adoc[]
ifndef::imagesdir[:imagesdir: {jbake-root}images]

"""
    def adoc = outFileAdoc.text
            .replaceAll("%%CRLF%% *", "\n")
            .replaceAll("(=+) \\[discrete\\]", "[discrete]\n\$1 ")
            // U+00A0 NO-BREAK SPACE -> AsciiDoc {nbsp} attribute.
            // Use an explicit Unicode escape so the literal NBSP doesn't get
            // normalised to a regular space by editors (which would replace
            // *every* space in the output - breaking headings, lists, etc.).
            .replaceAll("\u00A0", "{nbsp}")
            .replaceAll("(?sm)^ [+] *\$", "")
            .replaceAll("%7Bfilepath%7D", "{filepath}")
    println(pages[pageId].title)
    def linkedAttachments = "\n"
    if (adoc.contains('%%attachments%%')) {
        linkedAttachments += ".Attachments\n\n"
        attachments.findAll { it.value.pageId == pageId }.each { attachmentId, attributes ->
            linkedAttachments += "* link:${"../" * (folderStructure.size())}images/${folderStructure.join("/")}/" + attributes.version + "_" + attributes.filename + "[" + attributes.filename + " (v${attributes.version})]\n"
        }
        adoc = adoc.replace('%%attachments%%', linkedAttachments)
        linkedAttachments = "\n"
    }
    attachments.findAll { it.value.pageId == pageId }.each { attachmentId, attributes ->
        linkedAttachments += "// attachment /images/${folderStructure.join("/")}/" + attributes.filename + "[" + attributes.filename + "]\n"
    }
    outFileAdoc.write(
            fileHeader +
                    "== ${pages[pageId].title}\n\n" +
                    adoc +
                    linkedAttachments + "\n" +
                    childIncludes,
            'utf-8'
    )
    outFile.delete()
    return uTags
}

createMenu = { Map pages, startPageId ->
    def pageList = pages.findAll { it.value.parentId == startPageId }
    def menu = ""
    pageList.sort { (it.value.position ?: '-1') as Integer }.each { page ->
        def folderStructure = getFolderStructure(pages, page.key)
        menu += "*" * (folderStructure.size() + 1) + " xref:{jbake-root}" + folderStructure.join("/") + '/' + page.value.filename + ".adoc[" + page.value.title + "]\n"
        def childPageList = pages.findAll { it.value.parentId == page.key }
        if (childPageList.size() > 0) {
            menu += createMenu(pages, page.key)
        }
    }
    return menu
}
