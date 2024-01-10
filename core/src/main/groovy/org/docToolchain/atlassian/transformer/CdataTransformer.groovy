package org.docToolchain.atlassian.transformer

import org.docToolchain.atlassian.constants.ConfluenceTags
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class CdataTransformer {

    protected Element unescapeCDATASections(Element body){
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
        return new Document("")
            .outputSettings(new Document.OutputSettings().prettyPrint(false)
            .syntax(Document.OutputSettings.Syntax.xml))
            .html(html)
    }
}
