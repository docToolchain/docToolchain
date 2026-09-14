package docToolchain

import spock.lang.Specification

/**
 * microsite.host is what the sitemap tells crawlers. Getting it wrong is not
 * visibly wrong: the build succeeds, the site looks fine, and only the sitemap
 * names addresses that lead nowhere.
 *
 * It happened twice. First the value was the shipped example, 'https://localhost',
 * and all 109 entries were dead. Then it was corrected to the v4.0.x directory —
 * which exists and is served, but is not what this branch publishes, so 30 of 112
 * entries were dead: the pages added since that copy was frozen.
 *
 * Nothing in the build connected the two facts. This test does: the version
 * directory in host has to be the one .ci.sh copies this branch's output into.
 */
class SiteHostSpec extends Specification {

    static final File CONFIG = new File('docToolchainConfig.groovy')
    static final File PUBLISH = new File('.ci.sh')

    /** The value of microsite.host, as configured. */
    String configuredHost() {
        CONFIG.text.find(/host\s*=\s*'([^']+)'/) { match, host -> host }
    }

    /**
     * The directories .ci.sh copies this branch's build output into.
     * Each publishing branch has its own, e.g. ng -> v2.0.x.
     */
    List<String> publishedDirectories() {
        PUBLISH.text.findAll(/cp -Rf "\$\{BUILD_DIR\}"[^\n]*? (v[\d.]+x)\/\./) { match, dir -> dir }
    }

    void 'host is a real address, not the shipped example'() {

        expect:
        configuredHost()

        and:
        !(configuredHost() ==~ '(?i)https?://localhost(:\\d+)?(/.*)?')
        !configuredHost().contains('jbake.org')
    }

    void 'host names a version directory this build actually publishes'() {

        given: 'the directories .ci.sh writes into'
        def published = publishedDirectories()
        published

        when: 'the version segment is read out of the configured host'
        def version = configuredHost().find(/v[\d.]+x/)

        then: 'it is one of them — otherwise the sitemap points at another copy'
        version
        published.contains(version)
    }

    void 'host carries no trailing slash'() {

        expect: 'the sitemap template appends one itself'
        !configuredHost().endsWith('/')
    }
}
