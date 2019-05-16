package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GenerateHTMLSpec extends Specification {

    def gradleCommand

    void 'test correct handling of plantUML'() {
        when: 'input file contains a plantuml diagram'
            def file = new File('./src/test/docs/test.adoc')
            println file.canonicalPath
            def fileContent = file.text
        then: ''
            fileContent.contains('[plantuml,')
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generateHTML','--info', '-PmainConfigFile=./src/test/config.groovy'])
                    .build()
        then: 'the task has been successfully executed'
            result.task(":generateHTML").outcome == SUCCESS
        and: 'the output does not contain the warning "image to embed not found or not readable"'
            println result.output
            result.output.contains('image to embed not found or not readable') == false
        and: 'the output does not contain the warning "invalid style for listing block: plantuml'
            result.output.contains('invalid style for listing block: plantuml') == false
        and: 'an output file has been created'
            new File('./build/test/docs/html5/test.html').exists()
    }

}
