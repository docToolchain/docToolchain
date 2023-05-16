package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.IgnoreRest

import static org.gradle.testkit.runner.TaskOutcome.*

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
        result.task(":exportOpenApi").outcome == SUCCESS

        and: 'the OpenAPI specification as an AsciiDoc file has been created'
        new File(outputPath, "OpenAPI/index.adoc").exists()
    }

}
