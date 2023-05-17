package docToolchain

import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.IgnoreRest

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome


class ExportOpenApiSpec extends Specification {

    public static final File outputPath = new File('./src/test/groovy/scripts/exportOpenApi/build')

    void 'test export of an OpenAPI specification file to an AsciiDoc document' () {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportOpenApi`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportOpenApi', '--info',
                               '-PdocDir=./src/test/groovy/scripts/exportOpenApi',
                               '-PmainConfigFile=config.groovy'
                               // '-PopenApi.specFile=./src/test/scripts/exportOpenApi/petstore-v3.0.yaml',
                               // '-PopenApi.infoUrl=https://my-api.company.com',
                               // '-PopenApi.infoEmail=info@company.com'
                               )
                .build()

        then: 'the task has been successfully executed'
        result.task(":exportOpenApi").outcome == TaskOutcome.SUCCESS

        and: 'the OpenAPI specification as an AsciiDoc file has been created'
        new File(outputPath, "OpenAPI/index.adoc").exists()
    }

    void 'test export fails when missing property openApi.specFile' () {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportOpenApi`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportOpenApi', '--info',
                               '-PdocDir=./src/test/groovy/scripts/exportOpenApi',
                               '-PmainConfigFile=config-openApi-missing.groovy'
                               )
                .build()

        then: 'the task throws an exception with useful information'
        def e = thrown(Exception)
        e.message.contains("Missing property 'openApi.specFile': please provide the location of the OpenAPI specification file")

        and: 'no files were created'
        outputPath.listFiles() == null
    }

    void 'test export fails when specFile does not exist' () {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `exportOpenApi`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('exportOpenApi', '--info',
                               '-PdocDir=./src/test/groovy/scripts/exportOpenApi',
                               '-PmainConfigFile=config.groovy',
                               '-PopenApi.specFile=src/does-not-exist.yaml',
                               )
                .buildAndFail()

        then: 'the task fails with useful information'
        result.task(":exportOpenApi").outcome == TaskOutcome.FAILED
        result.getOutput().find("In plugin 'org.openapi.generator' type 'org.openapitools.generator.gradle.plugin.tasks.GenerateTask' property 'inputSpec' specifies file '(.+)/src/does-not-exist.yaml' which doesn't exist.")

        and: 'no files were created'
        outputPath.listFiles() == null
    }

}
