/**
 * Created by Ralf D. Mueller and Alexander Heusingfeld
 * https://github.com/rdmueller/asciidoc2confluence
 *
 * this script expects an HTML document created with AsciiDoctor
 * in the following style (default AsciiDoctor output)
 * <div class="sect1">
 *     <h2>Page Title</h2>
 *     <div class="sectionbody">
 *         <div class="sect2">
 *            <h3>Sub-Page Title</h3>
 *         </div>
 *         <div class="sect2">
 *            <h3>Sub-Page Title</h3>
 *         </div>
 *     </div>
 * </div>
 * <div class="sect1">
 *     <h2>Page Title</h2>
 *     ...
 * </div>
 *
 */

// some dependencies
/**
@Grapes(
        [@Grab('org.jsoup:jsoup:1.8.2'),
         @Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.6' ),
         @Grab('org.apache.httpcomponents:httpmime:4.5.1')]
)
**/
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.EncoderRegistry
import groovyx.net.http.ContentType
import java.security.MessageDigest
//to upload attachments:
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.HttpMultipartMode
import groovyx.net.http.Method

def CDATA_PLACEHOLDER_START = '<cdata-placeholder>'
def CDATA_PLACEHOLDER_END = '</cdata-placeholder>'

def baseUrl

// configuration

def confluenceSpaceKey
def confluenceCreateSubpages
def confluencePagePrefix

// helper functions

def MD5(String s) {
    MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
}

// for getting better error message from the REST-API
void trythis (Closure action) {
    try {
        action.run()
    } catch (HttpResponseException error) {
        println "something went wrong - got an http response code "+error.response.status+":"
        println error.response.data
        //throw error
    }
}


def parseAdmonitionBlock(block, String type) {
    content = block.select(".content").first()
    titleElement = content.select(".title")
    titleText = ''
    if(titleElement != null) {
        titleText = "<ac:parameter ac:name=\"title\">${titleElement.text()}</ac:parameter>"
        titleElement.remove()
    }
    block.after("<ac:structured-macro ac:name=\"${type}\">${titleText}<ac:rich-text-body>${content}</ac:rich-text-body></ac:structured-macro>")
    block.remove()
}

def uploadAttachment = { def pageId, String url, String fileName, String note ->
    def is
    def localHash
    if (url.startsWith('http')) {
        is = new URL(url).openStream()
        //build a hash of the attachment
        localHash = MD5(new URL(url).openStream().text)
    } else {
        is = new File(url).newDataInputStream()
        //build a hash of the attachment
        localHash = MD5(new File(url).newDataInputStream().text)
    }

    //https://docs.atlassian.com/confluence/REST/latest/
    def api = new RESTClient(config.confluence.api)
    //this fixes the encoding
    api.encoderRegistry = new EncoderRegistry( charset: 'utf-8' )

    def headers = [
            'Authorization': 'Basic ' + config.confluence.credentials,
            'X-Atlassian-Token':'no-check'
    ]
    //check if attachment already exists
    def result = "nothing"
    def attachment = api.get(path: 'content/' + pageId + '/child/attachment',
            query: [
                    'filename': fileName,
            ], headers: headers).data
    def http
    if (attachment.size==1) {
        // attachment exists. need an update?
        def remoteHash = attachment.results[0].extensions.comment.replaceAll("(?sm).*#([^#]+)#.*",'$1')
        if (remoteHash!=localHash) {
            //hash is different -> attachment needs to be updated
            http = new HTTPBuilder(config.confluence.api + 'content/' + pageId + '/child/attachment/' + attachment.results[0].id + '/data')
            println "    updated attachment"
        }
    } else {
        http = new HTTPBuilder(config.confluence.api + 'content/' + pageId + '/child/attachment')
    }
    if (http) {
        http.request(Method.POST) { req ->
            requestContentType: "multipart/form-data"
            MultipartEntity multiPartContent = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
            // Adding Multi-part file parameter "file"
            multiPartContent.addPart("file", new InputStreamBody(is, fileName))
            // Adding another string parameter "comment"
            multiPartContent.addPart("comment", new StringBody(note + "\r\n#" + localHash + "#"))
            req.setEntity(multiPartContent)
            headers.each { key, value ->
                req.addHeader(key, value)
            }
        }
    }
}


def realTitle = { pageTitle ->
    confluencePagePrefix + pageTitle
}

def rewriteMarks = { body ->
    // Confluence strips out mark elements.  Replace them with default formatting.
    body.select('mark').wrap('<span style="background:#ff0;color:#000"></style>').unwrap()
}

def rewriteDescriptionLists = { body ->
    def TAGS = [ dt: 'th', dd: 'td' ]
    body.select('dl').each { dl ->
        // WHATWG allows wrapping dt/dd in divs, simply unwrap them
        dl.select('div').each { it.unwrap() }

        // group dts and dds that belong together, usually it will be a 1:1 relation
        // but HTML allows for different constellations
        def rows = []
        def current = [dt: [], dd: []]
        rows << current
        dl.select('dt, dd').each { child ->
            def tagName = child.tagName()
            if (tagName == 'dt' && current.dd.size() > 0) {
                // dt follows dd, start a new group
                current = [dt: [], dd: []]
                rows << current
            }
            current[tagName] << child.tagName(TAGS[tagName])
            child.remove()
        }

        rows.each { row ->
            def sizes = [dt: row.dt.size(), dd: row.dd.size()]
            def rowspanIdx = [dt: -1, dd: sizes.dd - 1]
            def rowspan = Math.abs(sizes.dt - sizes.dd) + 1
            def max = sizes.dt
            if (sizes.dt < sizes.dd) {
                max = sizes.dd
                rowspanIdx = [dt: sizes.dt - 1, dd: -1]
            }
            (0..<max).each { idx ->
                def tr = dl.appendElement('tr')
                ['dt', 'dd'].each { type ->
                    if (sizes[type] > idx) {
                        tr.appendChild(row[type][idx])
                        if (idx == rowspanIdx[type] && rowspan > 1) {
                            row[type][idx].attr('rowspan', "${rowspan}")
                        }
                    } else if (idx == 0) {
                        tr.appendElement(TAGS[type]).attr('rowspan', "${rowspan}")
                    }
                }
            }
        }

        dl.wrap('<table></table>')
            .unwrap()
    }
}

def rewriteInternalLinks = { body, anchors, pageAnchors ->
    // find internal cross-references and replace them with link macros
    body.select('a[href]').each { a ->
        def href = a.attr('href')
        if (href.startsWith('#')) {
            def anchor = href.substring(1)
            def pageTitle = anchors[anchor] ?: pageAnchors[anchor]
            if (pageTitle) {
                // as Confluence insists on link texts to be contained
                // inside CDATA, we have to strip all HTML and
                // potentially loose styling that way.
                a.html(a.text())
                a.wrap("<ac:link${anchors.containsKey(anchor) ? ' ac:anchor="' + anchor + '"' : ''}></ac:link>")
                   .before("<ri:page ri:content-title=\"${realTitle pageTitle}\"/>")
                   .wrap("<ac:plain-text-link-body>${CDATA_PLACEHOLDER_START}${CDATA_PLACEHOLDER_END}</ac:plain-text-link-body>")
                   .unwrap()
            }
        }
    }
}

def rewriteCodeblocks = { body ->
    body.select('pre > code').each { code ->
        if (code.attr('data-lang')) {
            code.select('span[class]').each { span ->
                span.unwrap()
            }
            code.before("<ac:parameter ac:name=\"language\">${code.attr('data-lang')}</ac:parameter>")
        }
        code.parent() // pre now
            .wrap('<ac:structured-macro ac:name="code"></ac:structured-macro>')
            .unwrap()
        code.wrap("<ac:plain-text-body>${CDATA_PLACEHOLDER_START}${CDATA_PLACEHOLDER_END}</ac:plain-text-body>")
            .unwrap()
    }
}

def unescapeCDATASections = { html ->
    def start = html.indexOf(CDATA_PLACEHOLDER_START)
    while (start > -1) {
        def end = html.indexOf(CDATA_PLACEHOLDER_END, start)
        if (end > -1) {
            def prefix = html.substring(0, start) + CDATA_PLACEHOLDER_START
            def suffix = html.substring(end)
            def unescaped = html.substring(start + CDATA_PLACEHOLDER_START.length(), end)
                    .replaceAll('&lt;', '<').replaceAll('&gt;', '>')
                    .replaceAll('&amp;', '&')
            html = prefix + unescaped + suffix
        }
        start = html.indexOf(CDATA_PLACEHOLDER_START, start + 1)
    }
    html
}

//modify local page in order to match the internal confluence storage representation a bit better
//definition lists are not displayed by confluence, so turn them into tables
//body can be of type Element or Elements
def deferredUpload = []
def parseBody =  { body, anchors, pageAnchors ->
    body.select('div.paragraph').unwrap()
    body.select('div.ulist').unwrap()
    body.select('div.sect3').unwrap()
    [   'note':'info',
        'warning':'warning',
        'important':'warning',
        'caution':'note',
        'tip':'tip'            ].each { adType, cType ->
        body.select('.admonitionblock.'+adType).each { block ->
            parseAdmonitionBlock(block, cType)
        }
    }
    //special for the arc42-template
    body.select('div.arc42help').select('.content')
            .wrap('<ac:structured-macro ac:name="expand"></ac:structured-macro>')
            .wrap('<ac:rich-text-body></ac:rich-text-body>')
            .wrap('<ac:structured-macro ac:name="info"></ac:structured-macro>')
            .before('<ac:parameter ac:name="title">arc42</ac:parameter>')
            .wrap('<ac:rich-text-body><p></p></ac:rich-text-body>')
    body.select('div.arc42help').unwrap()
    body.select('div.title').wrap("<strong></strong>").before("<br />").wrap("<div></div>")
    body.select('div.listingblock').wrap("<p></p>").unwrap()
    // see if we can find referenced images and fetch them
    new File("tmp/images/.").mkdirs()
    // find images, extract their URLs for later uploading (after we know the pageId) and replace them with this macro:
    // <ac:image ac:align="center" ac:width="500">
    // <ri:attachment ri:filename="deployment-context.png"/>
    // </ac:image>
    body.select('img').each { img ->
        img.attributes().each { attribute ->
            //println attribute.dump()
        }
        def src = img.attr('src')
        def imgWidth = img.attr('width')?:500
        def imgAlign = img.attr('align')?:"center"
        println "    image: "+src

        //it is not an online image, so upload it to confluence and use the ri:attachment tag
        if(!src.startsWith("http")) {
          def newUrl = baseUrl.toString().replaceAll('\\\\','/').replaceAll('/[^/]*$','/')+src
          def fileName = (src.tokenize('/')[-1])

          trythis {
              deferredUpload <<  [0,newUrl,fileName,"automatically uploaded"]
          }
          img.after("<ac:image ac:align=\"${imgAlign}\" ac:width=\"${imgWidth}\"><ri:attachment ri:filename=\"${fileName}\"/></ac:image>")
        }
        // it is an online image, so we have to use the ri:url tag
        else {
          img.after("<ac:image ac:align=\"imgAlign\" ac:width=\"${imgWidth}\"><ri:url ri:value=\"${src}\"/></ac:image>")
        }
        img.remove()
    }
    rewriteMarks body
    rewriteDescriptionLists body
    rewriteInternalLinks body, anchors, pageAnchors
    //sanitize code inside code tags
    rewriteCodeblocks body
    def pageString = unescapeCDATASections body.html().trim()

    //change some html elements through simple substitutions
    pageString = pageString
            .replaceAll('<br>','<br />')
            .replaceAll('</br>','<br />')
            .replaceAll('<a([^>]*)></a>','')
            .replaceAll(CDATA_PLACEHOLDER_START,'<![CDATA[')
            .replaceAll(CDATA_PLACEHOLDER_END,']]>')

    return pageString
}

// the create-or-update functionality for confluence pages
def pushToConfluence = { pageTitle, pageBody, parentId, anchors, pageAnchors ->
    def api = new RESTClient(config.confluence.api)
    def headers = [
            'Authorization': 'Basic ' + config.confluence.credentials,
            'Content-Type':'application/json; charset=utf-8'
    ]
    //this fixes the encoding
    api.encoderRegistry = new EncoderRegistry( charset: 'utf-8' )
    //try to get an existing page
    localPage = parseBody(pageBody, anchors, pageAnchors)

    def localHash = MD5(localPage)
    def prefix = '<p><ac:structured-macro ac:name="toc"/></p>'+(config.confluence.extraPageContent?:'')
    localPage  = prefix+localPage
    localPage += '<p><ac:structured-macro ac:name="children"><ac:parameter ac:name="sort">creation</ac:parameter></ac:structured-macro></p>'
    localPage += '<p style="display:none">hash: #'+localHash+'#</p>'

    def request = [
            type : 'page',
            title: realTitle(pageTitle),
            space: [
                    key: config.confluence.spaceKey
            ],
            body : [
                    storage: [
                            value         : localPage,
                            representation: 'storage'
                    ]
            ]
    ]
    if (parentId) {
        request.ancestors = [
                [ type: 'page', id: parentId]
        ]
    }

    def pages
    trythis {
        def cql = "space='${config.confluence.spaceKey}' AND type=page AND title~'" + realTitle(pageTitle) + "'"
        if (parentId) {
            cql += " AND parent=${parentId}"
        }
        pages = api.get(path: 'content/search',
                        query: ['cql' : cql,
                                'expand'  : 'body.storage,version'
                               ], headers: headers).data.results
    }

    def page = pages.find { p -> p.title.equalsIgnoreCase(realTitle(pageTitle)) }

    if (page) {
        //println "found existing page: " + page.id +" version "+page.version.number

        //extract hash from remote page to see if it is different from local one

        def remotePage = page.body.storage.value.toString().trim()

        def remoteHash = remotePage =~ /(?ms)hash: #([^#]+)#/
        remoteHash = remoteHash.size()==0?"":remoteHash[0][1]

        if (remoteHash == localHash) {
            //println "page hasn't changed!"
            deferredUpload.each {
                uploadAttachment(page?.id, it[1], it[2], it[3])
            }
            deferredUpload = []
            return page.id
        } else {
            trythis {
                // update page
                // https://developer.atlassian.com/display/CONFDEV/Confluence+REST+API+Examples#ConfluenceRESTAPIExamples-Updatingapage
                request.id      = page.id
                request.version = [number: (page.version.number as Integer) + 1]
                def res = api.put(contentType: ContentType.JSON,
                                  requestContentType : ContentType.JSON,
                                  path: 'content/' + page.id, body: request, headers: headers)
            }
            println "> updated page"+page.id
            deferredUpload.each {
                uploadAttachment(page.id, it[1], it[2], it[3])
            }
            deferredUpload = []
            return page.id
        }
    } else {
        if (parentId) {
            def foreignPages
            trythis {
                def foreignCql = "space='${config.confluence.spaceKey}' AND type=page AND title~'" + realTitle(pageTitle) + "'"
                foreignPages = api.get(path: 'content/search',
                                      query: ['cql' : foreignCql],
                                    headers: headers).data.results
            }

            def foreignPage = foreignPages.find { p -> p.title.equalsIgnoreCase(realTitle(pageTitle)) }

            if (foreignPage) {
                throw new IllegalArgumentException("Cannot create page, page with the same "
                    + "title=${foreignPage.title} "
                    + "with id=${foreignPage.id} already exists in the space. "
                    + "A Confluence page title must be unique within a space, consider specifying a 'confluencePagePrefix' in ConfluenceConfig.groovy")
            }
        }

        //create a page
        trythis {
            page = api.post(contentType: ContentType.JSON,
                            requestContentType: ContentType.JSON,
                            path: 'content', body: request, headers: headers)
        }
        println "> created page "+page?.data?.id
        deferredUpload.each {
            uploadAttachment(page?.data?.id, it[1], it[2], it[3])
        }
        deferredUpload = []
        return page?.data?.id
    }
}

def parseAnchors = { page ->
    def anchors = [:]
    page.body.select('[id]').each { anchor ->
        def name = anchor.attr('id')
        anchors[name] = page.title
        anchor.before("<ac:structured-macro ac:name=\"anchor\"><ac:parameter ac:name=\"\">${name}</ac:parameter></ac:structured-macro>")
    }
    anchors
}

def pushPages
pushPages = { pages, anchors, pageAnchors ->
    pages.each { page ->
        println page.title
        def id = pushToConfluence page.title, page.body, page.parent, anchors, pageAnchors
        page.children*.parent = id
        pushPages page.children, anchors, pageAnchors
    }
}

def recordPageAnchor = { head ->
    def a = [:]
    if (head.attr('id')) {
        a[head.attr('id')] = head.text()
    }
    a
}

def promoteHeaders = { tree, start, offset ->
    (start..7).each { i ->
        tree.select("h${i}").tagName("h${i-offset}").before('<br />')
    }
}

config.confluence.input.each { input ->

    input.file = "${docDir}/${input.file}"

    println "publish ${input.file}"

    if (input.file ==~ /.*[.](ad|adoc|asciidoc)$/) {
        println "convert ${input.file}"
        "groovy asciidoc2html.groovy ${input.file}".execute()
        input.file = input.file.replaceAll(/[.](ad|adoc|asciidoc)$/,'.html')
        println "to ${input.file}"
    }
    confluenceSpaceKey = input.spaceKey?:config.confluence.spaceKey
    confluenceCreateSubpages = (input.createSubpages!= null)?input.createSubpages:config.confluence.createSubpages
    confluencePagePrefix = input.pagePrefix?:config.confluence.pagePrefix

    def html =input.file?new File(input.file).getText('utf-8'):new URL(input.url).getText()
    baseUrl  =input.file?new File(input.file):new URL(input.url)
    Document dom = Jsoup.parse(html, 'utf-8', Parser.xmlParser())
    dom.outputSettings().prettyPrint(false);//makes html() preserve linebreaks and spacing
    dom.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml); //This will ensure xhtml validity regarding entities
    dom.outputSettings().charset("UTF-8"); //does no harm :-)
    def masterid = input.ancestorId

    // if confluenceAncestorId is not set, create a new parent page
    def parentId = !input.ancestorId ? null : input.ancestorId
    def anchors = [:]
    def pageAnchors = [:]
    def sections = pages = []

    // let's try to select the "first page" and push it to confluence
    dom.select('div#preamble div.sectionbody').each { pageBody ->
        pageBody.select('div.sect2').unwrap()
        def preamble = [
            title: input.preambleTitle ?: "arc42",
            body: pageBody,
            children: [],
            parent: parentId
        ]
        pages << preamble
        sections = preamble.children
        parentId = null
        anchors.putAll(parseAnchors(preamble))
    }
    // <div class="sect1"> are the main headings
    // let's extract these
    dom.select('div.sect1').each { sect1 ->
        Elements pageBody = sect1.select('div.sectionbody')
        def currentPage = [
            title: sect1.select('h2').text(),
            body: pageBody,
            children: [],
            parent: parentId
        ]
        pageAnchors.putAll(recordPageAnchor(sect1.select('h2')))

        if (confluenceCreateSubpages) {
            pageBody.select('div.sect2').each { sect2 ->
                def title = sect2.select('h3').text()
                pageAnchors.putAll(recordPageAnchor(sect2.select('h3')))
                sect2.select('h3').remove()
                def body = sect2
                def subPage = [
                    title: title,
                    body: body
                ]
                currentPage.children << subPage
                promoteHeaders sect2, 4, 3
                anchors.putAll(parseAnchors(subPage))
            }
            pageBody.select('div.sect2').remove()
        } else {
            pageBody.select('div.sect2').unwrap()
            promoteHeaders sect1, 3, 2
        }
        sections << currentPage
        anchors.putAll(parseAnchors(currentPage))
    }

    pushPages pages, anchors, pageAnchors
}
""
