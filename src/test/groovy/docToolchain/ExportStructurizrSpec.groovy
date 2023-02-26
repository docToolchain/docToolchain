package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.IgnoreRest

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExportStructurizrSpec extends Specification {

    // TODO: how to access the configuration settings from the test?
    public static final File exportPath = new File('./src/test/docs/structurizr')

    void 'test conversion of Structurizr DSL file to PlantUML diagrams'() {

        given: 'a clean the environment'
        exportPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info', '-Pstructurizr.workspace=./src/test/testData/structurizr/workspace.dsl', '-Pstructurizr.exportPath='+exportPath.toString())
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        and: 'the System Context diagram file has been created'
        new File(exportPath, "SystemContext-1.puml").exists()

        and: 'the legend for the System Context diagram file has been created'
        new File(exportPath, "SystemContext-1-key.puml").exists()

        and: 'the Container diagram file has been created'
        new File(exportPath, "Container-1.puml").exists()

        and: 'the legend for the Container diagram file has been created'
        new File(exportPath, "Container-1-key.puml").exists()
    }
    
    void 'test conversion of Structurizr DSL file to C4-PlantUML diagrams'() {

        given: 'a clean the environment'
        exportPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info', '-Pstructurizr.workspace=./src/test/testData/structurizr/workspace.dsl', '-Pstructurizr.exportPath='+exportPath.toString(), '-Pstructurizr.format=plantuml/c4plantuml')
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        and: 'the System Context diagram file has been created'
        new File(exportPath, "SystemContext-1.puml").exists()

        and: 'the System Context diagram is a C4-PlantUML file'
        def c4plantuml_pattern = ~/.*!include https:\/\/raw\.githubusercontent\.com\/plantuml-stdlib\/C4-PlantUML\/.*/
        new File(exportPath, "SystemContext-1.puml").grep(c4plantuml_pattern)

        and: 'the Container diagram file has been created'
        new File(exportPath, "Container-1.puml").exists()

        and: 'the Container diagram is a C4-PlantUML file'
        new File(exportPath, "Container-1.puml").grep(c4plantuml_pattern)
    }

    void 'test export is skipped with no Structurizr DSL file '() {

        given: 'a clean the environment'
        exportPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info')
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        and: 'the directory is empty'
        exportPath.listFiles() == null
    }
}
