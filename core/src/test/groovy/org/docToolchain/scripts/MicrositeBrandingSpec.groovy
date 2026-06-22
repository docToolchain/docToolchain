package org.docToolchain.scripts

import groovy.text.SimpleTemplateEngine
import spock.lang.Shared
import spock.lang.Specification

/**
 * Acceptance tests for config-driven microsite branding (issue #1663).
 *
 * The branding keys (microsite.color* / microsite.font* / microsite.logo) reach
 * the templates as config.site_*. These tests render the real template fragments
 * with the same SimpleTemplateEngine that MicrositeBaker uses, so an unset key
 * keeps the theme default and a set key drives the emitted CSS / logo.
 */
class MicrositeBrandingSpec extends Specification {

    @Shared String themeSnippet
    @Shared String logoLine

    private static File locate(String relative) {
        ["../${relative}", relative].collect { new File(it) }.find { it.exists() }
    }

    def setupSpec() {
        def header = locate('src/site/templates/header.gsp')
        assert header != null: 'could not locate src/site/templates/header.gsp'
        def text = header.text
        def startMarker = '<!-- dtc:theme-config:start -->'
        def endMarker = '<!-- dtc:theme-config:end -->'
        def start = text.indexOf(startMarker)
        def end = text.indexOf(endMarker)
        assert start >= 0 && end > start: 'theme-config markers missing in header.gsp'
        themeSnippet = text.substring(start + startMarker.length(), end)

        def menu = locate('src/site/templates/menu.gsp')
        assert menu != null: 'could not locate src/site/templates/menu.gsp'
        logoLine = menu.readLines().find { it.contains('navbar-logo') }
        assert logoLine != null: 'navbar-logo line missing in menu.gsp'
    }

    private String render(String template, Map binding) {
        new SimpleTemplateEngine().createTemplate(template).make(binding).toString()
    }

    def "unset color and font keys emit no override (theme default kept)"() {
        when: 'no branding is configured'
            def html = render(themeSnippet, [config: [:]])

        then: 'no :root override block and no font stylesheet are injected'
            !html.contains('dtc-theme-config')
            !html.contains('--g-600')
            !html.contains('<link')
    }

    def "configured colors and fonts drive exactly the matching CSS variables"() {
        when: 'brand colors and a body font are configured'
            def html = render(themeSnippet, [config: [
                    site_colorPrimary: '#ff6600',
                    site_colorLink   : '#0066ff',
                    site_fontBody    : "'Roboto', sans-serif",
            ]])

        then: 'the override block sets exactly those custom properties'
            html.contains('<style id="dtc-theme-config">')
            html.contains('--g-600: #ff6600;')
            html.contains('--teal: #ff6600;')
            html.contains('--g-700: #0066ff;')
            html.contains("--dtc-font: 'Roboto', sans-serif;")

        and: 'properties for unset keys are not emitted'
            !html.contains('--dtc-head:')
            !html.contains('--dtc-bg:')
    }

    def "fontCssUrl injects a stylesheet link for custom web fonts"() {
        when: 'a web-font stylesheet is configured'
            def html = render(themeSnippet, [config: [site_fontCssUrl: 'https://fonts.example/css']])

        then: 'a matching <link> is injected'
            html.contains('<link href="https://fonts.example/css" rel="stylesheet">')
    }

    def "logo defaults to the bundled asset and uses microsite.logo when set"() {
        expect: 'an unset logo falls back to the bundled asset'
            render(logoLine, [content: [rootpath: ''], config: [:]])
                    .contains('images/doctoolchain-logo-blue.png')

        and: 'a configured logo is used instead'
            render(logoLine, [content: [rootpath: ''], config: [site_logo: 'images/brand.png']])
                    .contains('images/brand.png')
    }
}
