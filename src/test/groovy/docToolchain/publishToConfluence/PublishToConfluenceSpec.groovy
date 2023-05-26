package docToolchain.publishToConfluence

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.nodes.Document
import spock.lang.Specification
import spock.lang.Unroll

class PublishToConfluenceSpec extends Specification {
    void 'test default language'() {
        setup: 'load asciidoc2confluence'
        GroovyShell shell = new GroovyShell()
        def script = shell.parse(new File('./scripts/asciidoc2confluence.groovy'))

        when: 'run rewriteCodeblocks'
        Document dom = Jsoup.parse('<pre><code>none</code></pre>', 'utf-8', Parser.xmlParser())
        script.rewriteCodeblocks dom.getAllElements(), '<cdata-placeholder>', '</cdata-placeholder>'

        then: 'the language is text'
        dom.select('ac|structured-macro > ac|parameter').text() == 'text'
    }

    @Unroll
    void 'test converted language'() {
        setup: 'load asciidoc2confluence'
        GroovyShell shell = new GroovyShell()
        def script = shell.parse(new File('./scripts/asciidoc2confluence.groovy'))

        when: 'run rewriteCodeblocks'
        Document dom = Jsoup.parse("<pre><code data-lang=\"${input}\">language</code></pre>", 'utf-8', Parser.xmlParser())
        script.rewriteCodeblocks dom.getAllElements(), '<cdata-placeholder>', '</cdata-placeholder>'

        then: 'the language is converted'
        dom.select('ac|structured-macro > ac|parameter').text() == output

        where:
        input || output
        'terraform' || 'text' // fallback
        'yaml' || 'yml' // mapping
        'xml' || 'xml' // identity
    }

    void 'test body is parsed properly'() {
        setup: 'load asciidoc2confluence'
            GroovyShell shell = new GroovyShell()
            def script = shell.parse(new File('./scripts/asciidoc2confluence.groovy'))
            script.setProperty("baseUrl", "./src/test/build/exportConfluenceSpec")
            script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", []))
            script.setProperty("deferredUpload", [])
            def bodyFixture = new File('./src/test/groovy/docToolchain/publishToConfluence/fixtures/body.html');
        when: 'run parseBody'
            def output = script.parseBody Jsoup.parse(bodyFixture.text) , [], []

        then: 'the attachments have a reference of'
            def attachments = Jsoup.parse(output.get("page")).getElementsByAttribute("ri:filename")
            attachments.get(0).attr("ri:filename") == "diagram_classes.png"
            attachments.get(1).attr("ri:filename") == "efb618ba9d9e33fe8dcae23b34351adc.png"
    }

    void 'test ToC generation works as expected'() {
        setup: 'load asciidoc2confluence'
            GroovyShell shell = new GroovyShell()
            def script = shell.parse(new File('./scripts/asciidoc2confluence.groovy'))
            script.setProperty("baseUrl", "/Users/Foo/bar")
            script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of("disableToC", true)))
            script.setProperty("deferredUpload", [])
            def docFixture = new File('./src/test/groovy/docToolchain/publishToConfluence/fixtures/confluenceDocWithoutToC.html').text;

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
        setup: 'load asciidoc2confluence'
            GroovyShell shell = new GroovyShell()
            def script = shell.parse(new File('./scripts/asciidoc2confluence.groovy'))
            script.setProperty("baseUrl", "/Users/Foo/bar")
            script.setProperty("config", Map.of("imageDirs", ["Foo/"]))
            script.setProperty("deferredUpload", [])
            def imageFixture = new File('./src/test/groovy/docToolchain/publishToConfluence/fixtures/imageBase64.txt');
            def calculatedHash = script.MD5(imageFixture.text)

        when: 'given a clean environment'
            new File("./src/test/build/exportConfluenceSpec/confluence/images/" + calculatedHash + ".png").delete()
        then: 'nothing exists'
            !new File("./src/test/build/exportConfluenceSpec/confluence/images/" + calculatedHash + ".png").exists()

        when: 'the image did not exists yet with given alternative Name'
        then: 'the image exists after running handleEmbeddedImage'
            def embeddedImage = script.handleEmbeddedImage new File("./src/test/build/exportConfluenceSpec").canonicalPath, "Foo", "png", imageFixture.text
            new File(embeddedImage.get("filePath")).exists()
            embeddedImage.get("fileName") == "${calculatedHash}.png"

        when: 'the image did not existed with given alternative Name'
            println(new File(embeddedImage.get("filePath")))
            script.setProperty("config", Map.of("imageDirs", []))
            def existingImage = script.handleEmbeddedImage new File("./src/test/build/exportConfluenceSpec").canonicalPath, embeddedImage.get("fileName"), "png", imageFixture.text

        then: 'the image exists after running handleEmbeddedImage'
            new File(existingImage.get("filePath")).exists()
            existingImage.get("fileName") == "${calculatedHash}.png"
    }
}
