package docToolchain

import spock.lang.Specification

/**
 * The first time a theme is used in a project, generateSite asks the maintainer
 * for the values in configFragment.groovy and writes them into the config.
 *
 * Three things went wrong there, and each one was silent:
 *
 *  - the parser read a comment line as line[2..-1], which is out of bounds for a
 *    line that is just "//" — and the fragment has nine of those.
 *  - a placeholder written as '##footer-email' (no closing ##) was prompted for
 *    but never substituted, so the answer was thrown away.
 *  - host was never asked at all, which is why sitemaps kept the jBake default.
 *
 * This runs the parser over the shipped fragment, so all three fail loudly if
 * they return.
 */
class ConfigFragmentSpec extends Specification {

    static final File FRAGMENT = new File('src/site/configFragment.groovy')
    static final File INTERVIEW = new File('scripts/generateSite.gradle')

    /**
     * The interview from scripts/generateSite.gradle, with ant.input replaced by a
     * canned answer. Returns the config it would write and the questions it asked.
     */
    Map runInterview(Closure answer = { property -> "answer-for-${property}" }) {
        def conf = ''
        def comment = ''
        def example = ''
        def questions = []

        FRAGMENT.eachLine { line ->
            if (line.trim()) {
                if (line.startsWith('//')) {
                    conf += '    ' + line + '\n'
                    def tmp = line.substring(2).trim()
                    comment += tmp + '\n'
                    if (tmp.toLowerCase().startsWith('example')) {
                        example = tmp.replaceAll('[^ ]* ', '')
                    }
                } else if (line.contains('##')) {
                    def property = line.replaceAll('[ =].*', '')
                    questions << [property: property, example: example, comment: comment]
                    conf += '    ' + line.replaceAll('##.+##', answer(property)) + '\n'
                    (comment, example) = ['', '']
                } else {
                    conf += '    ' + line + '\n'
                }
            } else {
                conf += '\n'
            }
        }
        [config: conf, questions: questions]
    }

    void 'the interview survives a comment line that is only a double slash'() {

        given: 'the shipped fragment contains such lines'
        def bare = FRAGMENT.readLines().findAll { it.trim() == '//' }
        bare.size() > 0

        and: 'the expression generateSite uses to strip the comment marker'
        // Taken from the build script itself, so this test follows it when it changes.
        def stripper = INTERVIEW.text.find(/def tmp = line(?:\[2\.\.-1\]|\.substring\(2\))\.trim\(\)/)
        stripper

        when: 'it is applied to such a line'
        new GroovyShell(new Binding(line: '//')).evaluate(stripper + '\ntmp')

        then: 'it does not blow up — the whole interview dies with it if it does'
        noExceptionThrown()
    }

    void 'the maintainer is asked for the host'() {

        when:
        def host = runInterview().questions.find { it.property == 'host' }

        then: 'the question exists — without it the sitemap keeps the jBake default'
        host

        and: 'and it offers a real address as the suggested answer'
        host.example.startsWith('https://')
    }

    void 'every answer reaches the config'() {

        when: 'the interview runs and each question is answered'
        def result = runInterview()

        then: 'no placeholder survives — a leftover ## means an answer was discarded'
        !result.config.contains('##')

        and: 'the interview did ask something'
        result.questions.size() > 0
    }

    void 'the config it writes is valid groovy'() {

        when:
        def parsed = new ConfigSlurper().parse("microsite { ${runInterview().config} }")

        then:
        parsed.microsite.host == 'answer-for-host'
        parsed.microsite.title
    }
}
