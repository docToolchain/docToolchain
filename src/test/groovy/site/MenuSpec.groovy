package docToolchain
import spock.lang.*
import org.gradle.testkit.runner.GradleRunner
import static org.gradle.testkit.runner.TaskOutcome.*

class MenuSpec extends Specification {

    class ContentFixture {
        def menu
        def entriesMap
        def newEntries
        def rootpath = '/root_path/'
        def uri = 'content_uri/'
    }

    void 'test empty published content and empty config'() {
        given: 'empty published_content'
            Binding binding = new Binding()
            binding.config = [:]
            binding.published_content = []
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are empty'
            binding.content.menu == [:]
            binding.content.entriesMap == [:]
            binding.content.newEntries == []
    }

    void 'test with published content'() {
        given: '3 pages in published_content and empty menu config'
            Binding binding = new Binding()
            binding.config = [site_menu: [:]]
            binding.published_content = [
                ['jbake-menu': 'foo', 'jbake-title': 'Lorem Ipsum', 'jbake-order': '10', uri : 'foo/10_lorem-ipsum.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Dolor sit amet', 'jbake-order': '20', uri : 'foo/20_dolor_sit_amet.html'],
                ['jbake-menu': 'bar', 'jbake-title': 'Adipiscing elit', 'jbake-order': '10', uri : 'bar/10_adipiscing_elit.html']
            ]
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are computed'
            binding.content.menu == [
                foo:[
                    [title: 'Lorem Ipsum', order: '10', filename: null, uri: 'foo/10_lorem-ipsum.html'],
                    [title: 'Dolor sit amet', order: '20', filename: null, uri: 'foo/20_dolor_sit_amet.html']
                ],
                bar:[
                    [title: 'Adipiscing elit', order: '10', filename: null, uri: 'bar/10_adipiscing_elit.html']
                ]
            ]
            binding.content.entriesMap ==  [
                foo:[ 'foo', [
                        [title: 'Lorem Ipsum', order: '10', filename: null, uri: 'foo/10_lorem-ipsum.html'],
                        [title: 'Dolor sit amet', order: '20', filename: null, uri: 'foo/20_dolor_sit_amet.html']
                    ]
                ],
                bar:[ 'bar', [
                        [title: 'Adipiscing elit', order: '10', filename: null, uri: 'bar/10_adipiscing_elit.html']
                    ]
                ]
            ]
            binding.content.newEntries == [
                [ isActive: '', href: '/root_path/foo/10_lorem-ipsum.html', title: 'foo'],
                [ isActive: '', href: '/root_path/bar/10_adipiscing_elit.html', title: 'bar']
            ]
    }

    void 'test with published content and menu config'() {
        given: '3 pages in published_content and menu config'
            Binding binding = new Binding()
            binding.config = [site_menu: [code1: 'title1', code2: 'title2']]
            binding.published_content = [
                ['jbake-menu': 'code1', 'jbake-title': 'Lorem Ipsum', 'jbake-order': '10', uri : 'pages/10_lorem-ipsum.html'],
                ['jbake-menu': 'code1', 'jbake-title': 'Dolor sit amet', 'jbake-order': '20', uri : 'pages/20_dolor_sit_amet.html'],
                ['jbake-menu': 'code2', 'jbake-title': 'Adipiscing elit', 'jbake-order': '30', uri : 'pages/30_adipiscing_elit.html']
            ]
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are computed'
            binding.content.menu == [
                code1:[
                    [title: 'Lorem Ipsum', order: '10', filename: null, uri: 'pages/10_lorem-ipsum.html'],
                    [title: 'Dolor sit amet', order: '20', filename: null, uri: 'pages/20_dolor_sit_amet.html']
                ],
                code2:[
                    [title: 'Adipiscing elit', order: '30', filename: null, uri: 'pages/30_adipiscing_elit.html']
                ]
            ]
            binding.content.entriesMap ==  [
                code1:[ 'title1', [
                        [title: 'Lorem Ipsum', order: '10', filename: null, uri: 'pages/10_lorem-ipsum.html'],
                        [title: 'Dolor sit amet', order: '20', filename: null, uri: 'pages/20_dolor_sit_amet.html']
                    ]
                ],
                code2:[ 'title2', [
                        [title: 'Adipiscing elit', order: '30', filename: null, uri: 'pages/30_adipiscing_elit.html']
                    ]
                ]
            ]
            binding.content.newEntries == [
                [ isActive: '', href: '/root_path/pages/10_lorem-ipsum.html', title: 'title1'],
                [ isActive: '', href: '/root_path/pages/30_adipiscing_elit.html', title: 'title2']
            ]
    }

    def runMenuScript(Binding binding) {
        binding.content = new ContentFixture()
        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(new File('src/site/groovy/menu.groovy'))
        script.run()
    }
}
