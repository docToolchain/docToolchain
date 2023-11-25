package org.docToolchain.atlassian.transformer

import org.docToolchain.atlassian.constants.ConfluenceTags
import org.jsoup.nodes.Element

class HtmlTransformer {

    private CodeBlockTransformer codeBlockTransformer
    private CdataTransformer cdataTransformer

    HtmlTransformer() {
        this.codeBlockTransformer = new CodeBlockTransformer()
        this.cdataTransformer = new CdataTransformer()
    }

    String transformToConfluenceFormat(Element body) {
        codeBlockTransformer.transformCodeBlock(body)
        Element transformedElement = cdataTransformer.unescapeCDATASections(body)
        String html = transformedElement.html()
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
