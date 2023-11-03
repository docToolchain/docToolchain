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

    def "test ConfluencClientV1 API path without context"() {
        given: "i create a Confluence config and the API does not have a context path"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com/rest/api/",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClientV1 confluenceClient = getConfluenceClient(configService) as ConfluenceClientV1
        when: "the client has been created"
            def apiPath = confluenceClient.API_V1_PATH
        then: "the API path is set without the context path"
            apiPath == "/rest/api/"
    }

    def "test ConfluencClientV1 API path could be context aware"() {
        given: "i create a Confluence config that includes the API context path and has no trailing slash"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com/foo/rest/api",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClientV1 confluenceClient = getConfluenceClient(configService) as ConfluenceClientV1
        when: "the client has been created"
            def apiPath = confluenceClient.API_V1_PATH
        then: "the API path is set with the context path and a trailing slash"
            apiPath == "/foo/rest/api/"
    }

    def "test default API path is set correctly"() {
        given: "i create a Confluence config with host only"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClientV1 confluenceClient = getConfluenceClient(configService) as ConfluenceClientV1
        when: "the client has been created"
            def apiPath = confluenceClient.API_V1_PATH
        then: "the default API path is set correctly"
            apiPath == "/wiki/rest/api/"
    }
}
