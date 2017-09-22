package docToolchain
import spock.lang.*

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
            "$gradleCommand clean".execute()
        expect: 'log file does not exist'
            new File(targetDir, 'changelog.adoc').exists() == false
        when: 'the gradle task is invoked'
            def output = "$gradleCommand exportChangeLog".execute().text
        then: 'the the log file has been created'
            new File(targetDir, 'changelog.adoc').exists() == true
        and: 'its content ends with our sample file'
            new File(targetDir, 'changelog.adoc')
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
