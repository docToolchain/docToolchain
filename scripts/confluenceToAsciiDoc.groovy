// Driver: convert a Confluence space XML export (entities.xml + attachments)
// to a hierarchical AsciiDoc set.
//
// Shared conversion logic lives in scripts/confluenceConverter.groovy.
// This file is responsible for the XML-specific ingest (parseMetaData,
// XML-driven iteration over pages) and for the on-disk attachment layout
// that a Confluence space export uses.

import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult

import java.nio.file.Files
import java.nio.file.StandardCopyOption

// locate and evaluate the shared converter (works both via the Gradle task,
// which sets projectDir, and standalone `groovy confluenceToAsciiDoc.groovy ...`)
def converterFile = binding.hasVariable('projectDir')
        ? new File(projectDir as File, 'scripts/confluenceConverter.groovy')
        : new File('scripts/confluenceConverter.groovy')
if (!converterFile.exists()) {
    throw new FileNotFoundException("Cannot locate confluenceConverter.groovy (looked at ${converterFile.absolutePath})")
}
evaluate(converterFile)

// parse command line arguments and display usage info
List parseCliArgs(String[] args) {
    if (args.size() < 2) {
        println """
Usage:
  groovy exportConfluence.groovy srcDir=[name of source dir] destDir=[name of destination dir] [rootPageId=<id>] [rootPageTitle=<title>]
  both dirs can be specified as relative path.
  the script expects the srcDir to contain the unzipped data from a confluence space XML export.
  Optional:
    rootPageId    - restrict the export to the page with this id and all its descendants
    rootPageTitle - restrict the export to the page with this exact title and all its descendants
                    (only one of rootPageId / rootPageTitle should be given; rootPageId wins)
  """
        System.exit(1)
    }
    // split on the first '=' only so values may contain '=' characters (e.g. page titles)
    parsedArgs = args.collectEntries {
        def idx = it.indexOf('=')
        idx < 0 ? [(it.trim()): ''] : [(it.substring(0, idx).trim()): it.substring(idx + 1).trim()]
    }
    def srcDir = new File(parsedArgs.srcDir)
    def destDir = new File(parsedArgs.destDir)
    destDir.mkdirs()
    def rootPageId = parsedArgs.rootPageId ?: null
    def rootPageTitle = parsedArgs.rootPageTitle ?: null
    return [srcDir, destDir, rootPageId, rootPageTitle]
}

// parse entities.xml into pages / attachments / space / users metadata
def parseMetaData(entities) {
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
                    def filename = sanitizeFilename(title)
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
    if (pages[space.homePage] == null) {
        pages[space.homePage] = [:]
    }
    pages[space.homePage].parentId = 0
    return [unknownTags, pages, attachments, space, users]
}

// Confluence's XML export lays out attachments as
//   attachments/<pageId>/<originalIdOrId>/<version>
// Here we reshape them into   images/<folderStructure>/<version>_<filename>   under destDir.
def copyAttachments(attachments, pages, srcDir, destDir) {
    println "copying attachments"
    attachments.each { attachmentId, attachment ->
        def attachmentFile = new File(srcDir, 'attachments/' + attachment.pageId + '/' + (attachment.originalId ?: attachment.id) + "/" + attachment.version)
        def folderStructure = getFolderStructure(pages, attachment.pageId)
        def deepFilename = folderStructure.join("/") + "/" + attachment.version + "_" + (attachment.filename.replaceAll(":", "_"))
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

// Iterate XML pages, extract each page's body + child ids, then delegate to writePage.
def extractBodiesFromXml(GPathResult entities, Map pages, Map attachments, Map space, Map users, File destDir) {
    println "extracting contentBodies"
    println ""
    def unknownTags = []
    entities.object.each { object ->
        if (object["@class"] != 'Page') return
        def pageId = object.id.toString()
        def contentStatus = object.property.find { it.@name == 'contentStatus' }
        pages[pageId]?.status = ''
        if (
                pages[pageId] == null
                        || contentStatus != "current"
                        || !(pages[pageId].parentId != -1 || pages[pageId].parentId == space.homePage)
        ) {
            return
        }
        pages[pageId].status = 'current'
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
                def childId = element.id.text()
                // skip children that were filtered out (subtree mode)
                if (pages[childId] != null) {
                    children << childId
                }
            }
        }
        def uTags = writePage(pageId, body, children, pages, attachments, space, users, destDir)
        unknownTags = (unknownTags + uTags).unique()
    }
    return unknownTags
}

// ---------------- main ----------------
def (File srcDir, File destDir, String rootPageId, String rootPageTitle) = parseCliArgs(args)

println srcDir.canonicalPath
println destDir.canonicalPath
lucidInfoFile = new File(destDir, "lucidinfos.txt")
lucidInfoFile.write("", 'utf-8')

File inFile = new File(srcDir, "entities.xml")
def entities = new XmlSlurper().parseText(inFile.getText('utf-8'))

def (unknownTags,
     pages,
     attachments,
     space,
     users) = parseMetaData(entities)

println "Space: " + space

// optionally restrict the export to a subtree rooted at rootPageId / rootPageTitle
if (rootPageTitle && !rootPageId) {
    rootPageId = findRootIdByTitle(pages, rootPageTitle)
    println "Resolved rootPageTitle '${rootPageTitle}' to rootPageId '${rootPageId}'"
}
if (rootPageId) {
    def fullSize = pages.size()
    (pages, attachments) = filterToSubtree(pages, attachments, rootPageId)
    space.homePage = rootPageId
    println "Filtered to subtree under page '${pages[rootPageId]?.title}' (id=${rootPageId}): ${pages.size()} of ${fullSize} pages, ${attachments.size()} attachments"
}

copyAttachments(attachments, pages, srcDir, destDir)

unknownTags << extractBodiesFromXml(entities, pages, attachments, space, users, destDir)
unknownTags = unknownTags.unique()

new File(destDir, '_config.adoc').write("""
++++
<style>
div.ulist ul {
margin-left: 1em !important;
}
</style>
++++

""", 'utf-8')
println ""
println "pages converted: " + pages.findAll { it.value.status == 'current' }.size()
println "unknown tags found: " + unknownTags.unique()
unknownTagsStats.sort { a, b -> b.value.size() <=> a.value.size() }.each { tag, tagpages ->
    println tagpages.size().toString().padLeft(5) + " : " + tag
    tagpages.each { pageid ->
        println " " * 10 + "- " + pages[pageid]?.title
    }
}

new File(destDir, '_menu.adoc').write(createMenu(pages, 0), 'utf-8')
