package org.docToolchain.tasks

import org.docToolchain.atlassian.confluence.clients.ConfluenceClient
import org.docToolchain.atlassian.transformer.HtmlTransformer

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements

import java.nio.file.Path
import java.security.MessageDigest
import static groovy.io.FileType.FILES

import org.docToolchain.atlassian.confluence.clients.ConfluenceClientV1
import org.docToolchain.atlassian.confluence.clients.ConfluenceClientV2
import org.docToolchain.atlassian.confluence.ConfluenceService

/**
 * Based on asciidoc2confluence script created by Ralf D. Mueller and Alexander Heusingfeld
 * https://github.com/rdmueller/asciidoc2confluence
 *
 * This task expects an HTML document created with AsciiDoctor
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
class Asciidoc2ConfluenceTask extends DocToolchainTask {

    private ConfigObject config
    private String docDir
    private ConfluenceService confluenceService
    private ConfluenceClient confluenceClient
    private CDATA_PLACEHOLDER_START = '<cdata-placeholder>'
    private CDATA_PLACEHOLDER_END = '</cdata-placeholder>'
    private baseUrl
    private Map allPages
    // #938-mksiva: global variable to hold input spaceKey passed in the Config.groovy
    private spaceKeyInput
    // configuration
    private confluenceSpaceKey
    private confluenceSubpagesForSections
    private confluencePagePrefix
    private confluencePageSuffix

    static Asciidoc2ConfluenceTask From(ConfigObject config, String docDir) {
        return new Asciidoc2ConfluenceTask(config, docDir)
    }

    Asciidoc2ConfluenceTask(ConfigObject config, String docDir) {
        super(config)
        this.config = config
        this.docDir = docDir
        confluenceService = new ConfluenceService(configService)
        confluenceClient = configService.getConfigProperty("confluence.useV1Api") ?
            new ConfluenceClientV1(configService) :
            new ConfluenceClientV2(configService)
    }

    // helper functions
    static def MD5(String s) {
        MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
    }

    static def parseAdmonitionBlock(block, String type) {
        def content = block.select(".content").first()
        def titleElement = content.select(".title")
        def titleText = ''
        if(titleElement != null) {
            titleText = "<ac:parameter ac:name=\"title\">${titleElement.text()}</ac:parameter>"
            titleElement.remove()
        }
        block.after("<ac:structured-macro ac:name=\"${type}\">${titleText}<ac:rich-text-body>${content}</ac:rich-text-body></ac:structured-macro>")
        block.remove()
    }

    /**
     *  #342-dierk42
     *  Add labels to a Confluence page. Labels are taken from :keywords: which
     *  are converted as meta tags in HTML. Building the array: see below
     *
     *  Confluence allows adding labels only after creation of a page.
     *  Therefore we need extra API calls.
     *
     *  Currently the labels are added one by one. Suggestion for improvement:
     *  Build a label structure of all labels an place them with one call.
     *
     *  Replaces existing labels. No harm
     *  Does not check for deleted labels when keywords are deleted from source
     *  document!
     */
    def addLabels = { def pageId, def labelsArray ->
        // Attach each label in a API call of its own. The only prefix possible
        // in our own Confluence is 'global'
        labelsArray.each { label ->
            def label_data = [
                prefix : 'global',
                name : label
            ]
            confluenceClient.addLabel(pageId, label_data)
            println "added label ${label} to page ID ${pageId}"
        }
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

        def attachment = confluenceClient.getAttachment(pageId, fileName)
        if (attachment?.results) {
            // attachment exists. need an update?
            if (confluenceClient.attachmentHasChanged(attachment, localHash)) {
                //hash is different -> attachment needs to be updated
                confluenceClient.updateAttachment(pageId, attachment.results[0].id, is, fileName, note, localHash)
                println "    updated attachment"
            }
        } else {
            confluenceClient.createAttachment(pageId, is, fileName, note, localHash)
        }
    }

    def realTitle(pageTitle){
        confluencePagePrefix + pageTitle + confluencePageSuffix
    }

    static def rewriteMarks (body) {
        // Confluence strips out mark elements.  Replace them with default formatting.
        body.select('mark').wrap('<span style="background:#ff0;color:#000"></style>').unwrap()
    }

    /**
     * # 352-LuisMuniz: Helper methods
     * Fetch all pages of the defined config ancestorsIds. Only keep relevant info in the pages Map
     * The map is indexed by lower-case title
     */
    def retrieveAllPages(String spaceKey) {
        // #938-mksiva: added a condition spaceKeyInput is null, if it is null, it means that, space key is different, so re fetch all pages.
        if (allPages != null && spaceKeyInput == null) {
            println "allPages already retrieved"
            allPages
        } else {
            def pageIds = []
            def checkSpace = false
            int pageLimit = config.confluence.pageLimit ? config.confluence.pageLimit : 100
            config.confluence.input.each { input ->
                if (!input.ancestorId) {
                    // if one ancestorId is missing we should scan the whole space
                    checkSpace = true;
                    return
                }
                pageIds.add(input.ancestorId)
            }
            println (".")

            if(checkSpace) {
                allPages = confluenceClient.fetchPagesBySpaceKey(spaceKey, pageLimit)
            } else {
                allPages = confluenceClient.fetchPagesByAncestorId(pageIds, pageLimit)
            }
            allPages
        }
    }


    /**
     * Retrieve a page by id with contents and version
     */
    def retrieveFullPage = { String id ->
        println("retrieving page with id " + id)
        confluenceClient.retrieveFullPageById(id)
    }

    /**
     * if a parent has been specified, check whether a page has the same parent.
     */
    static boolean hasRequestedParent(Map existingPage, String requestedParentId) {
        if (requestedParentId) {
            existingPage.parentId == requestedParentId
        } else {
            true
        }
    }

    static def rewriteDescriptionLists(body) {
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

    def rewriteOpenAPI (Element body) {
        if (config.confluence.useOpenapiMacro == true || config.confluence.useOpenapiMacro == 'confluence-open-api') {
            body.select('div.openapi  pre > code').each { code ->
                def parent=code.parent()
                def rawYaml=code.wholeText()
                code.parent()
                    .wrap('<ac:structured-macro ac:name="confluence-open-api" ac:schema-version="1" ac:macro-id="1dfde21b-6111-4535-928a-470fa8ae3e7d"></ac:structured-macro>')
                    .unwrap()
                code.wrap("<ac:plain-text-body>${CDATA_PLACEHOLDER_START}${CDATA_PLACEHOLDER_END}</ac:plain-text-body>")
                    .replaceWith(new TextNode(rawYaml))
            }
        } else if (config.confluence.useOpenapiMacro == 'swagger-open-api') {
            body.select('div.openapi  pre > code').each { code ->
                def parent=code.parent()
                def rawYaml=code.wholeText()
                code.parent()
                    .wrap('<ac:structured-macro ac:name="swagger-open-api" ac:schema-version="1" ac:macro-id="f9deda8a-1375-4488-8ca5-3e10e2e4ee70"></ac:structured-macro>')
                    .unwrap()
                code.wrap("<ac:plain-text-body>${CDATA_PLACEHOLDER_START}${CDATA_PLACEHOLDER_END}</ac:plain-text-body>")
                    .replaceWith(new TextNode(rawYaml))
            }
        } else if (config.confluence.useOpenapiMacro == 'open-api') {

            def includeURL=null

            for (Element e : body.select('div .listingblock.openapi')) {
                for (String s : e.className().split(" ")) {
                    if (s.startsWith("url")) {
                        //include the link to the URL for the macro
                        includeURL = s.replace('url:', '')
                    }
                }
            }

            body.select('div.openapi  pre > code').each { code ->
                def parent=code.parent()
                def rawYaml=code.wholeText()

                code.parent()
                    .wrap('<ac:structured-macro ac:name="open-api" ac:schema-version="1" data-layout="default" ac:macro-id="4302c9d8-fca4-4f14-99a9-9885128870fa"></ac:structured-macro>')
                    .unwrap()

                if (includeURL!=null)
                {
                    code.before('<ac:parameter ac:name="url">'+includeURL+'</ac:parameter>')
                }
                else {
                    //default: show download button
                    code.before('<ac:parameter ac:name="showDownloadButton">true</ac:parameter>')
                    code.wrap("<ac:plain-text-body>${CDATA_PLACEHOLDER_START}${CDATA_PLACEHOLDER_END}</ac:plain-text-body>")
                        .replaceWith(new TextNode(rawYaml))
                }
            }
        }
    }

    static def getEmbeddedImageData(src){
        def imageData = src.split("[;:,]")
        def fileExtension = imageData[1].split("/")[1]
        // treat svg+xml as svg to be able to create a file from the embedded image
        // more MIME types: https://www.iana.org/assignments/media-types/media-types.xhtml#image
        if(fileExtension == "svg+xml"){
            fileExtension = "svg"
        }
        return Map.of(
            "fileExtension", fileExtension,
            "encoding", imageData[2],
            "encodedContent", imageData[3]
        )
    }

    def handleEmbeddedImage(basePath, fileName, fileExtension, encodedContent) {
        def imageDir = "images/"
        if(config.imageDirs.size() > 0){
            def dir = config.imageDirs.find { it ->
                def configureImagesDir = it.replace('./', '/')
                Path.of(basePath, configureImagesDir, fileName).toFile().exists()
            }
            if(dir != null){
                imageDir = dir.replace('./', '/')
            }
        }

        if(!Path.of(basePath, imageDir, fileName).toFile().exists()){
            println "Could not find embedded image at a known location"
            def embeddedImagesLocation = "/confluence/images/"
            new File(basePath + embeddedImagesLocation).mkdirs()
            def imageHash = MD5(encodedContent)
            println "Embedded Image Hash " + imageHash
            def image = new File(basePath + embeddedImagesLocation + imageHash + ".${fileExtension}")
            if(!image.exists()){
                println "Creating image at " + basePath + embeddedImagesLocation
                image.withOutputStream {output ->
                    output.write(encodedContent.decodeBase64())}
            }
            fileName = imageHash + ".${fileExtension}"
            return Map.of(
                "filePath", image.canonicalPath,
                "fileName", fileName
            )
        } else {
            return Map.of(
                "filePath", basePath + imageDir + fileName,
                "fileName", fileName
            )
        }
    }

    /**
     * modify local page in order to match the internal confluence storage representation a bit better
     * definition lists are not displayed by confluence, so turn them into tables
     * body can be of type Element or Elements
     */
    def parseBody(body, anchors, pageAnchors) {
        def uploads = []
        rewriteOpenAPI body

        body.select('div.paragraph').unwrap()
        body.select('div.ulist').unwrap()
        //body.select('div.sect3').unwrap()
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
            def src = img.attr('src')
            def imgWidth = img.attr('width')?:500
            def imgAlign = img.attr('align')?:"center"

            //it is not an online image, so upload it to confluence and use the ri:attachment tag
            if(!src.startsWith("http")) {
                def sanitizedBaseUrl = baseUrl.toString().replaceAll('\\\\','/').replaceAll('/[^/]*$','/')
                def newUrl
                def fileName
                //it is an embedded image
                if(src.startsWith("data:image")){
                    def imageData = getEmbeddedImageData(src)
                    def fileExtension = imageData.get("fileExtension")
                    def encodedContent = imageData.get("encodedContent")
                    fileName = img.attr('alt').replaceAll(/\s+/,"_").concat(".${fileExtension}")
                    def embeddedImage = handleEmbeddedImage(sanitizedBaseUrl, fileName, fileExtension, encodedContent)
                    newUrl = embeddedImage.get("filePath")
                    fileName = embeddedImage.get("fileName")
                }else {
                    newUrl = sanitizedBaseUrl + src
                    fileName = URLDecoder.decode((src.tokenize('/')[-1]),"UTF-8")
                }
                newUrl = URLDecoder.decode(newUrl,"UTF-8")
                println "    image: "+newUrl
                uploads <<  [0,newUrl,fileName,"automatically uploaded"]
                img.after("<ac:image ac:align=\"${imgAlign}\" ac:width=\"${imgWidth}\"><ri:attachment ri:filename=\"${fileName}\"/></ac:image>")
            }
            // it is an online image, so we have to use the ri:url tag
            else {
                img.after("<ac:image ac:align=\"imgAlign\" ac:width=\"${imgWidth}\"><ri:url ri:value=\"${src}\"/></ac:image>")
            }
            img.remove()
        }


        if(config.confluence.enableAttachments){
            attachmentPrefix = config.confluence.attachmentPrefix ? config.confluence.attachmentPrefix : 'attachment'
            body.select('a').each { link ->

                def src = link.attr('href')
                println "    attachment src: "+src

                //upload it to confluence and use the ri:attachment tag
                if(src.startsWith(attachmentPrefix)) {
                    def newUrl = baseUrl.toString().replaceAll('\\\\','/').replaceAll('/[^/]*$','/')+src
                    def fileName = URLDecoder.decode((src.tokenize('/')[-1]),"UTF-8")
                    newUrl = URLDecoder.decode(newUrl,"UTF-8")

                    uploads <<  [0,newUrl,fileName,"automatically uploaded non-image attachment by docToolchain"]
                    def uriArray=fileName.split("/")
                    def pureFilename = uriArray[uriArray.length-1]
                    def innerhtml = link.html()
                    link.after("<ac:structured-macro ac:name=\"view-file\" ac:schema-version=\"1\"><ac:parameter ac:name=\"name\"><ri:attachment ri:filename=\"${pureFilename}\"/></ac:parameter></ac:structured-macro>")
                    link.after("<ac:link><ri:attachment ri:filename=\"${pureFilename}\"/><ac:plain-text-link-body> <![CDATA[\"${innerhtml}\"]]></ac:plain-text-link-body></ac:link>")
                    link.remove()

                }
            }
        }

        rewriteMarks body
        rewriteDescriptionLists body
        //not really sure if must check here the type
        String bodyString = body
        if(body instanceof Element){
            bodyString = body.html()
        }
        Element saneHtml = new Document("")
            .outputSettings(new Document.OutputSettings().syntax(Document.OutputSettings.Syntax.xml).prettyPrint(false))
            .html(bodyString)
        HtmlTransformer transformer = new HtmlTransformer()
        if(config.jira.api){
            transformer.withJiraIntegration(config.jira.api)
        }
        if(config.confluence.jiraServerId){
            transformer.usingOnPremiseJira(config.confluence.jiraServerId)
        }
        def pageString = transformer.transformToConfluenceFormat(saneHtml, anchors, pageAnchors, confluencePagePrefix, confluencePagePrefix)

        return Map.of(
            "page", pageString,
            "uploads", uploads
        )
    }

    def generateAndAttachToC(localPage) {
        def content
        if(config.confluence.disableToC){
            def prefix = (config.confluence.extraPageContent?:'')
            content  = prefix+localPage
        }else{
            def default_toc = '<p><ac:structured-macro ac:name="toc"/></p>'
            def prefix = (config.confluence.tableOfContents?:default_toc)+(config.confluence.extraPageContent?:'')
            content  = prefix+localPage
            def default_children = '<p><ac:structured-macro ac:name="children"><ac:parameter ac:name="sort">creation</ac:parameter></ac:structured-macro></p>'
            content += (config.confluence.tableOfChildren?:default_children)
        }
        def localHash = MD5(localPage)
        content += '<ac:placeholder>hash: #'+localHash+'#</ac:placeholder>'
        return content
    }

    /**
     * the create-or-update functionality for confluence pages
     * #342-dierk42: added parameter 'keywords'
     */
    def pushToConfluence(pageTitle, pageBody, parentId, anchors, pageAnchors, keywords) {
        parentId = parentId?.toString()

        def deferredUpload = []
        String realTitleLC = realTitle(pageTitle).toLowerCase()
        String realTitle = realTitle(pageTitle)

        //try to get an existing page
        def parsedBody = parseBody(pageBody, anchors, pageAnchors)
        def localPage = parsedBody.get("page")
        deferredUpload.addAll(parsedBody.get("uploads"))
        def localHash = MD5(localPage)
        localPage = generateAndAttachToC(localPage)

        // #938-mksiva: Changed the 3rd parameter from 'config.confluence.spaceKey' to 'confluenceSpaceKey' as it was always taking the default spaceKey
        // instead of the one passed in the input for each row.
        def pages = retrieveAllPages(confluenceSpaceKey)
        println("pages retrieved")
        // println "Suche nach vorhandener Seite: " + pageTitle
        Map existingPage = pages[realTitleLC]
        def page

        if (existingPage) {
            if (hasRequestedParent(existingPage, parentId)) {
                page = retrieveFullPage(existingPage.id as String)
            } else {
                page = null
            }
        } else {
            page = null
        }
        // println "Gefunden: " + page.id + " Titel: " + page.title

        if (page) {
            println "found existing page: " + page.id +" version "+page.version.number

            //extract hash from remote page to see if it is different from local one
            def remotePage = page.body.storage.value.toString().trim()

            def remoteHash = remotePage =~ /(?ms)hash: #([^#]+)#/
            remoteHash = remoteHash.size()==0?"":remoteHash[0][1]

            // println "remoteHash: " + remoteHash
            // println "localHash:  " + localHash

            if (remoteHash == localHash) {
                println "page hasn't changed!"
                deferredUpload.each {
                    uploadAttachment(page?.id, it[1], it[2], it[3])
                }
                deferredUpload = []
                // #324-dierk42: Add keywords as labels to page.
                if (keywords) {
                    addLabels(page.id, keywords)
                }
                return page.id
            } else {
                def newPageVersion = (page.version.number as Integer) + 1

                confluenceClient.updatePage(
                    page.id,
                    realTitle,
                    confluenceSpaceKey,
                    localPage,
                    newPageVersion,
                    config.confluence.pageVersionComment ?: '',
                    parentId
                )
                println "> updated page "+page.id
                deferredUpload.each {
                    uploadAttachment(page.id, it[1], it[2], it[3])
                }
                deferredUpload = []
                // #324-dierk42: Add keywords as labels to page.
                if (keywords) {
                    addLabels(page.id, keywords)
                }
                return page.id
            }
        } else {
            //#352-LuisMuniz if the existing page's parent does not match the requested parentId, fail
            if (existingPage && !hasRequestedParent(existingPage, parentId)) {
                throw new IllegalArgumentException("Cannot create page, page with the same "
                    + "title=${existingPage.title} "
                    + "with id=${existingPage.id} already exists in the space. "
                    + "A Confluence page title must be unique within a space, consider specifying a 'confluencePagePrefix' in ConfluenceConfig.groovy")
            }
            //create a page
            page = confluenceClient.createPage(
                realTitle,
                confluenceSpaceKey,
                localPage,
                config.confluence.pageVersionComment ?: '',
                parentId
            )
            println "> created page "+page?.id
            deferredUpload.each {
                uploadAttachment(page?.id, it[1], it[2], it[3])
            }
            deferredUpload = []
            // #324-dierk42: Add keywords as labels to page.
            if (keywords) {
                addLabels(page?.id, keywords)
            }
            return page?.id
        }
    }

    static def parseAnchors(page) {
        def anchors = [:]
        page.body.select('[id]').each { anchor ->
            def name = anchor.attr('id')
            anchors[name] = page.title
            anchor.before("<ac:structured-macro ac:name=\"anchor\"><ac:parameter ac:name=\"\">${name}</ac:parameter></ac:structured-macro>")
        }
        anchors
    }

    def pushPages(pages, anchors, pageAnchors, labels) {
        pages.each { page ->
            page.title = page.title.trim()
            println page.title
            def id = pushToConfluence page.title, page.body, page.parent, anchors, pageAnchors, labels
            page.children*.parent = id
            // println "Push children von id " + id
            pushPages page.children, anchors, pageAnchors, labels
            // println "Ende Push children von id " + id
        }
    }

    static def recordPageAnchor(head) {
        def a = [:]
        if (head.attr('id')) {
            a[head.attr('id')] = head.text()
        }
        a
    }

    static def promoteHeaders(tree, start, offset) {
        (start..7).each { i ->
            tree.select("h${i}").tagName("h${i-offset}").before('<br />')
        }
    }

    def retrievePageIdByName = { String name ->
        def data = confluenceClient.retrievePageIdByName(name, confluenceSpaceKey)
        return data?.results?.get(0)?.id
    }

    def getPagesRecursive(Element element, String parentId, Map anchors, Map pageAnchors, int level, int maxLevel) {
        def pages = []
        element.select("div.sect${level}").each { sect ->
            def title = sect.select("h${level + 1}").text()
            pageAnchors.putAll(recordPageAnchor(sect.select("h${level + 1}")))
            Elements pageBody
            if (level == 1) {
                pageBody = sect.select('div.sectionbody')
            } else {
                pageBody = new Elements(sect)
                pageBody.select("h${level + 1}").remove()
            }
            def currentPage = [
                title: title,
                body: pageBody,
                children: [],
                parent: parentId
            ]

            if (maxLevel > level) {
                currentPage.children.addAll(getPagesRecursive(sect, null, anchors, pageAnchors, level + 1, maxLevel))
                pageBody.select("div.sect${level + 1}").remove()
            } else {
                pageBody.select("div.sect${level + 1}").unwrap()
            }
            promoteHeaders sect, level + 2, level + 1
            pages << currentPage
            anchors.putAll(parseAnchors(currentPage))
        }
        return pages
    }

    def getPages(Document dom, String parentId, int maxLevel) {
        def anchors = [:]
        def pageAnchors = [:]
        def pages = []
        def sections = []
        def title = dom.select('h1').text()
        if (maxLevel <= 0) {
            dom.select('div#content').each { pageBody ->
                pageBody.select('div.sect2').unwrap()
                promoteHeaders pageBody, 2, 1
                def page = [title   : title,
                            body    : pageBody,
                            children: [],
                            parent  : parentId]
                pages << page
                sections = page.children
                parentId = null
                anchors.putAll(parseAnchors(page))
            }
        } else {
            // let's try to select the "first page" and push it to confluence
            dom.select('div#preamble div.sectionbody').each { pageBody ->
                pageBody.select('div.sect2').unwrap()
                def preamble = [
                    title: title,
                    body: pageBody,
                    children: [],
                    parent: parentId
                ]
                pages << preamble
                sections = preamble.children
                parentId = null
                anchors.putAll(parseAnchors(preamble))
            }
            sections.addAll(getPagesRecursive(dom, parentId, anchors, pageAnchors, 1, maxLevel))
        }
        return [pages, anchors, pageAnchors]
    }

    void execute() {
        if(config.confluence.inputHtmlFolder) {
            def htmlFolder = "${docDir}/${config.confluence.inputHtmlFolder}"
            println "Starting processing files in folder: " + config.confluence.inputHtmlFolder
            def dir = new File(htmlFolder)

            dir.eachFileRecurse (FILES) { fileName ->
                if (fileName.isFile()){
                    def map = [file: config.confluence.inputHtmlFolder+fileName.getName()]
                    config.confluence.input.add(map)
                }
            }
        }

        config.confluence.input.each { input ->
            // TODO check why this is necessary
            if(input.file) {
                input.file = confluenceService.checkAndBuildCanonicalFileName(input.file)
                //  assignend, but never used in pushToConfluence(...) (fixed here)
                // #938-mksiva: assign spaceKey passed for each file in the input
                spaceKeyInput = input.spaceKey
                confluenceSpaceKey = input.spaceKey ?: config.confluence.spaceKey
                def confluenceCreateSubpages = (input.createSubpages != null) ? input.createSubpages : config.confluence.createSubpages
                def confluenceAllInOnePage = (input.allInOnePage != null) ? input.allInOnePage : config.confluence.allInOnePage
                if (!(confluenceCreateSubpages instanceof ConfigObject && confluenceAllInOnePage instanceof ConfigObject)) {
                    println "ERROR:"
                    println "Deprecated configuration, migrate as follows:"
                    println "allInOnePage = true -> subpagesForSections = 0"
                    println "allInOnePage = false && createSubpages = false -> subpagesForSections = 1"
                    println "allInOnePage = false && createSubpages = true -> subpagesForSections = 2"
                    throw new RuntimeException("config problem")
                }
                confluenceSubpagesForSections = (input.subpagesForSections != null) ? input.subpagesForSections : config.confluence.subpagesForSections

                if (confluenceSubpagesForSections instanceof ConfigObject) {
                    confluenceSubpagesForSections = 1
                }
                //  hard to read in case of using :sectnums: -> so we add a suffix
                confluencePagePrefix = input.pagePrefix ?: config.confluence.pagePrefix
                //  added
                confluencePageSuffix = input.pageSuffix ?: config.confluence.pageSuffix
                def confluencePreambleTitle = input.preambleTitle ?: config.confluence.preambleTitle
                if (!(confluencePreambleTitle instanceof ConfigObject)) {
                    println "ERROR:"
                    println "Deprecated configuration, use first level heading in document instead of preambleTitle configuration"
                    throw new RuntimeException("config problem")
                }
                File htmlFile = new File(input.file)

                println "Publish ${input.file} to $confluenceSpaceKey at ${config.confluence.api} ..."
                baseUrl = htmlFile
                Document dom = confluenceService.parseFile(htmlFile)

                // if ancestorName is defined try to find machingAncestorId in confluence
                def retrievedAncestorId
                if (input.ancestorName) {
                    // Retrieve a page id by name
                    retrievedAncestorId = retrievePageIdByName(input.ancestorName)
                    println("Retrieved pageId for given ancestorName '${input.ancestorName}' is ${retrievedAncestorId}")
                }
                // if input does not contain an ancestorName, check if there is ancestorId, otherwise check if there is a global one
                def parentId = retrievedAncestorId ?: input.ancestorId ?: config.confluence.ancestorId

                // if parentId is still not set, create a new parent page (parentId = null)
                parentId = parentId ?: null
                //println("ancestorName: '${input.ancestorName}', ancestorId: ${input.ancestorId} ---> final parentId: ${parentId}")

                // #342-dierk42: get the keywords from the meta tags
                def keywords = confluenceService.getKeywords(dom)

                def (pages, anchors, pageAnchors) = getPages(dom, parentId, confluenceSubpagesForSections)
                //println "Pages: ${pages.size()}"
                //pages.each { page ->
                //    println "$page"
                //}
                pushPages pages, anchors, pageAnchors, keywords
                if (parentId) {
                    println "published to ${config.confluence.api - "rest/api/"}/spaces/${confluenceSpaceKey}/pages/${parentId}"
                } else {
                    println "published to ${config.confluence.api - "rest/api/"}/spaces/${confluenceSpaceKey}"
                }
            }
        }
    }
}

