package org.docToolchain.atlassian

import org.docToolchain.configuration.ConfigService
import org.docToolchain.util.TestUtils
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import spock.lang.Specification

class ConfluenceServiceSpec extends Specification {

    def "test CheckAndBuildCanonicalFileName with .html file"() {
        setup: "a config with docDir `src/docs`"
            ConfigObject config = new ConfigObject()
            config.docDir = "src/docs"
            ConfigService configService = new ConfigService(config)
            ConfluenceService confluenceService = new ConfluenceService(configService)
        when: 'the filename is `foo.html`'
            String canonicalFilePath = confluenceService.checkAndBuildCanonicalFileName("foo.html")
        then: 'the canonical file path is returned'
            canonicalFilePath == "src/docs/foo.html"
            noExceptionThrown()
    }

    def "test CheckAndBuildCanonicalFileName with .html file with trailing and leading spaces"() {
        setup: "a config with docDir `  src/docs`"
            ConfigObject config = new ConfigObject()
            config.docDir = "   src/docs"
            ConfigService configService = new ConfigService(config)
            ConfluenceService confluenceService = new ConfluenceService(configService)
        when: 'the filename is `foo.html    `'
            String canonicalFilePath = confluenceService.checkAndBuildCanonicalFileName("foo.html   ")
        then: 'the canonical file path is returned'
            canonicalFilePath == "src/docs/foo.html"
            noExceptionThrown()
    }

    def "test CheckAndBuildCanonicalFileName with .adoc file"() {
        setup: "a config with docDir `src/docs`"
            ConfigObject config = new ConfigObject()
            config.docDir = "src/docs"
            ConfigService configService = new ConfigService(config)
            ConfluenceService confluenceService = new ConfluenceService(configService)
        when: 'the filename is `foo.adoc`'
            confluenceService.checkAndBuildCanonicalFileName("foo.adoc")
        then: 'an Exception is thrown'
            thrown(RuntimeException)
    }

    def "test CheckAndBuildCanonicalFileName with .asciidoc file"() {
        setup: "a config with docDir `src/docs`"
            ConfigObject config = new ConfigObject()
            config.docDir = "src/docs"
            ConfigService configService = new ConfigService(config)
            ConfluenceService confluenceService = new ConfluenceService(configService)
        when: 'the filename is `foo.asciidoc`'
            confluenceService.checkAndBuildCanonicalFileName("foo.asciidoc")
        then: 'an Exception is thrown'
            thrown(RuntimeException)
    }

    def "test CheckAndBuildCanonicalFileName with .ad file"() {
        setup: "a config with docDir `src/docs`"
            ConfigObject config = new ConfigObject()
            config.docDir = "src/docs"
            ConfigService configService = new ConfigService(config)
            ConfluenceService confluenceService = new ConfluenceService(configService)
        when: 'the filename is `foo.ad`'
            confluenceService.checkAndBuildCanonicalFileName("foo.ad")
        then: 'an Exception is thrown'
            thrown(RuntimeException)
    }

    def "test parse .html file"() {
        setup: "a configservice and a .html file"
            ConfigService configService = new ConfigService(new ConfigObject())
            ConfluenceService confluenceService = new ConfluenceService(configService)
            File htmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/fixtures/body.html")
        when: 'the get file is parsed'
            Document dom = confluenceService.parseFile(htmlFile)
        then: 'the returned Dom is configured as expected'
            dom.outputSettings().charset().displayName() == "UTF-8"
            dom.outputSettings().prettyPrint() == false
            dom.outputSettings().escapeMode() == Entities.EscapeMode.xhtml
            noExceptionThrown()
    }

    def "GetKeywords"() {
        setup: "a DOM with keywords"
            ConfigService configService = new ConfigService(new ConfigObject())
            ConfluenceService confluenceService = new ConfluenceService(configService)
            File htmlFile = new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/fixtures/withKeywords.html")
            Document dom = confluenceService.parseFile(htmlFile)
        when: 'we want to fetch the keywords from the DOM'
            ArrayList keywords = confluenceService.getKeywords(dom)
        then: 'the returned Dom is configured as expected'
            keywords.size() == 4
            keywords[0] == "foo"
            keywords[1] == "bar"
            keywords[2] == "baz"
            keywords[3] == "hello world"
            noExceptionThrown()
    }
}
