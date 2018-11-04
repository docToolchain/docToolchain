package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GeneratePdfSpec extends Specification {

    def gradleCommand

    void 'test correct handling of plantUML'() {
        when: 'input file contains a plantuml diagram'
            def file = new File('src/test/docs/test2.adoc')
            println file.canonicalPath
            def fileContent = file.text
        then: ''
            fileContent.contains('[plantuml,')
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generatePDF','--info','-PmainConfigFile=config_pdf.groovy', '-PdocDir=./src/test/', '-PinputPath=docs'])
                    .build()
        then: 'the task has been successfully executed'
        println result.output
            result.task(":generatePDF").outcome == SUCCESS
        and: 'the output does not contain the warning "image to embed not found or not readable"'
            println result.output
            result.output.contains('image to embed not found or not readable') == false
        and: 'it also does not contain any other error'
            result.output.toLowerCase().contains('error') == false
    }

}
