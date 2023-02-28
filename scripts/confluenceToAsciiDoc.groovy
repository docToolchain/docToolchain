/**
@Grapes(
        [@Grab('org.jsoup:jsoup:1.14.3')]
)
**/
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.nodes.Document
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

import java.nio.file.Files
import java.nio.file.StandardCopyOption

// parse command line arguments and display usage info
List parseCliArgs(String[] args) {
    // parse the exportet confluence entities from xml file
    if (args.size() < 2) {
        println """
Usage:
  groovy exportConfluence.groovy srcDir=[name of source dir] destDir=[name of destination dir]
  both dirs can be specified as relative path.
  the script expects the srcDir to contain the unzipped data from a confluence space XML export
  """
        System.exit(1)
    }
    parsedArgs = args.collectEntries { it.split("=").each { it.trim() } }
    def srcDir = new File(parsedArgs.srcDir)
    def destDir = new File(parsedArgs.destDir)
    destDir.mkdirs()
    return [srcDir, destDir]
}
lucidChartsIframe = false
unknownTagsStats = [:]
//takes confluence xHTML storage format and fixes some issues to be better converted by pandoc
def fixBody(pageId, String body, users, pages, space) {
    body = body
    // it seems to be a bug how CDATA sections are closed in the xHTML
            .replaceAll("]] ><", "]]><")
    // remove empty headings
            .replaceAll("<h[1-9]> </h[1-9]>","")
    // remove unecessary colspan attrib
            .replaceAll('colspan="1"',"")
    // fix colspan=2 for pandoc
            .replaceAll('colspan="2"(.*?)</(t[dh])>','$1</$2><$2></$2>')

    Document dom = Jsoup.parse(body, 'utf-8', Parser.xmlParser())
    dom.outputSettings().prettyPrint(false);//makes html() preserve linebreaks and spacing
    dom.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml); //This will ensure xhtml validity regarding entities
    dom.outputSettings().charset("UTF-8"); //does no harm :-)
    // search for image tags and convert them to HTML
    // get all ac: tags
    def acTags = []
    dom.select("*").each { element ->
        if (element.tagName().startsWith("ac:")) {
            acTags << element.tagName().replace(":", "|")
        }
    }
    //fix links which still point to confluence
    dom.select("a").each { element ->
        def href = element.attr('href')
        def regexp = href =~ '/spaces/([^/]+)/pages/([0-9]+)/'
        if (regexp.size()==1) {
            // we have a link to confluence.
            // let's try to redirect it to out static page
            def targetSpace = regexp[0][1]
            if (space.key!=targetSpace) {
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
                    element.before("<img src='{filepath}/${(version?version+"_":"1_")+(filename.replaceAll(":","_"))}' align='${alignment?:''}' width='${width?:''}' />")
                    element.remove()
                    break
                case "ac:link":
                    if (element.children().size()>0) {
                        def anchor = element.attr("ac:anchor")
                        def targetPage = element.select("ri|page").attr("ri:content-title")
                        def linkText = element.select("ac|plain-text-link-body").text()
                        if (linkText.trim()=="") {
                            linkText = element.select("ac|link-body").text()
                        }
                        targetPage = pages.find { _pageId, pageInfo -> pageInfo.title == targetPage}
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
                case [  "ac:layout",
                        "ac:layout-section",
                        "ac:layout-cell",
                        "ac:placeholder"
                ]:
                    // these tags are for multi column layouts and can't be easily converted
                    // ignore
                    break
                case "ac:structured-macro":
                    def macroName = element.attr("ac:name")
                    switch (macroName) {
                        case [
                                "drawio",
                                "excerpt-include",
                                "panel",
                        ]:
                            // ignore - not supported
                            break
                        case ["expand",
                              "expandable-comment",
                              ]:
                            //println ">>> $macroName"
                            //println element
                            break;
                        case ["view-file"]:
                            def filename = element.select(["ri|attachment"]).attr("ri:filename")
                            def version = element.select(["ri|attachment"]).attr("ri:version-at-save")
                            def height = element.select("ac|parameter[ac:name=height]")?:'400'
                            def filepath = "images/"+getFolderStructure(pages, pageId).join("/")
                            filepath = "../"*getFolderStructure(pages,pageId).size()+filepath
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
                            break;
                        case 'attachments':
                            // get all attachments for the current page and insert them as link
                            // since we don't know about attachments in this method, we delegate it for later
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
                            def localId = element.attr("ac:local-id")
                            def macroId = element.attr("ac:macro-id")
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
                            // create AsciiDoc which will be passed through
                            // only \r\n have to be encoded
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
                            //ignore toc, will be generated by asciidoctor
                            element.remove()
                            break
                        case 'children':
                            //ignore, contains no content
                            element.remove()
                            break
                        case [  'tip',
                                'info',
                                'warning',
                                'note'
                        ] :
                            // admotions
                            body = element.select("ac|rich-text-body").html()
                            def type = [
                                    'info':'NOTE',
                                    'warning':'WARNING',
                                    'note': 'CAUTION',
                                    'tip':'TIP'][macroName]
                            element.html("""
    <div class="admonition-wrapper">
    [${type}]%%CRLF%%
    ====%%CRLF%%
    ${body.replaceAll("<h([1-9])>","<h\$1>[discrete]")}%%CRLF%%
    ====%%CRLF%%
    </div>
    """)
                            element.unwrap()
                            break
                        case 'info':
                            body = element.select("ac|rich-text-body").html()
                            def title = element.select("ac|parameter[ac:name=title]").text()
                            element.html("""
    <div class="info-wrapper">
    .${title}%%CRLF%%
    ****%%CRLF%%
    ${body.replaceAll("<h([1-9])>","<h\$1>[discrete]")}%%CRLF%%
    ****%%CRLF%%
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
    ${code.replaceAll("\n","%%CRLF%%")}%%CRLF%%
    ----%%CRLF%%
    </div>
    """)
                            element.unwrap()
                            break
                        default:
                            unknownTags = (unknownTags << "ac:structured-macro $macroName").unique()
                            if (!unknownTagsStats["ac:structured-macro $macroName"]) {
                                unknownTagsStats["ac:structured-macro $macroName"]=[]
                            }
                            unknownTagsStats["ac:structured-macro $macroName"] << pageId
                    }
                    break
                case [
                        "ac:parameter",
                        "ac:rich-text-body",
                        "ac:plain-text-body"
                ]:
                    // ignore, part of structured-macro
                    break
                default:
                    unknownTags = (unknownTags << name).unique()
                    if (!unknownTagsStats[name]) {
                        unknownTagsStats[name]=[]
                    }
                    unknownTagsStats[name] << pageId

            }
        }
    }
    def html = dom.html()
                // some last dirty hacks
                // add a space in front of each link
                        .replaceAll("<a ", " <a ")
                // some links contain /#/ which is a problem for asciidoc
                        .replaceAll("[/][#][/]", '/\\\\#/')
                // remove unneeded line breaks in headlines
                        .replaceAll("<p></p><strong><br />", "<strong>")
                // remove empty bold tags
                        .replaceAll("<strong><br /></strong>", "<br />")
                // turn html titles to .-titles
                        .replaceAll("<div><div class=\"title\">([^<]+)</div></div>", ".\$1")
                // remove bold formatting and empty breaks from .-titles
                        .replaceAll("<strong><br />[.]([^<]+)</strong>", ".\$1")
    return [html, unknownTags]

}

// need to parse the entities.xml into meta data about page-structure, attachments and space
def parseMetaData( entities) {
// first parse all pages, then all contentBodies
    def pages = [:]
    def attachments = [:]
    def space = [:]
    def users = [:]
    def unknownTags = []
    println "parsing page meta-data"
    println ""

    entities.object.each { object ->
        switch (object["@class"]) {
            case 'Page':
                def pageId = object.id.toString()
                def contentStatus = object.property.find { it.@name == 'contentStatus' }
                if (contentStatus == "current") {
                    def title = object.property.find { it.@name == 'title' }.text()
                    def parentId = object.property.find { it.@name == 'parent' }.text()
                    def filename = title
                                        .replaceAll("[Ää]","ae")
                                        .replaceAll("[Üü]","ue")
                                        .replaceAll("[Öö]","oe")
                                        .replaceAll("[^a-zA-Z0-9]", "_")
                                        .replaceAll("_+", "_")
                    def position = object.property.find { it.@name == 'position' }.text()
                    println "Page: " + pageId + " - " + title
                    pages[pageId] = [title: title, parentId: parentId ?: -1, filename: filename, position: position]
                }
                break
            case 'Attachment':
                def id = object.id.toString()
                def contentStatus = object.property.find { it.@name == 'contentStatus' }
                if (contentStatus == "current") {
                    def title = object.property.find { it.@name == 'title' }.text()
                    def pageId = object.property.find { it.@name == 'containerContent' }.id.text()
                    def version = object.property.find { it.@name == 'version' }.text()
                    def originalId = object.property.find { it.@name == 'originalVersion' }.id.text()
                    attachments[id] = [filename: title, id: id, version: version, pageId: pageId, originalId: originalId]
                    println "Attachment: " + id + " - " + contentStatus + " - " + title
                }
                break
            case 'ConfluenceUserImpl':
                def id = object.id.toString()
                def name = object.property.find { it.@name == 'name' }.text()
                def atlassianAccountId = object.property.find { it.@name == 'atlassianAccountId' }.text()
                users[id] = [name: name, atlassianAccountId: atlassianAccountId]
                break
            case [
                  'ContentProperty',
                  'SpacePermission',
                  'CustomContentEntityObject',
                  'Notification',
                  'Comment',
                  'OutgoingLink',
                  'Label',
                  'Labelling',
                  'User2ContentRelationEntity',
                  'Content2ContentRelationEntity',
                  'SpaceDescription',
                  'LikeEntity',
                  'BucketPropertySetItem',
                  'ContentPermission',
                  'BlogPost'
            ]:
                // ignore
                break
            case 'Space':
                space.name = object.property.find { it.@name == 'name' }.text()
                space.key = object.property.find { it.@name == 'key' }.text()
                space.homePage = object.property.find { it.@name == 'homePage' }.id.text()
                break
            case 'BodyContent':
                // will be handled later
                break
            default:
                println object["@class"]
                break

        }
    }
    if (pages[space.homePage]==null) {
        pages[space.homePage] = [:]
    }
    pages[space.homePage].parentId = 0
    return [unknownTags, pages, attachments, space, users]
}

// confluence exports attachments in a non-human readable format
// let's change this
def void copyAttachments (attachments, pages, srcDir, destDir) {
    println "copying attachments"
    attachments.each { attachmentId, attachment ->
        def attachmentFile = new File(srcDir, 'attachments/' + attachment.pageId + '/' + (attachment.originalId ?: attachment.id) + "/" + attachment.version)
        def folderStructure = getFolderStructure(pages, attachment.pageId)
        def deepFilename = folderStructure.join("/")+"/"+attachment.version + "_" + (attachment.filename.replaceAll(":", "_"))
        if (folderStructure.size() > 1) {
            new File(destDir, "images/" + folderStructure.join("/")).mkdirs()
        }
        def destFile = new File(new File(destDir, 'images'), deepFilename)
        try {
            Files.copy(attachmentFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (Exception e) {
            println e.message
        }
    }
}

// get the folder structure for the current page through the page structure information
String[] getFolderStructure ( Map pages, String pageId) {
    def parentId = pages[pageId]?.parentId
    if (parentId && parentId != "null" && parentId != 0) {
        if (pages[parentId]) {
            return getFolderStructure(pages, parentId )  + pages[parentId].filename
        } else {
            println "parent page not found: " + parentId + " for " + pages[pageId].filename
            return []
        }
    } else {
        return []
    }
}

// extract xHTML bodies from export, convert it to HTML and then to adoc and write it to disk
def extractBodies(GPathResult entities, pages, attachments, space, users, File destDir) {
    println "extracting contentBodies"
    println ""
    def unknownTags = []
    entities.object.each { object ->
        switch (object["@class"]) {
            case 'Page':
                def pageId = object.id.toString()
                def contentStatus = object.property.find { it.@name == 'contentStatus' }
                pages[pageId]?.status = ''
                if (
                           contentStatus == "current"
                        && (pages[pageId].parentId != -1 || pages[pageId].parentId == space.homePage)
                ) {
                    pages[pageId].status = 'current'
                    metaData = pages[pageId]
                    def body = ""
                    object.collection.findAll { it.@name == 'bodyContents' }.each { it ->
                        def id = it.element.id.text()
                        body = entities.object
                                .find { it.@class == 'BodyContent' && it.id == id }
                                .property.find { it.@name == 'body' }.text()
                    }
                    def children = []
                    object.collection.findAll { it.@name == 'children' }.each { collection ->
                        collection.element.each { element ->
                            children << element.id.text()
                        }
                    }

                    def folderStructure = getFolderStructure(pages, pageId)
                    def deepFilename = folderStructure.join("/")+"/"+metaData.filename.toString()

                    // ensure that target folder exists
                    if (folderStructure.size() >= 1) {
                        new File(destDir, folderStructure.join("/")).mkdirs()
                    }
                    def outFile = new File(destDir, deepFilename + ".html")
                    // now, lets fix some body parts
                    def uTags
                    (body, uTags) = fixBody( pageId, body, users, pages, space)
                    unknownTags = (unknownTags + uTags).unique()

                    outFile.write(body, 'utf-8')

                    // convert to AsciiDoc
                    def outFilename = outFile.canonicalPath[0..-6] + ".adoc"
                    def command = "pandoc --wrap preserve -f html -t asciidoc -s ${outFile.canonicalPath} -o ${outFilename}"
                    def process = command.execute()
                    process.waitForProcessOutput(System.out, System.err)
                    if (process.exitValue()>1) {
                        println "couldn't convert ${outfile.canonicalPath}"
                    } else {
                        def outFileAdoc = new File(outFilename)
                        def filename = (outFile.toString() - destDir.toString())
                        def weightedChildren = []
                        children.each { child ->
                            weightedChildren << [weight : ((pages[child]?.position?:"-1") as Integer),
                                                include: "include::" + pages[pageId].filename + "/" + pages[child].filename + ".adoc[levelOffset=+1]",
                                                menu   : "include::" + pages[child].filename + "/_menu.adoc[]"]
                        }
                        def childIncludes = ""
                        if (weightedChildren.size()>0) {
                        childIncludes = """
    ifdef::includeChildren[]
    ${weightedChildren.sort { it.weight }.collect { it.include }.join("\n")}
    endif::includeChildren[]
    """
                        }
                        println deepFilename
                        def fileHeader = """
:jbake-menu: ${folderStructure.size()>0?folderStructure[0]:'-'}
:jbake-deep-menu: ${folderStructure.join("/")}
:jbake-status: published
:jbake-type: page_custom_menu
:jbake-order: ${metaData.position?:'0'}
:jbake-root: ${"../" * (folderStructure.size() )}
:filename: ${metaData.filename.toString()}.adoc
:filepath: ${folderStructure.join("/")}
include::{jbake-root}_config.adoc[]
ifndef::imagesdir[:imagesdir: {jbake-root}images]

"""
                        // apply some last dirty fixes
                        def adoc = outFileAdoc.text
                                // fix \r\n for passed through asciidoc
                                .replaceAll("%%CRLF%% *", "\n")
                                // fix descrete headers
                                .replaceAll("(=+) \\[discrete\\]", "[discrete]\n\$1 ")
                                // nonbreaking space asciidoc style
                                .replaceAll(" ", "{nbsp}")
                                // empty headline
                                .replaceAll("(?sm)^ [+] *\$", "")
                                // filepath attribute
                                .replaceAll("%7Bfilepath%7D", "{filepath}")
                        println(pages[pageId].title)
                        def linkedAttachments = "\n"
                        if (adoc.contains('%%attachments%%')) {
                            linkedAttachments += ".Attachments\n\n"
                            attachments.findAll{it.value.pageId == pageId}.each { attachmentId, attributes ->
                                linkedAttachments += "* link:${ "../" * (folderStructure.size() )}images/${folderStructure.join("/")}/" + attributes.version+"_"+attributes.filename +"["+attributes.filename+" (v${attributes.version})]\n"
                            }
                            adoc = adoc.replace('%%attachments%%', linkedAttachments)
                            linkedAttachments = "\n"
                        }
                        attachments.findAll{it.value.pageId == pageId}.each { attachmentId, attributes ->
                            linkedAttachments += "// attachment /images/${folderStructure.join("/")}/" + attributes.filename +"["+attributes.filename+"]\n"
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
    //                    menuFile.append("""
    //${"*" * folderStructure.size()} xref:${metaData.filename.toString()}.adoc[${pages[pageId].title}]
    //""")
                    }
                }
                break
        }
    }
    return unknownTags
}

def createMenu(pages, startPageId) {

    def pageList = pages.findAll { it.value.parentId == startPageId}
    def menu = ""
    pageList.sort{(it.value.position?:'-1') as Integer}.each { page ->
        def folderStructure = getFolderStructure(pages, page.key)
        menu +=  "*"*(folderStructure.size()+1) + " xref:{jbake-root}"+folderStructure.join("/")+'/'+page.value.filename+".adoc["+page.value.title+"]\n"
        def childPageList = pages.findAll { it.value.parentId == page.key}
        if (childPageList.size()>0) {
            menu += createMenu(pages, page.key)
        }
    }
    return menu
}
// this is where the main code starts
def (File srcDir, File destDir) = parseCliArgs(args)

println srcDir.canonicalPath
println destDir.canonicalPath
lucidInfoFile = new File( destDir,"lucidinfos.txt")
lucidInfoFile.write("", 'utf-8')

File inFile = new File(srcDir, "entities.xml")
def entities = new XmlSlurper().parseText(inFile.getText('utf-8'))

def (unknownTags,
        pages,
        attachments,
        space,
        users) = parseMetaData(entities)

println "Space: " + space

copyAttachments(attachments, pages, srcDir, destDir)

unknownTags << extractBodies(entities, pages, attachments, space, users, destDir)
unknownTags = unknownTags.unique()

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
println ""
println "pages converted: " + pages.findAll{it.value.status=='current'}.size()
println "unknown tags found: " + unknownTags.unique()
unknownTagsStats.sort{a, b -> b.value.size() <=> a.value.size()}.each { tag, tagpages ->
    println tagpages.size().toString().padLeft(5)+" : "+tag
    tagpages.each { pageid ->
        println " "*10+"- "+pages[pageid].title
    }
}

new File(destDir, '_menu.adoc').write( createMenu(pages, 0), 'utf-8')