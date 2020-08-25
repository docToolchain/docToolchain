package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@Stepwise
class ExportExcelSpec extends Specification {

    def gradleCommand

    @Shared
    def filenameList = []
    @Shared
    def filecontentList = []

    void setupSpec() {
        // 'get all files from the testdata folder'
        new File('./src/test/testData/excel/Sample.xlsx/.').eachFile { file ->
            filenameList << file.name
            filecontentList << file.text
                                    .trim()
                                    .replaceAll("\r","")
                                    // the output depends on the locale!
                                    .replaceAll("([0-9])[,]([0-9])",'$1.$2')
        }
        println filenameList
    }

    void 'test export of Excel file'() {
        when: 'the gradle task is invoked'
                def result = GradleRunner.create()
                        .withProjectDir(new File('.'))
                        .withArguments(['exportExcel', '--info', '-PmainConfigFile=./src/test/config.groovy'])
                        .build()
        then: 'the task has been successfully executed'
                result.task(":exportExcel").outcome == SUCCESS
    }
    @Unroll
    void 'test exported files: #filename'() {
        when: 'the test before exported the excel file'
        then: 'the export file have been created'
                new File('./src/test/docs/excel/Sample.xlsx/'+filename).exists() == true
                and: 'its content ends with our sample file'
                new File('./src/test/docs/excel/Sample.xlsx/'+filename)
                            .text.trim()
                                    .replaceAll("\r","")
                                    // the output depends on the locale!
                                    .replaceAll("([0-9])[,]([0-9])",'$1.$2')
                                    .endsWith(filecontent)
        where: 'iterate the expected files'
                filename << filenameList
                filecontent << filecontentList
    }

}
