package org.docToolchain.atlassian.clients

import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient
import org.docToolchain.configuration.ConfigService
import org.docToolchain.util.TestUtils

class ConfluenceClientV2Spec extends ConfluenceClientSpec {

    final confluenceClientMockResponse = [
        data: [
            results: [
                [
                    id: "123456"
                ]
            ]
        ]
    ]

    @Override
    ConfluenceClient getConfluenceClient(ConfigService configService) {
        def mock = GroovySpy(RESTClient, global: true)
        mock.get(_) >> confluenceClientMockResponse
        return new ConfluenceClientV2(configService)
    }

    @Override
    ConfluenceClient setupConfluenceClientToFetchPagesBySpaceKey(ConfigService configService) {
        def mock = GroovySpy(RESTClient, global: true)
        mock.get(_)  >> confluenceClientMockResponse
            >> [data: new JsonSlurper().parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV2/space.json"))]
            >> [data: [results: []]]
        return new ConfluenceClientV2(configService)
    }

    @Override
    ConfluenceClient setupConfluenceClientToFetchPagesByAncestorIdKey(ConfigService configService) {
        def mock = GroovySpy(RESTClient, global: true)
        JsonSlurper jsonSlurper = new JsonSlurper()
        mock.get(_) >> confluenceClientMockResponse
            >> [data: jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV2/ancestorId.json"))]
            >> [data: [results: []]]
            >> [data: jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV2/ancestorId_child.json"))]
            >> [data: [results: []]]
        return new ConfluenceClientV2(configService)
    }

    def "test initialization of the ConfluenceV2 client"() {
        when: "i create a ConfluenceClientV2"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        then: "the client is created and the spaceId has been set correctly"
            confluenceClient != null
            confluenceClient.spaceId == "123456"
    }
}
