package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.IgnoreRest

import static org.gradle.testkit.runner.TaskOutcome.*

class ExportStructurizrSpec extends Specification {

    public static final File outputPath = new File('./src/test/docs/structurizr')

    void 'test export views from Structurizr DSL file to PlantUML diagrams'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info',
                               '-Pstructurizr.workspace.path=./src/test/testData/structurizr/default',
                               '-Pstructurizr.export.outputPath='+outputPath.toString())
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        and: 'the System Context diagram file has been created'
        new File(outputPath, "SystemContext-001.puml").exists()

        and: 'the legend for the System Context diagram file has been created'
        new File(outputPath, "SystemContext-001-key.puml").exists()

        and: 'the Container diagram file has been created'
        new File(outputPath, "Container-001.puml").exists()

        and: 'the legend for the Container diagram file has been created'
        new File(outputPath, "Container-001-key.puml").exists()
    }

    void 'test export views from Structurizr DSL with configuration file'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info', '-PmainConfigFile=./src/test/testData/structurizr/config.groovy')
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        and: 'all expected files were created'
        outputPath.list().size() == 5       // 4 PUML files + README.adoc
    }

    void 'test export views from Structurizr DSL with configured filename'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info',
                               '-Pstructurizr.workspace.path=./src/test/testData/structurizr/filename',
                               '-Pstructurizr.workspace.filename=minimal-example',
                               '-Pstructurizr.export.outputPath='+outputPath.toString())
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        and: 'all expected files were created'
        outputPath.list().size() == 5       // 4 PUML files + README.adoc
    }

    void 'test export views of Structurizr DSL file to C4-PlantUML diagrams'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info',
                               '-Pstructurizr.workspace.path=./src/test/testData/structurizr/default',
                               '-Pstructurizr.export.outputPath='+outputPath.toString(),
                               '-Pstructurizr.export.format=plantuml/c4plantuml')
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportStructurizr").outcome == SUCCESS

        // C4-Plantuml do not have legend files
        and: 'the System Context diagram is a C4-PlantUML file'
        def c4plantuml_pattern = ~/.*!include <C4\/C4>*/
        new File(outputPath, "SystemContext-001.puml").grep(c4plantuml_pattern)

        and: 'the Container diagram is a C4-PlantUML file'
        new File(outputPath, "Container-001.puml").grep(c4plantuml_pattern)
    }

    void 'test throwing exception on missing configuration parameter `structurizr.workspace.path`'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr` with no configuration'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info')
                .build()

        then: 'the task throws an exception with information'
        def e = thrown(Exception)
        e.message.contains("Missing configuration parameter 'structurizr.workspace.path': please provide the path where the Structurizr workspace file is located.")

        and: 'no diagrams were created/changed'
        outputPath.listFiles() == null
    }

    void 'test throwing exception when workspace file can not be found'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr` with no configuration'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info',
                               '-Pstructurizr.workspace.path=./src/test/testData/structurizr/filename',
                               '-Pstructurizr.export.outputPath='+outputPath.toString(),
                               '-Pstructurizr.format=plantuml/c4plantuml')
                .build()

        then: 'the task throws an exception with information'
        def e = thrown(Exception)
        e.message.contains("./src/test/testData/structurizr/filename/workspace.dsl does not exist")

        and: 'no diagrams were created/changed'
        outputPath.listFiles() == null
    }

    void 'test throwing exception on missing configuration parameter `structurizr.export.outputPath`'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportStructurizr` without `export.outputPath`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportStructurizr', '--info', '-Pstructurizr.workspace.path=./src/test/testData/structurizr/default')
                .build()

        then: 'the task throws an exception with information'
        def e = thrown(Exception)
        e.message.contains("Missing configuration parameter 'structurizr.export.outputPath': please provide the directory where the diagrams should be exported.")

        and: 'no diagrams were created/changed'
        outputPath.listFiles() == null
    }
}
