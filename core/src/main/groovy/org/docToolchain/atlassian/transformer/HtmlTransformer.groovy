package org.docToolchain.atlassian.transformer

import org.docToolchain.atlassian.constants.ConfluenceTags
import org.jsoup.nodes.Element

class HtmlTransformer {

    private CodeBlockTransformer codeBlockTransformer
    private LinkTransformer linkTransformer
    private String jiraServerId
    private String jiraBaseUrl

    HtmlTransformer() {
        this.codeBlockTransformer = new CodeBlockTransformer()
        this.linkTransformer = new LinkTransformer()
    }

    HtmlTransformer withJiraIntegration(String jiraBaseUrl) {
        this.jiraBaseUrl = jiraBaseUrl
        return this
    }

    HtmlTransformer usingOnPremiseJira(String jiraServerId) {
        this.jiraServerId = jiraServerId
        return this
    }

    String transformToConfluenceFormat(Element body,  anchors, pageAnchors) {
        codeBlockTransformer.transformCodeBlock(body)
        linkTransformer.transformLinks(body, anchors, pageAnchors, jiraBaseUrl, jiraServerId)
        return sanitizeBody(body)
    }

    private String sanitizeBody(Element body){
        String html = body.html().trim()
        def start = html.indexOf(ConfluenceTags.CDATA_PLACEHOLDER_START)
        while (start > -1) {
            def end = html.indexOf(ConfluenceTags.CDATA_PLACEHOLDER_END, start)
            if (end > -1) {
                def prefix = html.substring(0, start) + ConfluenceTags.CDATA_PLACEHOLDER_START
                def suffix = html.substring(end)
                def unescaped = html.substring(start + ConfluenceTags.CDATA_PLACEHOLDER_START.length(), end)
                    .replaceAll('&lt;', '<').replaceAll('&gt;', '>')
                    .replaceAll('&amp;', '&')
                html = prefix + unescaped + suffix
            }
            start = html.indexOf(ConfluenceTags.CDATA_PLACEHOLDER_START, start + 1)
        }
        return html
            .replaceAll('<br>','<br />')
            .replaceAll('</br>','<br />')
            .replaceAll('<a([^>]*)></a>','')
            .replaceAll(ConfluenceTags.CDATA_PLACEHOLDER_START,'<![CDATA[')
            .replaceAll(ConfluenceTags.CDATA_PLACEHOLDER_END,']]>')
        // workaround for #402
            .replaceAll('(?m)(ac:name="language">)([\n\r\t ]*)([a-z]+)([\n\r\t ]*)(</ac)','$1$3$5')
    }
}
