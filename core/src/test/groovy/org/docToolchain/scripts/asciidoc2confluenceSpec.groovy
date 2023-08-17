package org.docToolchain.scripts

import groovy.json.JsonSlurper
import org.docToolchain.atlassian.ConfluenceClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import spock.lang.Specification
import spock.lang.Unroll

class asciidoc2confluenceSpec extends Specification {

    static final ASCIIDOC2CONFLUENCE_SCRIPT = new File("./src/main/groovy/org/docToolchain/scripts/asciidoc2confluence.groovy")

    GroovyShell setupShell() {
        def config = new ConfigObject()
        config.confluence = [
            api : 'https://my.confluence/rest/api/',
            useV1Api : true,
        ]
        return new GroovyShell(new Binding([
            config: config
        ]))
    }

    void 'test space output'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'

        def jsonSlurper = new JsonSlurper()
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        script.setProperty("confluenceClient", Stub(constructorArgs: ["mock", "mock"],ConfluenceClient.class){
            fetchPagesBySpaceKey(_, _, _) >> [data: jsonSlurper.parse(new File('./src/test/resources/asciidoc2confluence/space.json'))] >>
                // no more results
                [data: [results: []]] })
        when: 'run retrieveAllPagesBySpace'
        def result = script.retrieveAllPagesBySpace('spaceKey', 100)
        then: 'the pages are given'
        result.size() == 10
        result == ['page old 1': [title: 'page old 1', id: '688183', parentId: '47456033'],
                   'page old 2': [title: 'page old 2', id: '1081416', parentId: null],
                   'page old 3': [title: 'page old 3', id: '4390965', parentId: '1081416'],
                   'page old 4': [title: 'page old 4', id: '4391029', parentId: '4390965'],
                   'page old 5': [title: 'page old 5', id: '4391039', parentId: '14094801'],
                   'page old 6': [title: 'page old 6', id: '4391048', parentId: '4390965'],
                   'page old 7': [title: 'page old 7', id: '4391051', parentId: '4390965'],
                   'page old 8': [title: 'page old 8', id: '4391056', parentId: '14094801'],
                   'page old 9': [title: 'page old 9', id: '4718619', parentId: '4390965'],
                   'page old 0': [title: 'page old 0', id: '4718623', parentId: '4390965']]
    }

    void 'test ancestor-id output'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def jsonSlurper = new JsonSlurper()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        script.setProperty("confluenceClient", Stub(constructorArgs: ["mock", "mock"],ConfluenceClient.class){
            fetchPagesByAncestorId(_, _, _) >> [data: jsonSlurper.parse(new File('./src/test/resources/asciidoc2confluence/ancestorId.json'))] >>
                // no more pages outside the limit
                [data: [results: []]] >>
                // first child loop
                [data: jsonSlurper.parse(new File('./src/test/resources/asciidoc2confluence/ancestorId_child.json'))] >>
                // all other child loops
                [data: [results: []]] })
        when: 'run retrieveAllPagesByAncestorId'
        def result = script.retrieveAllPagesByAncestorId(['123'], 100)
        then: 'the pages are given'
        result.size() == 7
        result == ['page 1': [title: 'page 1', id: '183954870', parentId: '123'],
                   'page 2': [title: 'page 2', id: '92996640', parentId: '123'],
                   'page 3': [title: 'page 3', id: '101845068', parentId: '123'],
                   'page 4': [title: 'page 4', id: '183954872', parentId: '123'],
                   'page 5': [title: 'page 5', id: '71210367', parentId: '183954870'],
                   'page 6': [title: 'page 6', id: '76418864', parentId: '183954870'],
                   'page 7': [title: 'page 7', id: '71208921', parentId: '183954870']]
    }

    void 'test default language'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        when: 'run rewriteCodeblocks'
        Document dom = Jsoup.parse('<pre><code>none</code></pre>', 'utf-8', Parser.xmlParser())
        script.rewriteCodeblocks dom.getAllElements(), '<cdata-placeholder>', '</cdata-placeholder>'

        then: 'the language is text'
        dom.select('ac|structured-macro > ac|parameter').text() == 'text'
    }

    @Unroll
    void 'test converted language'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)

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
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        script.setProperty("baseUrl", "./src/test/build/asciidoc2confluence")
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", []))
        script.setProperty("deferredUpload", [])
        def bodyFixture = new File('./src/test/resources/asciidoc2confluence/fixtures/body.html');
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
        def docFixture = new File('./src/test/resources/asciidoc2confluence/fixtures/confluenceDocWithoutToC.html').text;

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
        def imageFixture = new File('./src/test/resources/asciidoc2confluence/fixtures/imageBase64.txt');
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

    void 'test the correct editor is used'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def script = shell.parse(ASCIIDOC2CONFLUENCE_SCRIPT)
        when: 'explicitly not enforce the new editor'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of(
            "enforceNewEditor", false
        )))
        def expectedVersion1 = script.determineEditorVersion()

        then: 'the editor is the old one'
        expectedVersion1 == "v1"

        when: 'enforce the new editor'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", Map.of(
            "enforceNewEditor", true
        )))
        def expectedVersion2 = script.determineEditorVersion()

        then: 'the editor is the new one'
        expectedVersion2 == "v2"

        when: 'no config is set explicitly for the editor'
        script.setProperty("config", Map.of("imageDirs", ["Foo/"], "confluence", []))
        def expectedVersion3 = script.determineEditorVersion()

        then: 'the default editor is the old one'
        expectedVersion3 == "v1"
    }

}
