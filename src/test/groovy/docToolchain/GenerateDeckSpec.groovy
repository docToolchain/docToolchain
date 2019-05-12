package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.SKIPPED

class GenerateDeckSpec extends Specification {

    def gradleCommand

    void 'test correct generation of slide deck'() {
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generateDeck','--info', '-PinputPath=src/test/docs','-PmainConfigFile=src/test/config.groovy'])
                    .build()
        then: 'the task has been successfully executed'
            result.task(":generateDeck").outcome == SUCCESS
        and:  'an output file has been created'
            new File('./build/test/docs/decks/html5/test.html').exists()
    }

    void 'test skipped generation of slide deck'() {
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generateDeck','--info', '-PinputPath=src/test/docs','-PmainConfigFile=src/test/config_without_revealjs.groovy'])
                    .build()
        then: 'the task has been skipped'
            result.task(":generateDeck").outcome == SKIPPED
    }

}
