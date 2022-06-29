package docToolchain
import spock.lang.*
import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*

class MenuSpec extends Specification {

    class ContentFixture {
        def menu
        def entriesMap
        def newEntries
    }

    void 'test empty published content'() {
        given: 'empty published_content'
            Binding binding = new Binding()
            binding.published_content = []
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are empty'
            binding.content.menu == [:]
            binding.content.entriesMap == [:]
            binding.content.newEntries == []
    }

    void 'test with published content'() {
        given: '3 pages in published_content'
            Binding binding = new Binding()
            binding.published_content = [
                ['jbake-menu': 'foo', 'jbake-title': 'Lorem Ipsum', 'jbake-order': 10, uri : 'foo/10_lorem-ipsum.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Dolor sit amet', 'jbake-order': 20, uri : 'foo/20_dolor_sit_amet.html'],
                ['jbake-menu': 'bar', 'jbake-title': 'Adipiscing elit', 'jbake-order': 10, uri : 'bar/10_adipiscing_elit.html']
            ]
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are computed'
            binding.content.menu == [
                foo:[
                    [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'foo/10_lorem-ipsum.html'],
                    [title: 'Dolor sit amet', order: 20, filename: null, uri: 'foo/20_dolor_sit_amet.html']
                ],
                bar:[
                    [title: 'Adipiscing elit', order: 10, filename: null, uri: 'bar/10_adipiscing_elit.html']
                ]
            ]
            binding.content.entriesMap == [:]
            binding.content.newEntries == []
    }

    def runMenuScript(Binding binding) {
        binding.content = new ContentFixture()
        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(new File('src/site/groovy/menu.groovy'))
        script.run()
    }
}
