package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.IgnoreRest

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExportStructurizrSpec extends Specification {

    // TODO: how to access the configuration settings from the test?
    public static final File exportedStructurizrDir = new File('./src/test/docs/structurizr')

    void 'test conversion of Structurizr DSL file to diagrams'() {

        given: 'a clean the environment'
        exportedStructurizrDir.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info', '-PmainConfigFile=./src/test/config.groovy')
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        and: 'the System Context diagram file has been created'
        new File(exportedStructurizrDir, "SystemContext-1.puml").exists()

        and: 'the legend for the System Context diagram file has been created'
        new File(exportedStructurizrDir, "SystemContext-1-key.puml").exists()

        and: 'the Container diagram file has been created'
        new File(exportedStructurizrDir, "Container-1.puml").exists()

        and: 'the legend for the Container diagram file has been created'
        new File(exportedStructurizrDir, "Container-1-key.puml").exists()
    }
}
