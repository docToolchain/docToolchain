package org.docToolchain.atlassian.clients

import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.docToolchain.configuration.ConfigService
import org.docToolchain.util.TestUtils

class ConfluenceClientV1Spec extends ConfluenceClientSpec {

    @Override
    ConfluenceClient getConfluenceClient(ConfigService configService) {
        return new ConfluenceClientV1(configService)
    }

    @Override
    ConfluenceClient setupConfluenceClientToFetchPagesBySpaceKey(ConfigService configService) {
        def mock = GroovySpy(RESTClient, global: true)
        mock.get(_) >> [data: new JsonSlurper().parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV1/space.json"))]
            >> [data: [results: []]]
        return new ConfluenceClientV1(configService)
    }

    @Override
    ConfluenceClient setupConfluenceClientToFetchPagesByAncestorIdKey(ConfigService configService) {
        JsonSlurper jsonSlurper = new JsonSlurper()
        def mock = GroovySpy(RESTClient, global: true)
        mock.get(_) >> [data: jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV1/ancestorId.json"))]
            >> [data: [results: []]]
            >> [data: jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV1/ancestorId_child.json"))]
            >> [data: [results: []]]
        return new ConfluenceClientV1(configService)
    }
}
