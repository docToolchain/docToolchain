package docToolchain

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GeneratePdfSpecWithDefaultTheme extends Specification {

    def gradleCommand

    void 'test correct handling of plantUML and default PDF theme'() {
        when: 'input file contains a plantuml diagram'
            def file = new File('src/test/testPdfDefaultTheme/testPdfDefaultThemeDocs/test2.adoc')
            println file.canonicalPath
            def fileContent = file.text
        then: ''
            fileContent.contains('[plantuml,')
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generatePDF','--info',
                        '-PdocDir=./src/test/testPdfDefaultTheme',
                        '-PmainConfigFile=testPdfDefaultThemeConfig.groovy'])
                    .build()
        then: 'the task has been successfully executed'
        println result.output
            result.task(":generatePDF").outcome == SUCCESS
        and: 'the output does not contain the warning "image to embed not found or not readable"'
            println result.output
            result.output.contains('image to embed not found or not readable') == false
        and: 'it also does not contain any other error'
            // the output will contain some dependencies with "error" in the path name
            // so let's first remove this and then check for other errors
            // Downloading https://plugins.gradle.org/m2/com/google/errorprone/error_prone_annotations/2.3.4/error_prone_annotations-2.3.4.jar to /tmp/gradle_download4302981685249827904bin
            result.output.toLowerCase().replaceAll("/error","").contains('error') == false
    }

}

class GeneratePdfSpecWithSpecificTheme extends Specification {

    def gradleCommand

    void 'test correct handling of plantUML and specific PDF theme'() {
        when: 'input file contains a plantuml diagram'
            def file = new File('src/test/testPdfSpecificTheme/testPdfSpecificThemeDocs/test2.adoc')
            println file.canonicalPath
            def fileContent = file.text
        then: ''
            fileContent.contains('[plantuml,')
        when: 'the gradle task is invoked'
            def result = GradleRunner.create()
                    .withProjectDir(new File('.'))
                    .withArguments(['generatePDF','--info',
                        '-PdocDir=./src/test/testPdfSpecificTheme',
                        '-PmainConfigFile=testPdfSpecificThemeConfig.groovy',
                        '-PpdfThemeDir=./testPdfTheme'])
                    .build()
        then: 'the task has been successfully executed'
        println result.output
            result.task(":generatePDF").outcome == SUCCESS
        and: 'the output does not contain the warning "image to embed not found or not readable"'
            println result.output
            result.output.contains('image to embed not found or not readable') == false
        and: 'it also does not contain any other error'
            // the output will contain some dependencies with "error" in the path name
            // so let's first remove this and then check for other errors
            // Downloading https://plugins.gradle.org/m2/com/google/errorprone/error_prone_annotations/2.3.4/error_prone_annotations-2.3.4.jar to /tmp/gradle_download4302981685249827904bin
            result.output.toLowerCase().replaceAll("/error","").contains('error') == false
    }

}
