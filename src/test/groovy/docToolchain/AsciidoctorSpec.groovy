package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class AsciidoctorSpec extends Specification {

    public static final File outputPath = new File('./src/test/testAsciidoctor/build/test/docs/asciidoctor')

    void 'test correct handling of images'() {
        given: 'a clean the environment'
        outputPath.deleteDir()
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
                '-PfailOnMissingImages=true',
                '-PdocDir=./src/test/testAsciidoctor',
                '-PmainConfigFile=testAsciidoctor.groovy',
            ])
            .buildAndFail()
        then: 'the task has been failed'
            result.task(":asciidoctor").outcome == FAILED
        and: 'the output does contain the warning "image to embed not found or not readable"'
            println result.output
            result.output.contains('- image to embed not found or not readable:')
        and: 'threw an exception to fail the build'
            result.output.contains('at org.asciidoctor.gradle.remote.ExecutorBase.failOnWarnings')
        and: 'an output file has been created'
            new File('./src/test/testAsciidoctor/build/test/docs/asciidoctor/broken_images.html').exists()
    }

    void 'test correct handling of images, ignoring missing images'() {
        given: 'a clean the environment'
        outputPath.deleteDir()
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
            .build()
        then: 'the task has been failed'
        result.task(":asciidoctor").outcome == SUCCESS
        and: 'the output does not contain the warning "image to embed not found or not readable"'
        println result.output
            !result.output.contains('- image to embed not found or not readable:')
        and: 'an output file has been created'
        new File('./src/test/testAsciidoctor/build/test/docs/asciidoctor/broken_images.html').exists()
    }

}
