package docToolchain.export2confluence

import groovy.json.JsonSlurper
import org.docToolchain.atlassian.ConfluenceClient
import spock.lang.Specification

class Export2ConfluenceSpec extends Specification {

    GroovyShell setupShell() {
        def config = new ConfigObject()
        config.confluence = [
            api : 'https://my.confluence/rest/api/',
            useV1Api : true,
        ]
        return new GroovyShell(new Binding([
            config: config
        ]))
    }

    void 'test space output'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'

        def jsonSlurper = new JsonSlurper()
        GroovyShell shell = setupShell()
        def script = shell.parse(new File("./core/src/main/groovy/org/docToolchain/scripts/asciidoc2confluence.groovy"))
        script.setProperty("confluenceClient", Stub(constructorArgs: ["mock", "mock"],ConfluenceClient.class){
            fetchPagesBySpaceKey(_, _, _) >> [data: jsonSlurper.parse(new File('./src/test/groovy/docToolchain/export2confluence/space.json'))] >>
                // no more results
                [data: [results: []]] })
        when: 'run retrieveAllPagesBySpace'
        def result = script.retrieveAllPagesBySpace('spaceKey', 100)
        then: 'the pages are given'
        result.size() == 10
        result == ['page old 1': [title: 'page old 1', id: '688183', parentId: '47456033'],
                   'page old 2': [title: 'page old 2', id: '1081416', parentId: null],
                   'page old 3': [title: 'page old 3', id: '4390965', parentId: '1081416'],
                   'page old 4': [title: 'page old 4', id: '4391029', parentId: '4390965'],
                   'page old 5': [title: 'page old 5', id: '4391039', parentId: '14094801'],
                   'page old 6': [title: 'page old 6', id: '4391048', parentId: '4390965'],
                   'page old 7': [title: 'page old 7', id: '4391051', parentId: '4390965'],
                   'page old 8': [title: 'page old 8', id: '4391056', parentId: '14094801'],
                   'page old 9': [title: 'page old 9', id: '4718619', parentId: '4390965'],
                   'page old 0': [title: 'page old 0', id: '4718623', parentId: '4390965']]
    }

    void 'test ancestor-id output'() {
        setup: 'load org.docToolchain.scripts.asciidoc2confluence'
        GroovyShell shell = setupShell()
        def jsonSlurper = new JsonSlurper()
        def script = shell.parse(new File("./core/src/main/groovy/org/docToolchain/scripts/asciidoc2confluence.groovy"))
        script.setProperty("confluenceClient", Stub(constructorArgs: ["mock", "mock"],ConfluenceClient.class){
            fetchPagesByAncestorId(_, _, _) >> [data: jsonSlurper.parse(new File('./src/test/groovy/docToolchain/export2confluence/ancestorId.json'))] >>
                // no more pages outside the limit
                [data: [results: []]] >>
                // first child loop
                [data: jsonSlurper.parse(new File('./src/test/groovy/docToolchain/export2confluence/ancestorId_child.json'))] >>
                // all other child loops
                [data: [results: []]] })
        when: 'run retrieveAllPagesByAncestorId'
        def result = script.retrieveAllPagesByAncestorId(['123'], 100)
        then: 'the pages are given'
        result.size() == 7
        result == ['page 1': [title: 'page 1', id: '183954870', parentId: '123'],
                   'page 2': [title: 'page 2', id: '92996640', parentId: '123'],
                   'page 3': [title: 'page 3', id: '101845068', parentId: '123'],
                   'page 4': [title: 'page 4', id: '183954872', parentId: '123'],
                   'page 5': [title: 'page 5', id: '71210367', parentId: '183954870'],
                   'page 6': [title: 'page 6', id: '76418864', parentId: '183954870'],
                   'page 7': [title: 'page 7', id: '71208921', parentId: '183954870']]
    }


}
