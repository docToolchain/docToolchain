package org.docToolchain.atlassian.transformer

import org.docToolchain.atlassian.constants.ConfluenceTags
import org.jsoup.nodes.Element

class LinkTransformer {

    protected List<Element> transformLinks(Element body,  anchors, pageAnchors, String jiraBaseUrl, String jiraServerId) {
        return body.select('a[href]').each { link ->
            def href = link.attr('href')
            if (href.startsWith('#')) {
                rewriteInternalLinks(link, anchors, pageAnchors)
            } else if (jiraBaseUrl && href.startsWith(jiraBaseUrl + "/browse/")) {
                rewriteJiraLinks(link, jiraServerId)
            }
        }
    }

    private rewriteInternalLinks(Element a, anchors, pageAnchors) {
        def anchor = a.attr('href').substring(1)
        def pageTitle = anchors[anchor] ?: pageAnchors[anchor]
        if (pageTitle && a.text()) {
            // as Confluence insists on link texts to be contained
            // inside CDATA, we have to strip all HTML and
            // potentially loose styling that way.
            a.html(a.text())
            a.wrap("<ac:link${anchors.containsKey(anchor) ? ' ac:anchor="' + anchor + '"' : ''}></ac:link>")
                .before("<ri:page ri:content-title=\"${realTitle pageTitle}\"/>")
                .wrap("<ac:plain-text-link-body>${ConfluenceTags.CDATA_PLACEHOLDER_START}${ConfluenceTags.CDATA_PLACEHOLDER_END}</ac:plain-text-link-body>")
                .unwrap()
        }
    }

    private rewriteJiraLinks(Element a, String jiraServerId) {
        def ticketId = a.text()
        def macroBlock = """<ac:structured-macro ac:name=\"jira\" ac:schema-version=\"1\">
                     <ac:parameter ac:name=\"key\">${ticketId}</ac:parameter>"""
        if (jiraServerId) {
            macroBlock += "<ac:parameter ac:name=\"serverId\">${jiraServerId}</ac:parameter>"
        }
        macroBlock += "</ac:structured-macro>"
        a.before(macroBlock)
        a.remove()
    }
}
