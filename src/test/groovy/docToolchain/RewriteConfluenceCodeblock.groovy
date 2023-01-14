package docToolchain

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.nodes.Document
import spock.lang.Specification
import spock.lang.Unroll

class RewriteConfluenceCodeblock extends Specification {
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
}
