package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExportMarkdownSpec extends Specification {


    public static final File exportedMarkdownFile = new File('./build/docs/src/docs/exportMarkdownDocs.adoc')

    void 'test conversion of sample markdown file'() {

        given: 'a clean the environment'
        exportedMarkdownFile.delete()

        when: 'executing the gradle task `exportMarkdown`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportMarkdown')
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportMarkdown").outcome == SUCCESS

        and: 'the AsciiDoc file has been created'
        exportedMarkdownFile.exists()

        and: 'its content is in AsciiDoc syntax'
        exportedMarkdownFile.text.startsWith('= exportMarkdown')
    }
}
