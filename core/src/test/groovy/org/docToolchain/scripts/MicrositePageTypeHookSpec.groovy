package org.docToolchain.scripts

import groovy.text.SimpleTemplateEngine
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

/**
 * Acceptance tests for the per-page-type styling hook of the v4 microsite.
 *
 * A site theme can style the landing page and the content pages differently
 * with one extra stylesheet: every page template puts the page type on <html>
 * (data-dtc-type="masterindex" | "page" | ...), and header.gsp loads
 * css/site-extra.css last, but only when the theme ships that file. These tests
 * render the real template fragments with the same SimpleTemplateEngine that
 * MicrositeBaker uses.
 */
class MicrositePageTypeHookSpec extends Specification {

    static final String START = '<!-- dtc:site-extra:start -->'
    static final String END = '<!-- dtc:site-extra:end -->'
    static final List<String> PAGE_TEMPLATES = [
            'archive.gsp', 'index.gsp', 'news.gsp', 'page.gsp', 'page_custom_menu.gsp',
            'page_toc.gsp', 'page_tocl.gsp', 'post.gsp', 'search.gsp', 'tags.gsp']

    @Shared String headerText
    @Shared String siteExtraSnippet
    @Shared List<File> pageTemplates

    @TempDir
    Path siteDir

    private static File locate(String relative) {
        ["../${relative}", relative].collect { new File(it) }.find { it.exists() }
    }

    def setupSpec() {
        def header = locate('src/site/templates/header.gsp')
        assert header != null: 'could not locate src/site/templates/header.gsp'
        headerText = header.text
        def start = headerText.indexOf(START)
        def end = headerText.indexOf(END)
        assert start >= 0 && end > start: 'site-extra markers missing in header.gsp'
        siteExtraSnippet = headerText.substring(start + START.length(), end)

        // every template MicrositeBaker renders as an HTML page
        pageTemplates = PAGE_TEMPLATES.collect { new File(header.parentFile, it) }
        pageTemplates.each { assert it.exists(): "missing template ${it.name}" }
    }

    private String render(String template, Map binding) {
        new SimpleTemplateEngine().createTemplate(template).make(binding).toString()
    }

    def "every page template puts the page type on the <html> element"() {
        expect:
            pageTemplates.each { File template ->
                def htmlLine = template.readLines().find { it.contains('<html') }
                assert htmlLine != null: "${template.name} has no <html> element"
                def html = render(htmlLine, [content: [type: 'masterindex']])
                assert html.contains('data-dtc-type="masterindex"'): "${template.name}: ${htmlLine}"
            }
    }

    def "a theme without site-extra.css gets no extra stylesheet link"() {
        when: 'the prepared site has no assets/css/site-extra.css'
            def html = render(siteExtraSnippet, [
                    content: [rootpath: '../'],
                    config : [sourceFolder: siteDir.toFile()]])

        then:
            !html.contains('<link')
    }

    def "a theme with site-extra.css gets it linked relative to the page"() {
        given: 'the theme ships assets/css/site-extra.css'
            def css = siteDir.resolve('assets/css/site-extra.css').toFile()
            css.parentFile.mkdirs()
            css.text = '[data-dtc-type=masterindex] body { background: yellow; }'

        when:
            def html = render(siteExtraSnippet, [
                    content: [rootpath: '../'],
                    config : [sourceFolder: siteDir.toFile()]])

        then:
            html.contains('<link href="../css/site-extra.css" rel="stylesheet">')
    }

    def "site-extra.css loads after the theme CSS and the brand overrides"() {
        expect: 'it is the last stylesheet, so it wins the cascade'
            headerText.indexOf(START) > headerText.indexOf('css/doctoolchain-v4.css')
            headerText.indexOf(START) > headerText.indexOf('<!-- dtc:theme-config:end -->')
    }
}
