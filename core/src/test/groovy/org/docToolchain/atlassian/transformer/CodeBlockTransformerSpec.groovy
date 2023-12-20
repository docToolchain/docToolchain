package org.docToolchain.atlassian.transformer

import org.docToolchain.util.TestUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import spock.lang.Specification

class CodeBlockTransformerSpec extends Specification {

    static final String TEST_RESOURCE_PATH = "/fixtures/atlassian/transformer/codeblockTransformer"

    CodeBlockTransformer codeBlockTransformer

    def "test codeblock transformation with subpagesForSections >= 2"() {
        setup: "we initialize the transformer"
            codeBlockTransformer = new CodeBlockTransformer()
            File expectedHtmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}${TEST_RESOURCE_PATH}/section_transformed.html")
            Element transformedHtml = new Document("").outputSettings(new Document.OutputSettings().prettyPrint(false)).html(expectedHtmlFile.text)
        when: 'we pass a HTML section into the transformer'
            File htmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}${TEST_RESOURCE_PATH}/section.html")
            Element input = new Document("").outputSettings(new Document.OutputSettings().prettyPrint(false)).html(htmlFile.text)
            def result = codeBlockTransformer.transformCodeBlock(input)
        then:  'there is no exception, newlines are preserved and the code has the expected Confluence structure'
            noExceptionThrown()
            result.size() == 1
            // need to call trim() to avoid whitespace issues, newlines are still preserved. Hence this is sane.
            input.select("div[class=content]").get(0).html().trim() == transformedHtml.select("div[class=content]").get(0).html().trim()
    }

    def "test codeblock transformation with subpagesForSections = 1"() {
        setup: "we initialize the transformer"
            codeBlockTransformer = new CodeBlockTransformer()
            File expectedHtmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}${TEST_RESOURCE_PATH}/sectionbody_transformed.html")
            Element transformedHtml = new Document("").outputSettings(new Document.OutputSettings().prettyPrint(false)).html(expectedHtmlFile.text)
        when: 'we pass a HTML section into the transformer'
            File htmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}${TEST_RESOURCE_PATH}/sectionbody.html")
            Element input = new Document("").outputSettings(new Document.OutputSettings().prettyPrint(false)).html(htmlFile.text)
            def result = codeBlockTransformer.transformCodeBlock(input)
        then: 'there is no exception, newlines are preserved and the code has the expected Confluence structure'
            noExceptionThrown()
            result.size() == 1
            // need to call trim() to avoid whitespace issues, newlines are still preserved. Hence this is sane.
            input.select("div[class=content]").get(0).html().trim() == transformedHtml.select("div[class=content]").get(0).html().trim()
    }

}
