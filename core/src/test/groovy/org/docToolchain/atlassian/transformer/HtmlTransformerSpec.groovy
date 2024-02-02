package org.docToolchain.atlassian.transformer

import org.docToolchain.util.TestUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import spock.lang.Specification

class HtmlTransformerSpec extends Specification {

    static final String TEST_RESOURCE_PATH = "/fixtures/atlassian/transformer/htmlTransformer"

    HtmlTransformer htmlTransformer

    def "test html transformation"() {
        setup: "we initialize the transformer"
            htmlTransformer = new HtmlTransformer()
            File expectedHtmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}${TEST_RESOURCE_PATH}/1355-source-code-format-transformed.html")
            String transformedHtml = expectedHtmlFile.text
        when: 'we pass a HTML section into the transformer'
            File htmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}${TEST_RESOURCE_PATH}/1355-source-code-format.html")
            Element input = new Document("").outputSettings(new Document.OutputSettings().prettyPrint(false)).html(htmlFile.text)
            def result = htmlTransformer.transformToConfluenceFormat(input)
        then:  'there is no exception, newlines are preserved and the code has the expected Confluence structure'
            noExceptionThrown()
            // need to call trim() to avoid whitespace issues, newlines are still preserved. Hence this is sane.
            result.trim() == transformedHtml.trim()
    }
}
