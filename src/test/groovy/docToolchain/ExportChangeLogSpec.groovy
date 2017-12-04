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
                    .withArguments(['exportChangeLog','--info'])
                    .build()
        then: 'the task has been successfully executed'
            result.task(":exportChangeLog").outcome == SUCCESS
        and: 'the log file has been created'
            new File('./build/docs/changelog.adoc').exists() == true
        and: 'its content ends with our sample file'
            new File('./build/docs/changelog.adoc')
                    .text.trim().replaceAll("\r","")
                    .endsWith("""
| 2016-08-21 
| Ralf D. Mueller 
| added arc42 template as content 

| 2016-07-11 
| Ralf D. Mueller 
| fixed formatting 

| 2016-07-10 
| Ralf D. Mueller 
| added plantUML to gradle build 

| 2016-07-05 
| Ralf D. Mueller 
| simple asciidoc build with gradle 
""".trim().replaceAll("\r",""))
    }

}
