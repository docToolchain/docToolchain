package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GenerateDeckSpec extends Specification {

    def gradleCommand

    void 'test correct generation of slide deck'() {
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generateDeck','--info', '-PinputPath=src/test/docs','-PmainConfigFile=src/test/config.groovy'])
                    .build()
        then: 'the task has been successfully executed'
            System.out.println "="*80
            System.out.println result.output
            result.task(":generateDeck").outcome == SUCCESS
        and:  'an output file has been created'
            new File('./build/test/docs/decks/simplePresentation.html').exists()
        and:  'the output contains proof that it is a reveal deck'
            new File('./build/test/docs/decks/simplePresentation.html').text.contains('https://github.com/hakimel/reveal.js')
    }

    void 'test skipped generation of slide deck'() {
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generateDeck','--info', '-PinputPath=src/test/docs','-PmainConfigFile=src/test/config_without_revealjs.groovy'])
                    .build()
        then: 'we get an exception'
            def e = thrown java.lang.Exception
        and: 'it contains some info about the problem'
            e.message.contains('Please specify at least one inputFile in your docToolchainConfig.groovy')
    }

}
