package docToolchain
import spock.lang.*
import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*

class ExportChangeLogSpec extends Specification {

    def gradleCommand

    void setup() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            gradleCommand = "./gradlew.bat"
        } else {
            gradleCommand = "./gradlew"
        }
    }
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
            result.output.contains('changelog exported')
            result.task(":exportChangeLog").outcome == SUCCESS
        and: 'the the log file has been created'
            new File('./build/docs/changelog.adoc').exists() == true
        and: 'its content ends with our sample file'
            new File('./build/docs/changelog.adoc')
                    .text.trim()
                    .endsWith("""
| 2017-09-08 
| Isidoro 
| Added spanish translation for Arc42 Template 

| 2017-04-09 
| Ralf D. Mueller 
| fix #24 template updated to V7.0 

| 2017-04-08 
| Ralf D. Mueller 
| fixed typo 

| 2016-10-03 
| Ralf D. Mueller 
| added jira based open issues list 

| 2016-08-21 
| Ralf D. Mueller 
| added arc42 template as content 
""".trim())
    }

}
