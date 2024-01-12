package org.docToolchain.scripts

import org.docToolchain.util.TestUtils
import org.jsoup.Jsoup
import spock.lang.Specification

class asciidoc2confluenceSpec extends Specification {

    static final ASCIIDOC2CONFLUENCE_SCRIPT = new File("./src/main/groovy/org/docToolchain/scripts/asciidoc2confluence.groovy")

    ConfigObject config

    def setup() {
        config = new ConfigObject()
        config.confluence = [
            api : 'https://my.confluence/rest/api/',
            useV1Api : true,
        ]
    }

    GroovyShell setupShell() {
        return new GroovyShell(new Binding([
            config: config
        ]))
    }

    void 'test body is parsed properly'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        script.setProperty("baseUrl", "./src/test/build/asciidoc2confluence")
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", []))
        script.setProperty("deferredUpload", [])
        def bodyFixture = new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/fixtures/body.html")
        when: 'run parseBody'
        def output = script.parseBody Jsoup.parse(bodyFixture.text) , [], []

        then: 'the attachments have a reference of'
        def attachments = Jsoup.parse(output.get("page")).getElementsByAttribute("ri:filename")
        attachments.get(0).attr("ri:filename") == "diagram_classes.png"
        attachments.get(1).attr("ri:filename") == "efb618ba9d9e33fe8dcae23b34351adc.png"
    }

    void 'test ToC generation works as expected'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        script.setProperty("baseUrl", "/Users/Foo/bar")
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of("disableToC", true)))
        script.setProperty("deferredUpload", [])
        def docFixture = new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/fixtures/confluenceDocWithoutToC.html").text

        when: 'we disable ToC generation and run generateAndAttachToC'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of("disableToC", true)))
        def outputWithoutToC = script.generateAndAttachToC docFixture

        then: 'the output has'
        Jsoup.parse(outputWithoutToC).getElementsByAttributeValue("ac:name","toc").size() == 0

        when: 'we enable ToC generation and run generateAndAttachToC'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of("disableToC", false)))
        def outputWithToC = script.generateAndAttachToC docFixture

        then: 'the output has'
        def documentWithSimpleToc = Jsoup.parse(outputWithToC)
        documentWithSimpleToc.getElementsByAttributeValue("ac:name","toc").size() == 1
        documentWithSimpleToc.getElementsByAttributeValue("ac:name","children").size() == 1

        when: 'we enable ToC generation and set custom tableOfChildren and run generateAndAttachToC'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of("disableToC", false, "tableOfChildren", '<p><ac:structured-macro ac:name="foo"><h1>bar</bar></ac:structured-macro></p>')))
        def outputWithToCAndChildren = script.generateAndAttachToC docFixture

        then: 'the output has'
        def document = Jsoup.parse(outputWithToCAndChildren)
        document.getElementsByAttributeValue("ac:name","toc").size() == 1
        document.getElementsByAttributeValue("ac:name","foo").size() == 1
        document.getElementsByAttributeValue("ac:name","children").size() == 0

        when: 'we enable ToC generation and set extraPageContent and run generateAndAttachToC'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of("disableToC", false, "extraPageContent", '<p><h1 id="extra">HELLO WORLD</h1></p>')))
        def outputWithToCAndExtraContent = script.generateAndAttachToC docFixture

        then: 'the output has an element with id "extra" and text "HELLO WORLD"'
        Jsoup.parse(outputWithToCAndExtraContent).getElementById("extra").text() == "HELLO WORLD"

        when: 'we enable ToC generation and set custom tableOfContents and run generateAndAttachToC'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of("disableToC", false, "tableOfContents", '<p><h1 id="custom">My ToC</h1></p>')))
        def outputWithCustomToC = script.generateAndAttachToC docFixture

        then: 'the output has an element with id "custom" and text "My ToC"'
        Jsoup.parse(outputWithCustomToC).getElementById("custom").text() == "My ToC"
    }

    void 'test handling of embedded images'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        script.setProperty("baseUrl", "/Users/Foo/bar")
        script.setProperty("config", Map.of("imageDirs", ["Foo/"]))
        script.setProperty("deferredUpload", [])
        def imageFixture = new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/fixtures/imageBase64.txt")
        def calculatedHash = script.MD5(imageFixture.text)

        when: 'given a clean environment'
        new File("./src/test/build/asciidoc2confluence/confluence/images/" + calculatedHash + ".png").delete()
        then: 'nothing exists'
        !new File("./src/test/build/asciidoc2confluence/confluence/images/" + calculatedHash + ".png").exists()

        when: 'the image did not exists yet with given alternative Name'
        then: 'the image exists after running handleEmbeddedImage'
        def embeddedImage = script.handleEmbeddedImage new File("./src/test/build/asciidoc2confluence").canonicalPath, "Foo", "png", imageFixture.text
        new File(embeddedImage.get("filePath")).exists()
        embeddedImage.get("fileName") == "${calculatedHash}.png"

        when: 'the image did not existed with given alternative Name'
        println(new File(embeddedImage.get("filePath")))
        script.setProperty("config", Map.of("imageDirs", []))
        def existingImage = script.handleEmbeddedImage new File("./src/test/build/asciidoc2confluence").canonicalPath, embeddedImage.get("fileName"), "png", imageFixture.text

        then: 'the image exists after running handleEmbeddedImage'
        new File(existingImage.get("filePath")).exists()
        existingImage.get("fileName") == "${calculatedHash}.png"
    }

}
