package docToolchain

import org.gradle.testkit.runner.GradleRunner
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class ConvertDom2Pages extends Specification {
    @Unroll
    void 'test layers with preamble'() {
        setup: 'load asciidoc2confluence'
        GroovyShell shell = new GroovyShell()
        def script = shell.parse(new File("./scripts/asciidoc2confluence.groovy"))

        when: 'convert to HTML'
        def result = GradleRunner.create()
            .withProjectDir(new File('.'))
            .withArguments(['generateHTML','--info', '-PmainConfigFile=./src/test/config_pages.groovy'])
            .build()
        def htmlFile = new File('./build/test/docs/html5/withPreamble.html')

        then: 'HTML successfully created'
        result.task(":generateHTML").outcome == SUCCESS || result.task(":generateHTML").outcome == UP_TO_DATE
        htmlFile.exists()

        when: 'run getPages'
        Document dom = Jsoup.parse(htmlFile.getText('utf-8'), 'utf-8', Parser.xmlParser())
        def (pages, anchors, pageAnchors) = script.getPages(dom, "42", layers)

        then: 'the pages are given'
        pages.size() == 1
        pages[0].title == 'Level 1'
        assertion(pages[0])

        where:
        layers | assertion
        0 | { p ->
            content(p.body.toString(),
                'L1 L2a L3a L4a L5a L3b L2b',
                'Level 2a Level 2b',
                'Level 3a Level 3b',
                'Level 4a',
                'Level 5a'
            ) &&
                p.children.size() == 0
        }
        1 | { p ->
            content(p.body.toString(),
                'L1',
            ) &&
                p.children.size() == 2 &&
                p.children[0].title == 'Level 2a' &&
                content(p.children[0].body.toString(),
                    'L2a L3a L4a L5a L3b',
                    'Level 3a Level 3b',
                    'Level 4a',
                    'Level 5a'
                ) &&
                p.children[0].children.size() == 0 &&
                p.children[1].title == 'Level 2b' &&
                content(p.children[1].body.toString(),
                    'L2b',
                ) &&
                p.children[1].children.size() == 0
        }
        2 | { p ->
            content(p.body.toString(),
                'L1',
            ) &&
                p.children.size() == 2 &&
                p.children[0].title == 'Level 2a' &&
                content(p.children[0].body.toString(),
                    'L2a',
                ) &&
                p.children[0].children.size() == 2 &&
                p.children[0].children[0].title == 'Level 3a' &&
                content(p.children[0].children[0].body.toString(),
                    'L3a L4a L5a',
                    'Level 4a',
                    'Level 5a'
                ) &&
                p.children[0].children[0].children.size() == 0 &&
                p.children[0].children[1].title == 'Level 3b' &&
                content(p.children[0].children[1].body.toString(),
                    'L3b',
                ) &&
                p.children[0].children[1].children.size() == 0 &&
                p.children[1].title == 'Level 2b' &&
                content(p.children[1].body.toString(),
                    'L2b',
                ) &&
                p.children[1].children.size() == 0
        }
        3 | { p ->
            content(p.body.toString(),
                'L1',
            ) &&
                p.children.size() == 2 &&
                p.children[0].title == 'Level 2a' &&
                content(p.children[0].body.toString(),
                    'L2a'
                ) &&
                p.children[0].children.size() == 2 &&
                p.children[0].children[0].title == 'Level 3a' &&
                content(p.children[0].children[0].body.toString(),
                    'L3a'
                ) &&
                p.children[0].children[0].children.size() == 1 &&
                p.children[0].children[0].children[0].title == 'Level 4a' &&
                content(p.children[0].children[0].children[0].body.toString(),
                    'L4a L5a',
                    'Level 5a'
                ) &&
                p.children[0].children[0].children[0].children.size() == 0 &&
                p.children[0].children[1].title == 'Level 3b' &&
                content(p.children[0].children[1].body.toString(),
                    'L3b',
                ) &&
                p.children[0].children[1].children.size() == 0 &&
                p.children[1].title == 'Level 2b' &&
                content(p.children[1].body.toString(),
                    'L2b',
                ) &&
                p.children[1].children.size() == 0
        }
    }

    @Unroll
    void 'test layers without preamble'() {
        setup: 'load asciidoc2confluence'
        GroovyShell shell = new GroovyShell()
        def script = shell.parse(new File("./scripts/asciidoc2confluence.groovy"))

        when: 'convert to HTML'
        def result = GradleRunner.create()
            .withProjectDir(new File('.'))
            .withArguments(['generateHTML','--info', '-PmainConfigFile=./src/test/config_pages.groovy'])
            .build()
        def htmlFile = new File('./build/test/docs/html5/withoutPreamble.html')

        then: 'HTML successfully created'
        result.task(":generateHTML").outcome == SUCCESS || result.task(":generateHTML").outcome == UP_TO_DATE
        htmlFile.exists()

        when: 'run getPages'
        Document dom = Jsoup.parse(htmlFile.getText('utf-8'), 'utf-8', Parser.xmlParser())
        def (pages, anchors, pageAnchors) = script.getPages(dom, "42", layers)

        then: 'the pages are given'
        assertion(pages)

        where:
        layers | assertion
        0 | { p ->
            p.size() == 1 &&
                p[0].title == 'Level 1' &&
                content(p[0].body.toString(),
                    'L2a L3a L4a L5a L3b L2b',
                    'Level 2a Level 2b',
                    'Level 3a Level 3b',
                    'Level 4a',
                    'Level 5a'
                ) &&
                p[0].children.size() == 0
        }
        1 | { p ->
            p.size() == 2 &&
                p[0].title == 'Level 2a' &&
                content(p[0].body.toString(),
                    'L2a L3a L4a L5a L3b',
                    'Level 3a Level 3b',
                    'Level 4a',
                    'Level 5a'
                ) &&
                p[0].children.size() == 0 &&
                p[1].title == 'Level 2b' &&
                content(p[1].body.toString(),
                    'L2b',
                ) &&
                p[1].children.size() == 0
        }
        2 | { p ->
            p.size() == 2 &&
                p[0].title == 'Level 2a' &&
                content(p[0].body.toString(),
                    'L2a',
                ) &&
                p[0].children.size() == 2 &&
                p[0].children[0].title == 'Level 3a' &&
                content(p[0].children[0].body.toString(),
                    'L3a L4a L5a',
                    'Level 4a',
                    'Level 5a'
                ) &&
                p[0].children[0].children.size() == 0 &&
                p[0].children[1].title == 'Level 3b' &&
                content(p[0].children[1].body.toString(),
                    'L3b',
                ) &&
                p[0].children[1].children.size() == 0 &&
                p[1].title == 'Level 2b' &&
                content(p[1].body.toString(),
                    'L2b',
                ) &&
                p[1].children.size() == 0
        }
        3 | { p ->
            p.size() == 2 &&
                p[0].title == 'Level 2a' &&
                content(p[0].body.toString(),
                    'L2a'
                ) &&
                p[0].children.size() == 2 &&
                p[0].children[0].title == 'Level 3a' &&
                content(p[0].children[0].body.toString(),
                    'L3a'
                ) &&
                p[0].children[0].children.size() == 1 &&
                p[0].children[0].children[0].title == 'Level 4a' &&
                content(p[0].children[0].children[0].body.toString(),
                    'L4a L5a',
                    'Level 5a'
                ) &&
                p[0].children[0].children[0].children.size() == 0 &&
                p[0].children[1].title == 'Level 3b' &&
                content(p[0].children[1].body.toString(),
                    'L3b',
                ) &&
                p[0].children[1].children.size() == 0 &&
                p[1].title == 'Level 2b' &&
                content(p[1].body.toString(),
                    'L2b',
                ) &&
                p[1].children.size() == 0
        }
    }

    private boolean content(String html, String body, String... headings) {
        Document dom = Jsoup.parse(html, 'utf-8', Parser.xmlParser())
        def headingMatches = []
        if (headings.size() > 0) {
            headingMatches = (1..headings.size()).collect {i ->
                dom.select("h${i}").text() == headings[i - 1]
            }
        }
        return dom.select('p').text() == body &&
            headingMatches.inject(true) { m1, m2 -> m1 && m2 }
    }
}
