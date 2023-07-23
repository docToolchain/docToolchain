package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class CollectIncludesSpec extends Specification {

    public static final File outputPath = new File('./build/test/docs/collectIncludes')

    void 'test it traverses all folders except the ignored one and collects all matching files'() {

        given: 'a clean the environment'
        outputPath.deleteDir()

        when: 'executing the gradle task `collectIncludes`'
        def result = GradleRunner.create()
                .withProjectDir(new File('.'))
                .withArguments('collectIncludes', '--info',
                               '-PmainConfigFile=./src/test/testData/collectIncludes/config.groovy',
                               '-PscanDir=./src/test/testData/collectIncludes/docs')
                .build()

        then: 'the task has been successfully executed'
        result.task(":collectIncludes").outcome == SUCCESS

        def expectedOutputFile = new File(outputPath, "_includes/ADR_includes.adoc")

        and: 'the ADR_includes.adoc file has been created'
            expectedOutputFile.exists()
        and: 'it has a reference to include::../../../../.././src/test/testData/collectIncludes/docs/ADR_001_BoringNormalFile.adoc[]'
            expectedOutputFile.text.contains('include::../../../../.././src/test/testData/collectIncludes/docs/ADR_001_BoringNormalFile.adoc[]')
        and: 'does not include the file from excluded folder'
            !expectedOutputFile.text.contains('ADR_007_Headquarter.adoc')
    }

    void 'test it traverses just the specific folder and collects all matching files'() {

        given: 'a clean the environment'
        outputPath.deleteDir()
        println new File(".").canonicalPath

        when: 'executing the gradle task `collectIncludes`'
        def result = GradleRunner.create()
            .withProjectDir(new File('.'))
            .withArguments('collectIncludes', '--info',
                '-PmainConfigFile=./src/test/testData/collectIncludes/config.groovy',
                '-PscanDir=./src/test/testData/collectIncludes/docs/someOtherDir')
            .build()

        then: 'the task has been successfully executed'
        result.task(":collectIncludes").outcome == SUCCESS

        def expectedOutputFile = new File(outputPath, "_includes/ADR_includes.adoc")

        and: 'the ADR_includes.adoc file has been created'
            expectedOutputFile.exists()
        and: 'it has a reference to include::../../../../.././src/test/testData/collectIncludes/docs/someOtherDir/ADR_003_NestedFile.adoc[]'
            expectedOutputFile.text.contains('src/test/testData/collectIncludes/docs/someOtherDir/ADR_003_NestedFile.adoc[]')
        and: 'does not include the file from project root folder'
            !expectedOutputFile.text.contains('ADR_001_BoringNormalFile.adoc[]')
    }
}
