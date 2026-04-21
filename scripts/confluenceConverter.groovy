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
// Strip leading chapter numbers ("5.2.4. Title" -> "Title") from headings.
// AsciiDoc can re-add its own numbering via :sectnums: if desired, so by default
// we remove the hand-written prefix Confluence often carries forward.
if (!binding.variables.containsKey('stripChapterNumbering')) {
    stripChapterNumbering = true
}

// get the folder structure for the current page through the page structure information.
// Uses `filename` (the original sanitised name) — this governs XHTML paths, image
// paths, and the {filepath} attribute substituted into image references.
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

// Same as getFolderStructure but reads `adocFilename` (prefix-stripped filename)
// when present, falling back to `filename`. This governs the .adoc output paths,
// include:: directives, and _menu.adoc xref paths. The two tracks (original for
// images/XHTML, stripped for .adoc) allow shortening file paths without breaking
// image references.
getAdocFolderStructure = { Map pages, String pageId ->
    def parentId = pages[pageId]?.parentId
    if (parentId && parentId != "null" && parentId != 0) {
        if (pages[parentId]) {
            return getAdocFolderStructure(pages, parentId) + (pages[parentId].adocFilename ?: pages[parentId].filename)
        } else {
            println "parent page not found: " + parentId + " for " + (pages[pageId].adocFilename ?: pages[pageId].filename)
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
fixBody = { String pageId, String body, Map users, Map pages, Map attachments, Map space ->
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
    // Strip <br/>-only cells. Confluence emits <td><br/></td> for "empty"
    // fields; pandoc would render the <br/> as a stray " +" (AsciiDoc hard
    // line break) which ends up as a literal "+" in the output. We ONLY
    // empty cells whose direct children are all <br/> and which carry no
    // text - cells containing images, paragraphs, nested tables etc. stay
    // untouched.
    dom.select("td, th").each { cell ->
        def kids = cell.children()
        def allBr = !kids.isEmpty() && kids.every { it.tagName() == 'br' }
        if (allBr && cell.text().trim().isEmpty()) {
            cell.empty()
        }
    }
    // Detect row-header tables (every body row's first cell is a <th>, like
    // Confluence's Status/Author/Date metadata blocks) and flag them so the
    // post-pandoc step can emit a proper `[cols="h,1,...]` specifier. That's
    // cleaner than wrapping each <th> in <strong>: AsciiDoc's "h" column style
    // renders the whole column as a header (bold/grey), which is what the
    // semantic already asks for.
    // (see https://docs.asciidoctor.org/asciidoc/latest/tables/format-column-content/)
    dom.select("table").each { table ->
        def tbody = table.selectFirst("tbody")
        def rows = (tbody ? tbody.children() : table.children())
                .findAll { it.tagName() == 'tr' }
        if (rows.isEmpty()) return
        def allRowsStartWithTh = rows.every { row ->
            def firstCell = row.children().find { ['td', 'th'].contains(it.tagName()) }
            firstCell?.tagName() == 'th'
        }
        if (!allRowsStartWithTh) return
        def numCols = rows.first().children().findAll { ['td', 'th'].contains(it.tagName()) }.size()
        if (numCols < 2) return
        // Marker picked up by writePage after pandoc. Use '-' separators
        // because pandoc escapes '_'.
        table.before("<p>%%TABLE-ROWHEADER-${numCols}%%</p>")
        rows.each { row ->
            row.select("th").each { th ->
                th.tagName("td")
                th.removeAttr("scope")
            }
        }
    }
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
                    def riFilename = element.select(["ri|attachment"]).attr("ri:filename")
                    def riVersion = element.select(["ri|attachment"]).attr("ri:version-at-save")
                    // Resolve against the attachments map so drawio-merged PNGs
                    // (where filename was rewritten to "<base>.drawio.png" but
                    // the XHTML still references the original name) point at the
                    // renamed file. originalFilename is set to the name Confluence
                    // stored in <ri:attachment ri:filename=...>; filename may have
                    // been rewritten by the merge step.
                    def imgAtt = attachments.find {
                        it.value.pageId == pageId && (
                                it.value.originalFilename == riFilename ||
                                        it.value.filename == riFilename
                        )
                    }?.value
                    def actualFilename = imgAtt?.filename ?: riFilename
                    def actualVersion = imgAtt?.version ?: riVersion ?: '1'
                    element.before("<img src='{filepath}/${actualVersion}_${actualFilename.replaceAll(':', '_').replaceAll(' ', '_')}' align='${alignment ?: ''}' width='${width ?: ''}' />")
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
                                // All anchor IDs are prefixed with '_' (see the
                                // %%ANCHOR%% post-processing) to ensure they start
                                // with a letter/underscore (AsciiDoc / HTML requirement).
                                // References must use the same prefixed form.
                                targetLink += "#_" + anchor
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
                    // inline comment markers wrap text with no visible markup; unwrap so
                    // the underlying text survives but the tag doesn't leak into the output
                    element.unwrap()
                    break
                case ["ac:layout",
                      "ac:layout-section",
                      "ac:layout-cell"
                ]:
                    // multi-column layouts have no HTML equivalent; keep children, drop the
                    // wrapper so pandoc doesn't see unknown tags in the input
                    element.unwrap()
                    break
                case "ac:placeholder":
                    // empty placeholder, no content worth keeping
                    element.remove()
                    break
                case "ac:structured-macro":
                    def macroName = element.attr("ac:name")
                    switch (macroName) {
                        case 'drawio':
                            // The drawio macro is rendered by the Confluence plugin at view
                            // time; the storage format only carries macro parameters. We look
                            // up the matching PNG attachment (Confluence stores it under the
                            // name "<diagramName>.png") and emit a regular image reference.
                            // After the drawio merge step in the API driver, the actual file
                            // on disk may have been renamed to "<diagramName>.drawio.png"
                            // (with the XML embedded as an iTXt chunk); matching on
                            // originalFilename keeps that transparent here.
                            def diagramName = element.select("ac|parameter[ac:name=diagramName]").text()
                            def diagramWidth = element.select("ac|parameter[ac:name=diagramWidth]").text()
                            if (diagramName) {
                                def originalPng = diagramName + ".png"
                                def att = attachments.find {
                                    it.value.pageId == pageId && (
                                            it.value.originalFilename == originalPng ||
                                                    it.value.filename == originalPng
                                    )
                                }?.value
                                if (att) {
                                    def version = att.version ?: '1'
                                    def actualFilename = att.filename ?: originalPng
                                    def widthAttr = diagramWidth ? " width='${diagramWidth}'" : ""
                                    element.before("<img src='{filepath}/${version}_${actualFilename.replaceAll(':', '_').replaceAll(' ', '_')}'${widthAttr} />")
                                }
                            }
                            element.remove()
                            break
                        case 'captioneditem':
                            // Wraps content (typically an image or drawio) with an anchor and
                            // optional caption. Keep the inner body; promote the anchor to an
                            // AsciiDoc block anchor via a placeholder (pandoc would otherwise
                            // escape '[[' as '++[[++'). Use DIRECT children for parameter
                            // access / removal so nested macros' parameters aren't touched.
                            def captionedAnchorParam = element.children().find {
                                it.tagName() == 'ac:parameter' && it.attr('ac:name') == 'anchor'
                            }
                            def captionedAnchor = captionedAnchorParam?.text() ?: ''
                            if (captionedAnchor) {
                                element.before("\n<p>%%ANCHOR%%${captionedAnchor}%%ANCHOR-END%%</p>\n")
                            }
                            def captionedBody = element.select("ac|rich-text-body").first()
                            if (captionedBody) captionedBody.unwrap()
                            element.children().findAll { it.tagName() == 'ac:parameter' }.each { it.remove() }
                            element.unwrap()
                            break
                        case 'details':
                            // The `details` macro wraps a table of page metadata
                            // (Status / Author / Date etc.). The table itself converts
                            // fine through pandoc, so we just strip the macro wrapper.
                            // Use DIRECT children for parameter removal - otherwise we'd
                            // strip parameters of nested macros (especially `status`
                            // inside the metadata table), breaking those handlers.
                            def detailsBody = element.select("ac|rich-text-body").first()
                            if (detailsBody) detailsBody.unwrap()
                            element.children().findAll { it.tagName() == 'ac:parameter' }.each { it.remove() }
                            element.unwrap()
                            break
                        case 'status':
                            // Inline coloured status badge (e.g. Yellow "wip"). AsciiDoc
                            // has no built-in coloured-label; we emit an inline role
                            // `[.status.<lowercase-colour>]#<title>#` so themes can style
                            // it. Placeholders keep the square brackets out of pandoc's
                            // escape treatment.
                            def statusColour = (element.children().find {
                                it.tagName() == 'ac:parameter' && it.attr('ac:name') == 'colour'
                            }?.text()) ?: 'Grey'
                            def statusTitle = (element.children().find {
                                it.tagName() == 'ac:parameter' && it.attr('ac:name') == 'title'
                            }?.text()) ?: ''
                            if (statusTitle) {
                                // No <span> wrapper: pandoc sometimes drops raw <span>s
                                // inside tables, hiding our placeholder. Emitting as a
                                // plain text node inside the cell works reliably.
                                element.before("%%STATUS-BEGIN-${statusColour}%%${statusTitle}%%STATUS-END%%")
                            }
                            element.remove()
                            break
                        case 'contributors':
                            // Renders the list of contributors for this page. The API
                            // driver populates pages[pageId].contributors via the
                            // history.contributors.publishers expand; the XML driver
                            // currently leaves it empty.
                            def names = pages[pageId]?.contributors ?: []
                            if (names) {
                                element.before("<span>${names.join(', ')}</span>")
                            }
                            element.remove()
                            break
                        case 'table-filter':
                            // table-filter wraps tables with filter controls. The filter
                            // metadata is in ac:parameter children (would leak as text).
                            // Keep the inner body (which may contain a real table) and
                            // strip the wrapper + params. Same DOM-preserving unwrap as
                            // expand/admonition.
                            def tfBody = element.select("ac|rich-text-body").first()
                            if (tfBody) tfBody.unwrap()
                            element.children().findAll { it.tagName() == 'ac:parameter' }.each { it.remove() }
                            element.unwrap()
                            break
                        case 'detailssummary':
                            // Dynamic child-page listing via CQL — cannot be replicated
                            // in static AsciiDoc. Drop it entirely.
                            element.remove()
                            break
                        case ["excerpt-include",
                              "panel",
                              "expandable-comment"
                        ]:
                            // not supported - drop the macro (including children, to avoid
                            // leaking ac:parameter text into the output). `expand` is
                            // handled separately above so we keep its body.
                            element.remove()
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
                            // Confluence's standard page-footer pattern is
                            // <hr/><p><children/></p>. Remove the macro, the
                            // enclosing empty <p>, and the preceding <hr/> so
                            // no stray horizontal rule remains.
                            def childrenParent = element.parent()
                            element.remove()
                            if (childrenParent?.tagName() == 'p' &&
                                    childrenParent.text().trim().isEmpty() &&
                                    childrenParent.children().isEmpty()) {
                                def prev = childrenParent.previousElementSibling()
                                if (prev?.tagName() == 'hr') prev.remove()
                                childrenParent.remove()
                            }
                            break
                        case ['tip',
                              'info',
                              'warning',
                              'note'
                        ]:
                            // Admonitions need [TYPE] and ==== as block attribute / delimiter
                            // lines in the AsciiDoc output, but pandoc escapes literal "[" and
                            // "]" in HTML text as "++[++" / "++]++". We therefore emit unique
                            // placeholders here and substitute them in writePage AFTER pandoc
                            // has run. Also: instead of rewriting the body as a string (which
                            // would detach any nested ac: macros and skip them in the outer
                            // iteration), we manipulate the DOM so nested drawio/image/etc.
                            // stay in place.
                            // NB: use DIRECT children for parameter access / removal. A nested
                            // macro (e.g. info inside expand) also has ac:parameter children,
                            // and element.select(...) would pick them up. Same for the later
                            // remove() pass - otherwise we'd strip the nested macro's params
                            // before its own handler runs.
                            def admonTitleParam = element.children().find {
                                it.tagName() == 'ac:parameter' && it.attr('ac:name') == 'title'
                            }
                            def admonTitle = admonTitleParam?.text() ?: ''
                            def admonType = [
                                    'info'   : 'NOTE',
                                    'warning': 'WARNING',
                                    'note'   : 'CAUTION',
                                    'tip'    : 'TIP'][macroName]
                            // placeholder tokens use '-' as separator; pandoc escapes '_' as '++_++'
                            def admonTitlePart = admonTitle ? "\n<p>%%ADMON-TITLE%%${admonTitle.trim()}%%ADMON-TITLE-END%%</p>\n" : ""
                            element.before(admonTitlePart + "<p>%%ADMON-BEGIN-${admonType}%%</p>\n")
                            element.after("\n<p>%%ADMON-END%%</p>")
                            element.select("ac|rich-text-body").first()?.select("h1,h2,h3,h4,h5,h6")?.each { h ->
                                h.before("<p>%%DISCRETE%%</p>")
                            }
                            def admonBody = element.select("ac|rich-text-body").first()
                            if (admonBody) admonBody.unwrap()
                            element.children().findAll { it.tagName() == 'ac:parameter' }.each { it.remove() }
                            element.unwrap()
                            break
                        case 'expand':
                            // arc42 templates use `expand` for collapsible sections.
                            // Same DOM-preserving + direct-children treatment as admonitions.
                            def expandTitleParam = element.children().find {
                                it.tagName() == 'ac:parameter' && it.attr('ac:name') == 'title'
                            }
                            def expandTitle = expandTitleParam?.text() ?: ''
                            def expandTitlePart = expandTitle ? "\n<p>%%EXPAND-TITLE%%${expandTitle.trim()}%%EXPAND-TITLE-END%%</p>\n" : ""
                            element.before(expandTitlePart + "<p>%%EXPAND-BEGIN%%</p>\n")
                            element.after("\n<p>%%EXPAND-END%%</p>")
                            def expandBody = element.select("ac|rich-text-body").first()
                            if (expandBody) expandBody.unwrap()
                            element.children().findAll { it.tagName() == 'ac:parameter' }.each { it.remove() }
                            element.unwrap()
                            break
                        case 'anchor':
                            // Same placeholder technique as captioneditem's anchor - pandoc
                            // would otherwise escape "[[anchor]]" to "++[[++anchor++]]++".
                            def anchor = element.select("ac|parameter").text()
                            if (anchor) {
                                element.before("\n<p>%%ANCHOR%%${anchor}%%ANCHOR-END%%</p>\n")
                            }
                            element.remove()
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
    // Optionally strip hand-written chapter numbering from heading opens:
    //   "<h1>5.2.4. Title ..." -> "<h1>Title ..."
    if (stripChapterNumbering) {
        html = html.replaceAll(/(<h[1-9](?:\s[^>]*)?>)\s*\d+(?:\.\d+)*\.?\s+/, '$1')
    }
    return [html, unknownTags]
}

// Convert and write one page to disk (HTML via fixBody, then pandoc to AsciiDoc,
// then post-processing + file header + child includes + attachment links).
// Returns the list of unknown tags encountered while processing this page.
writePage = { String pageId, String rawBody, List<String> childIds,
              Map pages, Map attachments, Map space, Map users, File destDir ->
    def metaData = pages[pageId]
    def adocFilename = metaData.adocFilename ?: metaData.filename
    // Original folder structure (based on `filename`) — used for image/{filepath} refs.
    def folderStructure = getFolderStructure(pages, pageId)
    // Prefix-stripped folder structure (based on `adocFilename`) — used for .adoc output
    // paths, include:: directives, and jBake attributes. When no prefix regex is set,
    // adocFilename == filename and the two structures are identical.
    def adocFolderStructure = getAdocFolderStructure(pages, pageId)
    def deepFilename = adocFolderStructure.join("/") + "/" + adocFilename.toString()

    if (adocFolderStructure.size() >= 1) {
        new File(destDir, adocFolderStructure.join("/")).mkdirs()
    }
    def outFile = new File(destDir, deepFilename + ".html")
    def (String body, List uTags) = fixBody(pageId, rawBody ?: '', users, pages, attachments, space)

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
        def parentAdocFn = pages[pageId].adocFilename ?: pages[pageId].filename
        def childAdocFn = pages[child].adocFilename ?: pages[child].filename
        weightedChildren << [weight : ((pages[child]?.position ?: "-1") as Integer),
                             include: "include::" + parentAdocFn + "/" + childAdocFn + ".adoc[levelOffset=+1]",
                             menu   : "include::" + childAdocFn + "/_menu.adoc[]"]
    }
    def childIncludes = ""
    if (weightedChildren.size() > 0) {
        // Natural sort: pad numeric segments in the include path so
        // "arc42_2_..." < "arc42_10_..." instead of the alphabetical
        // "arc42_10_..." < "arc42_2_...". The REST API often returns
        // children sorted by title (alphabetical), so the position values
        // we assigned via eachWithIndex just mirror that wrong order.
        // Natural sort on the filename is a better universal default.
        def naturalKey = { String s -> s.replaceAll(/\d+/) { it.padLeft(10, '0') } }
        // No leading whitespace in the template — AsciiDoc treats 4+ spaces
        // as a literal/code block, which would swallow the ifdef and includes.
        childIncludes = """
ifdef::includeChildren[]
${weightedChildren.sort { a, b -> naturalKey(a.include) <=> naturalKey(b.include) }.collect { it.include }.join("\n")}
endif::includeChildren[]
"""
    }
    println deepFilename
    def fileHeader = """
:jbake-menu: ${adocFolderStructure.size() > 0 ? adocFolderStructure[0] : '-'}
:jbake-deep-menu: ${adocFolderStructure.join("/")}
:jbake-status: published
:jbake-type: page_custom_menu
:jbake-order: ${metaData.position ?: '0'}
:jbake-root: ${"../" * (adocFolderStructure.size())}
:filename: ${adocFilename.toString()}.adoc
:filepath: ${folderStructure.join("/")}
include::{jbake-root}_config.adoc[]
ifdef::show-microsite-menu[]
include::{jbake-root}_menu.adoc[]
++++
<!-- endtoc -->
++++
endif::show-microsite-menu[]
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
            // Placeholders use '-' (not '_') as separator - pandoc's AsciiDoc writer
            // escapes '_' as '++_++' (guard against inline-italic collisions), which
            // would break any regex looking for intact underscores.
            .replaceAll(/\s*%%DISCRETE%%\s*/, '\n\n[discrete]\n')
    // Admonition and expand: match an optional TITLE placeholder immediately
    // followed by the BEGIN placeholder, so we can both (a) guarantee a blank
    // line BEFORE the block and (b) keep the ".title" line directly attached
    // to the block attribute (no blank line between, which AsciiDoc needs for
    // the title to stick). Admonitions use 4-char "====", collapsibles use
    // 6-char "======" so nesting works both ways.
    adoc = adoc.replaceAll(/\s*(?:%%ADMON-TITLE%%([\s\S]*?)%%ADMON-TITLE-END%%\s*)?%%ADMON-BEGIN-(\w+)%%\s*/) { full, title, type ->
        def titleLine = (title && title.trim()) ? ".${title.trim()}\n" : ""
        "\n\n${titleLine}[${type}]\n====\n\n"
    }
    adoc = adoc.replaceAll(/\s*%%ADMON-END%%\s*/, '\n\n====\n\n')
    adoc = adoc.replaceAll(/\s*(?:%%EXPAND-TITLE%%([\s\S]*?)%%EXPAND-TITLE-END%%\s*)?%%EXPAND-BEGIN%%\s*/) { full, title ->
        def titleLine = (title && title.trim()) ? ".${title.trim()}\n" : ""
        "\n\n${titleLine}[%collapsible]\n======\n\n"
    }
    adoc = adoc.replaceAll(/\s*%%EXPAND-END%%\s*/, '\n\n======\n\n')
    // Anchor placeholder -> AsciiDoc block anchor. Blank line BEFORE (so it's
    // separated from the previous block), single newline AFTER (so the anchor
    // sticks to the next block - a blank line here would detach it). Also
    // clean up any "++_++" pandoc injected into underscore-containing names.
    adoc = adoc.replaceAll(/\s*%%ANCHOR%%([^%]+)%%ANCHOR-END%%\s*/) { full, name ->
        def cleanName = '_' + name.replaceAll('\\+\\+_\\+\\+', '_')
        "\n\n[[${cleanName}]]\n"
    }
    // Row-header table placeholder -> explicit `[cols="h,1,..."]` attribute on
    // the table that follows. Consumes pandoc's auto-generated `[cols=...]`
    // line if present so we don't end up with two attribute lists.
    adoc = adoc.replaceAll(/%%TABLE-ROWHEADER-(\d+)%%\s*\n+\s*(?:\[[^\]]*\]\s*\n)?\|===/) { full, numCols ->
        def n = numCols as Integer
        def colsSpec = 'h' + (',1' * (n - 1))
        "[cols=\"${colsSpec}\"]\n|==="
    }
    // Status badge placeholder -> AsciiDoc inline role. Closure so we can
    // lowercase the colour for a stable CSS class.
    adoc = adoc.replaceAll(/%%STATUS-BEGIN-(\w+)%%([\s\S]*?)%%STATUS-END%%/) { full, colour, title ->
        "[.status.${colour.toLowerCase()}]#${title.trim()}#"
    }
    // Pandoc always emits "image:foo[]" (inline image syntax) even when the
    // image is on its own line. AsciiDoc needs "image::foo[]" (block image)
    // for standalone figures, so promote any line whose sole content is
    // image:... to the block form. Inline images embedded in text are left
    // untouched because the regex is anchored to start-of-line.
    adoc = adoc.replaceAll(/(?m)^(\s*)image:(?!:)/, '$1image::')
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
    // Natural sort by title so "Chapter 2" < "Chapter 10" (numeric segments
    // compared as numbers). The REST API's child/page endpoint often returns
    // children sorted alphabetically by title, which gives wrong order for
    // numbered page names.
    def naturalKey = { String s -> s.replaceAll(/\d+/) { it.padLeft(10, '0') } }
    pageList.sort { a, b -> naturalKey(a.value.title) <=> naturalKey(b.value.title) }.each { page ->
        def adocFolder = getAdocFolderStructure(pages, page.key)
        def adocFn = page.value.adocFilename ?: page.value.filename
        menu += "*" * (adocFolder.size() + 1) + " xref:{jbake-root}" + adocFolder.join("/") + '/' + adocFn + ".adoc[" + page.value.title + "]\n"
        def childPageList = pages.findAll { it.value.parentId == page.key }
        if (childPageList.size() > 0) {
            menu += createMenu(pages, page.key)
        }
    }
    return menu
}
