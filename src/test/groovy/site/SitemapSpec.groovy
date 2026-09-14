package docToolchain

import groovy.text.SimpleTemplateEngine
import spock.lang.Specification
import spock.lang.Unroll

import java.text.SimpleDateFormat

/**
 * A sitemap promises crawlers that these addresses exist. Two ways it used to
 * break that promise without anyone noticing:
 *
 *  - site.host still held a placeholder, so every entry pointed at jbake.org
 *    or localhost.
 *  - a page rendered to another extension (the lunr.js search index) was still
 *    listed as .html and answered 404.
 *
 * These tests render the real template, so they fail when either comes back.
 */
class SitemapSpec extends Specification {

    static final File TEMPLATE = new File('src/site/templates/sitemap.gsp')

    /** How a docToolchain site is configured: the search index renders to .js, pages to .html. */
    static final Map EXTENSIONS = [
        template_lunrjsindex_extension: '.js',
        template_page_extension       : '.html',
    ]

    def page(String uri, String type = 'page') {
        [uri: uri, type: type, date: new SimpleDateFormat('yyyy-MM-dd').parse('2026-06-26')]
    }

    /** The addresses the rendered sitemap actually claims. */
    List<String> locations(String host, List content) {
        def out = new StringWriter()
        new SimpleTemplateEngine()
            .createTemplate(TEMPLATE.text)
            .make([config: EXTENSIONS + [site_host: host], published_content: content])
            .writeTo(out)
        (out.toString() =~ /<loc>([^<]*)<\/loc>/).collect { it[1] }
    }

    void 'a page that renders to another extension is left out'() {

        given: 'a normal page and the lunr.js search index'
        def content = [page('arc42/open_questions.html'), page('lunrjsindex.html', 'lunrjsindex')]

        when: 'the sitemap is rendered'
        def locations = locations('https://doctoolchain.org/Bausteinsicht', content)

        then: 'only the page is listed — the index is not a page and answers 404 under .html'
        locations == ['https://doctoolchain.org/Bausteinsicht/arc42/open_questions.html']
    }

    @Unroll
    void 'an unset host writes an empty sitemap rather than a wrong one: #description'() {

        when: 'the sitemap is rendered with a host nobody set'
        def locations = locations(host, [page('index.html')])

        then: 'no entry is written, because every entry would lead nowhere'
        locations == []

        where:
        host                     | description
        'http://jbake.org'       | 'the jBake default'
        'http://www.jbake.org'   | 'the jBake default, with www'
        'https://jbake.org'      | 'the jBake default, over https'
        'https://localhost'      | 'the example from the shipped config'
        'https://localhost:8080' | 'a local preview with a port'
        ''                       | 'empty'
        null                     | 'missing'
    }

    void 'a trailing slash on the host does not become a double slash'() {

        expect:
        locations('https://doctoolchain.org/Bausteinsicht/', [page('index.html')]) ==
            ['https://doctoolchain.org/Bausteinsicht/index.html']
    }

    void 'every page is listed once, under the host, as html'() {

        given: 'forty pages and one search index'
        def content = (1..40).collect { page("chapter/page-${it}.html") } +
            [page('lunrjsindex.html', 'lunrjsindex')]

        when:
        def locations = locations('https://example.org/docs', content)

        then: 'the count follows the pages, not the published content'
        locations.size() == 40

        and: 'each entry is a complete, well-formed address'
        locations.every { it.startsWith('https://example.org/docs/') }
        locations.every { it.endsWith('.html') }
        locations.every { !it.contains('docs//') }

        and: 'no address is claimed twice'
        locations.unique(false).size() == locations.size()
    }
}
