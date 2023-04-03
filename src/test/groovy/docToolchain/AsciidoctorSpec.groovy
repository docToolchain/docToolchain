package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED

class AsciidoctorSpec extends Specification {

    def gradleCommand

    void 'test correct handling of images'() {
        when: 'input file contains images'
        def file = new File('src/test/testAsciidoctor/docs/broken_images.adoc')
        println file.canonicalPath
        def fileContent = file.text
        then: ''
        fileContent.contains('image:')
        when: 'the gradle task is invoked'
        def result = GradleRunner.create()
            .withProjectDir(new File('.'))
            .withArguments([
                'asciidoctor',
                '--info',
                '-PdocDir=./src/test/testAsciidoctor',
                '-PmainConfigFile=testAsciidoctor.groovy',
            ])
            .buildAndFail()
        then: 'the task has been successfully executed'
        result.task(":asciidoctor").outcome == FAILED
        and: 'the output does contain the warning "image to embed not found or not readable"'
        println result.output
        result.output.contains('image to embed not found or not readable')
        and: 'the output does not contain the warning "invalid style for listing block: plantuml'
        !result.output.contains('invalid style for listing block: plantuml')
        and: 'an output file has been created'
        new File('./build/test/docs/html5/test.html').exists()
    }

}
