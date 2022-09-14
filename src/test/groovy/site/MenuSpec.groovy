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
                    [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'foo/10_lorem-ipsum.html', children:[]],
                    [title: 'Dolor sit amet', order: 20, filename: null, uri: 'foo/20_dolor_sit_amet.html', children:[]]
                ],
                bar:[
                    [title: 'Adipiscing elit', order: 10, filename: null, uri: 'bar/10_adipiscing_elit.html', children:[]]
                ]
            ]
            binding.content.entriesMap ==  [
                foo:[ 'foo', [
                        [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'foo/10_lorem-ipsum.html', children:[]],
                        [title: 'Dolor sit amet', order: 20, filename: null, uri: 'foo/20_dolor_sit_amet.html', children:[]]
                    ]
                ],
                bar:[ 'bar', [
                        [title: 'Adipiscing elit', order: 10, filename: null, uri: 'bar/10_adipiscing_elit.html', children:[]]
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
                    [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'pages/10_lorem-ipsum.html', children:[]],
                    [title: 'Dolor sit amet', order: 20, filename: null, uri: 'pages/20_dolor_sit_amet.html', children:[]]
                ],
                code2:[
                    [title: 'Adipiscing elit', order: 30, filename: null, uri: 'pages/30_adipiscing_elit.html', children:[]]
                ]
            ]
            binding.content.entriesMap ==  [
                code1:[ 'title1', [
                        [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'pages/10_lorem-ipsum.html', children:[]],
                        [title: 'Dolor sit amet', order: 20, filename: null, uri: 'pages/20_dolor_sit_amet.html', children:[]]
                    ]
                ],
                code2:[ 'title2', [
                        [title: 'Adipiscing elit', order: 30, filename: null, uri: 'pages/30_adipiscing_elit.html', children:[]]
                    ]
                ]
            ]
            binding.content.newEntries == [
                [ isActive: '', href: '/root_path/pages/10_lorem-ipsum.html', title: 'title1'],
                [ isActive: '', href: '/root_path/pages/30_adipiscing_elit.html', title: 'title2']
            ]
    }

    void 'test hierachical with published content'() {
        given: '6 pages in published_content with empty menu config'
            Binding binding = new Binding()
            binding.config = [site_menu: [:]]
            binding.published_content = [
                ['jbake-menu': 'foo', 'jbake-title': 'Lorem Ipsum', 'jbake-order': '10', uri : 'foo/10_lorem-ipsum.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Section kaz', 'jbake-order': '35', uri : 'foo/kaz/index.html'], // simulate a ':jbake-order: 35' present in the page
                ['jbake-menu': 'foo', 'jbake-title': 'Kaz Page', 'jbake-order': '100', uri : 'foo/kaz/100_page.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Section bar', 'jbake-order': '-987654321', uri : 'foo/22_bar/index.html'], // simulate no ':jbake-order:' attribute present in the page
                ['jbake-menu': 'foo', 'jbake-title': 'Adipiscing elit', 'jbake-order': '10', uri : 'foo/22_bar/10_adipiscing_elit.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Dolor sit amet', 'jbake-order': '20', uri : 'foo/22_bar/20_dolor_sit_amet.html']
            ]
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are computed'
            binding.content.menu == [
                foo:[
                    [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'foo/10_lorem-ipsum.html', children:[]],
                    [title: 'Section bar', order: 22, filename: null, uri: 'foo/22_bar/index.html', children: [
                            [title: 'Adipiscing elit', order: 10, filename: null, uri: 'foo/22_bar/10_adipiscing_elit.html', children:[]],
                            [title: 'Dolor sit amet', order: 20, filename: null, uri: 'foo/22_bar/20_dolor_sit_amet.html', children:[]]
                        ]
                    ],
                    [title: 'Section kaz', order: 35, filename: null, uri: 'foo/kaz/index.html', children: [
                            [title: 'Kaz Page', order: 100, filename: null, uri: 'foo/kaz/100_page.html', children:[]]
                        ]
                    ]
                ]
            ]
            binding.content.entriesMap ==  [
                foo:[ 'foo', [
                        [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'foo/10_lorem-ipsum.html', children:[]],
                        [title: 'Section bar', order: 22, filename: null, uri: 'foo/22_bar/index.html', children: [
                                [title: 'Adipiscing elit', order: 10, filename: null, uri: 'foo/22_bar/10_adipiscing_elit.html', children:[]],
                                [title: 'Dolor sit amet', order: 20, filename: null, uri: 'foo/22_bar/20_dolor_sit_amet.html', children:[]]
                            ]
                        ],
                        [title: 'Section kaz', order: 35, filename: null, uri: 'foo/kaz/index.html', children: [
                                [title: 'Kaz Page', order: 100, filename: null, uri: 'foo/kaz/100_page.html', children:[]]
                            ]
                        ]
                    ]
                ]
            ]
            binding.content.newEntries == [
                [ isActive: '', href: '/root_path/foo/10_lorem-ipsum.html', title: 'foo']
            ]
    }

    void 'test hierachical no index with published content'() {
        given: '6 pages in published_content with menu config'
            Binding binding = new Binding()
            binding.config = [site_menu: [foo: 'My Title', maa: 'Other Title']]
            binding.published_content = [
                ['jbake-menu': 'foo', 'jbake-title': 'Lorem Ipsum', 'jbake-order': '10', uri : 'foo/10_lorem-ipsum.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Dolor sit amet', 'jbake-order': '100', uri : 'foo/bar/100_dolor_sit_amet.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Adipiscing elit', 'jbake-order': '10', uri : 'foo/bar/10_adipiscing_elit.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'One', 'jbake-order': '10', uri : 'foo/30_baz/10_one.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Two', 'jbake-order': '20', uri : 'foo/30_baz/20_two.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Three', 'jbake-order': '30', uri : 'foo/30_baz/30_three.html']
            ]
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are computed'

            binding.content.menu == [
                foo:[
                    [title: 'bar', order: -1, filename: null, uri: null, children: [
                            [title: 'Adipiscing elit', order: 10, filename: null, uri: 'foo/bar/10_adipiscing_elit.html', children:[]],
                            [title: 'Dolor sit amet', order: 100, filename: null, uri: 'foo/bar/100_dolor_sit_amet.html', children:[]]
                        ]
                    ],
                    [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'foo/10_lorem-ipsum.html', children:[]],
                    [title: 'baz', order: 30, filename: null, uri: null, children: [
                            [title: 'One', order: 10, filename: null, uri: 'foo/30_baz/10_one.html', children:[]],
                            [title: 'Two', order: 20, filename: null, uri: 'foo/30_baz/20_two.html', children:[]],
                            [title: 'Three', order: 30, filename: null, uri: 'foo/30_baz/30_three.html', children:[]]
                        ]
                    ]
                ]
            ]

            binding.content.entriesMap ==  [
                foo:[ 'My Title', [
                        [title: 'bar', order: -1, filename: null, uri: null, children: [
                                [title: 'Adipiscing elit', order: 10, filename: null, uri: 'foo/bar/10_adipiscing_elit.html', children:[]],
                                [title: 'Dolor sit amet', order: 100, filename: null, uri: 'foo/bar/100_dolor_sit_amet.html', children:[]]
                            ]
                        ],
                        [title: 'Lorem Ipsum', order: 10, filename: null, uri: 'foo/10_lorem-ipsum.html', children:[]],
                        [title: 'baz', order: 30, filename: null, uri: null, children: [
                                [title: 'One', order: 10, filename: null, uri: 'foo/30_baz/10_one.html', children:[]],
                                [title: 'Two', order: 20, filename: null, uri: 'foo/30_baz/20_two.html', children:[]],
                                [title: 'Three', order: 30, filename: null, uri: 'foo/30_baz/30_three.html', children:[]]
                            ]
                        ]
                    ]
                ],
                maa:[ 'Other Title', []]
            ]
            binding.content.newEntries == [
                [ isActive: '', href: '/root_path/foo/bar/10_adipiscing_elit.html', title: 'My Title']
            ]
    }

     void 'test with index page the the root'() {
        given: '2 pages in published_content and a menu config'
            Binding binding = new Binding()
            binding.config = [site_menu: [foo: 'Some FOO']]
            binding.published_content = [
                // Simulate no 'jbake-order' defined in the pages:
                ['jbake-menu': 'foo', 'jbake-title': 'Lorem Ipsum', 'jbake-order': '-987654321', uri : 'foo/index.html'],
                ['jbake-menu': 'foo', 'jbake-title': 'Dolor sit amet', 'jbake-order': '-1', uri : 'foo/page.html']
            ]
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are computed'
            binding.content.menu == [
                foo:[
                    [title: 'Lorem Ipsum', order: -987654321, filename: null, uri: 'foo/index.html', children:[]],
                    [title: 'Dolor sit amet', order: -1, filename: null, uri: 'foo/page.html', children:[]]
                ]
            ]
            binding.content.entriesMap ==  [
                foo:[ 'Some FOO', [
                        [title: 'Lorem Ipsum', order: -987654321, filename: null, uri: 'foo/index.html', children:[]],
                        [title: 'Dolor sit amet', order: -1, filename: null, uri: 'foo/page.html', children:[]]
                    ]
                ]
            ]
            binding.content.newEntries == [
                [ isActive: '', href: '/root_path/foo/index.html', title: 'Some FOO']
            ]
    }

     void 'test with only folders in the top level folder'() {
        given: '4 pages in published_content and a menu config'
            Binding binding = new Binding()
            binding.config = [site_menu: [p: 'My pages']]
            binding.published_content = [
                ['jbake-menu': 'p', 'jbake-title': 'A', 'jbake-order': '10', uri : 'p/x/a.html'],
                ['jbake-menu': 'p', 'jbake-title': 'B', 'jbake-order': '10', uri : 'p/x/b.html'],
                ['jbake-menu': 'p', 'jbake-title': 'C', 'jbake-order': '10', uri : 'p/y/c.html'],
                ['jbake-menu': 'p', 'jbake-title': 'D', 'jbake-order': '10', uri : 'p/y/d.html']
            ]
        when: 'run the `menu.groovy` script'
            runMenuScript(binding)
        then: 'arrays are computed'
            binding.content.menu == [
                p:[
                    [title: 'x', order: -1, filename: null, uri: null, children: [
                            [title: 'A', order: 10, filename: null, uri: 'p/x/a.html', children:[]],
                            [title: 'B', order: 10, filename: null, uri: 'p/x/b.html', children:[]]
                        ]
                    ],
                    [title: 'y', order: -1, filename: null, uri: null, children: [
                            [title: 'C', order: 10, filename: null, uri: 'p/y/c.html', children:[]],
                            [title: 'D', order: 10, filename: null, uri: 'p/y/d.html', children:[]]
                        ]
                    ]
                ]
            ]
            binding.content.entriesMap ==  [
                p:[ 'My pages', [
                        [title: 'x', order: -1, filename: null, uri: null, children: [
                                [title: 'A', order: 10, filename: null, uri: 'p/x/a.html', children:[]],
                                [title: 'B', order: 10, filename: null, uri: 'p/x/b.html', children:[]]
                            ]
                        ],
                        [title: 'y', order: -1, filename: null, uri: null, children: [
                                [title: 'C', order: 10, filename: null, uri: 'p/y/c.html', children:[]],
                                [title: 'D', order: 10, filename: null, uri: 'p/y/d.html', children:[]]
                            ]
                        ]
                    ]
                ]
            ]
            binding.content.newEntries == [
                [ isActive: '', href: '/root_path/p/x/a.html', title: 'My pages']
            ]
    }

    def runMenuScript(Binding binding) {
        binding.content = new ContentFixture()
        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(new File('src/site/groovy/menu.groovy'))
        script.run()
    }
}
