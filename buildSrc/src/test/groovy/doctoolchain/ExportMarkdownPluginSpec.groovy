package doctoolchain

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExportMarkdownPluginSpec extends Specification {

    @Rule
    TemporaryFolder testProjectDir = new TemporaryFolder()

    @Rule
    TemporaryFolder inputFolder = new TemporaryFolder()

    @Rule
    TemporaryFolder outputFolder = new TemporaryFolder()

    File buildFile

    def "Simple Test to verify the conversion of multiple files is generally working"() {

        given: 'three simple markdown files in the inputDirectoy'
        inputFolder.newFile('1.md').text = "#Title1"
        inputFolder.newFile('2.md').text = "#Title2"
        inputFolder.newFile('3.md').text = "#Title3"

        and: 'build.gradle with a exportMarkdown task which has input and output configured.'

        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """\
            plugins {
                id 'doctoolchain.exportMarkdown' //Empty PLugin needed for automatic classpath injection
            }
            
            task("exportMarkdown", type: doctoolchain.ExportMarkdownTask) {
                from '${inputFolder.getRoot()}'
                into '${outputFolder.getRoot()}'
            }
           
        """.stripIndent()

        when: 'executing the \'exportMarkdown\' task'
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('exportMarkdown', '--stacktrace')
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .build()

        then: 'the task execution was succesfull'
        result.task(':exportMarkdown').outcome == SUCCESS

        and: 'and all input files are converted to adoc files in the output directory'
        outputFolder.getRoot().listFiles().find { it.name == "1.adoc" }?.text == "= Title1"
        outputFolder.getRoot().listFiles().find { it.name == "2.adoc" }?.text == "= Title2"
        outputFolder.getRoot().listFiles().find { it.name == "3.adoc" }?.text == "= Title3"
    }
}
