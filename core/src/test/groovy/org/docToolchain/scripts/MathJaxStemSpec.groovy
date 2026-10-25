package org.docToolchain.scripts

import groovy.text.SimpleTemplateEngine
import spock.lang.Shared
import spock.lang.Specification

/**
 * Acceptance tests for STEM (math) rendering in the microsite (issue #1675).
 *
 * The theme footer injects the bundled, local MathJax only on pages that carry
 * math, and never references a CDN. (That AsciiDoctor emits the \( … \) / \$ … \$
 * delimiters once the stem attribute is set is exercised end-to-end by the CI
 * "Render smoke test"; AsciidoctorJ is not on the core test classpath.)
 */
class MathJaxStemSpec extends Specification {

    @Shared String mathjaxSnippet

    private static File locate(String relative) {
        ["../${relative}", relative].collect { new File(it) }.find { it.exists() }
    }

    def setupSpec() {
        def footer = locate('src/site/templates/footer.gsp')
        assert footer != null: 'could not locate src/site/templates/footer.gsp'
        def text = footer.text
        def start = text.indexOf('<!-- dtc:mathjax:start -->')
        def end = text.indexOf('<!-- dtc:mathjax:end -->')
        assert start >= 0 && end > start: 'mathjax markers missing in footer.gsp'
        mathjaxSnippet = text.substring(start, end)
    }

    private String renderFooter(Map content) {
        new SimpleTemplateEngine().createTemplate(mathjaxSnippet).make([content: content]).toString()
    }

    def "footer injects the local bundled MathJax for a page with latexmath"() {
        when: 'the rendered body carries a latexmath delimiter'
            def html = renderFooter([rootpath: '', body: 'see <span>\\( x^2 \\)</span>'])

        then: 'the bundled SVG MathJax is loaded from the theme, not a CDN'
            html.contains('src="js/mathjax/tex-mml-svg.js"')
            html.contains("load: ['input/asciimath']")

        and: 'no remote URL is referenced (fully self-contained / offline)'
            !html.contains('http')
            !html.contains('//cdn')
    }

    def "footer injects MathJax for an asciimath page and a stem block"() {
        expect: 'an asciimath \\$ delimiter triggers the loader'
            renderFooter([rootpath: '', body: 'x \\$ y \\$ z']).contains('tex-mml-svg.js')

        and: 'a stem block class triggers the loader'
            renderFooter([rootpath: '', body: '<div class="stemblock">…</div>']).contains('tex-mml-svg.js')
    }

    def "footer resolves the script against the page rootpath"() {
        expect: 'a nested page gets the correct relative path to the bundled script'
            renderFooter([rootpath: '../../', body: '\\( a \\)'])
                    .contains('src="../../js/mathjax/tex-mml-svg.js"')
    }

    def "footer adds no MathJax to a page without math"() {
        when: 'the body carries no math'
            def html = renderFooter([rootpath: '', body: '<p>just prose, no formulas</p>'])

        then:
            !html.contains('tex-mml-svg.js')
            !html.contains('MathJax')
    }
}
