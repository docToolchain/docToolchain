package docToolchain
import spock.lang.*
import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*

class ExportChangeLogSpec extends Specification {

    def gradleCommand

    void 'test creation of log file'() {
        setup: 'clean the environment'
        when: 'remove old changelog file'
            new File('./build/docs/changelog.adoc').delete()
        then: 'log file does not exist'
            new File('./build/docs/changelog.adoc').exists() == false
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['exportChangeLog','--info','-PchangelogConfigFile=src/test/changelogConfig.groovy'])
                    .build()
        then: 'the task has been successfully executed'
            result.task(":exportChangeLog").outcome == SUCCESS
        and: 'the log file has been created'
            new File('./build/docs/changelog.adoc').exists() == true
        and: 'its content ends with our sample file'
            new File('./build/docs/changelog.adoc')
                    .text.trim().replaceAll("\r","")
                    .endsWith("""
| 2017-10-25 
| Ralf D. Mueller 
| refined tests 

| 2017-09-25 
| Jakub J Jablonski 
| Fix failing test for PDF generation with PlantUML 

| 2017-09-24 
| Ralf D. Mueller 
| added tests for plantUml 
""".trim().replaceAll("\r",""))
    }

}
