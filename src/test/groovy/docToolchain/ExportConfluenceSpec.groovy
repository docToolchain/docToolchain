package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.IgnoreRest

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExportConfluenceSpec extends Specification {

    public static final File exportedConfluenceDir = new File('./src/test/build/exportConfluenceSpec')


    void 'test conversion of Confluence space to AsciiDoc'() {

        given: 'a clean the environment'
        exportedConfluenceDir.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportConfluence`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportConfluence', '--info', '-PmainConfigFile=./src/test/config.groovy')
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportConfluence").outcome == SUCCESS

        and: 'the AsciiDoc root file has been created'
        new File(exportedConfluenceDir,"AsciiDoc_Test_Home/adoc_test.adoc").exists()

        and: 'its content is in AsciiDoc syntax'
        new File(exportedConfluenceDir,"AsciiDoc_Test_Home/adoc_test.adoc").text.contains('== adoc-test')
    }
}
