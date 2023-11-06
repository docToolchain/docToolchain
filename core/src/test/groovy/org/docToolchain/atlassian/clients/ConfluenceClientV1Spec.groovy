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

    def "test attachment diff can be detected correctly"() {
        def mockedResponse =  [results:[
            [id:"att178445677", type:"attachment", status:"current", title:"01_2_iso-25010-topics-DE.drawio.png",
             macroRenderedOutput:[:], metadata:[comment:"automatically uploaded #a937de4e4d3a556d0b8886d7ecb5e0c5#", mediaType:"image/png"],
             extensions:[mediaType:"image/png", fileSize:"100109", comment:"automatically uploaded #a937de4e4d3a556d0b8886d7ecb5e0c5#",
                         mediaTypeDescription:"PNG Image", fileId:"e790d35e-bdc1", collectionName:"contentId-44444"],
             _expandable:[], _links:[webui:"", self:"", download:""]]], start:0, limit:50, size:1, _links:[base:"", context:"/wiki", self:""]]
        when: "i create a ConfluenceClientV1"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        and : "i check if the attachment has changed"
            def hasNotChanged = confluenceClient.attachmentHasChanged(mockedResponse, "a937de4e4d3a556d0b8886d7ecb5e0c5")
            def hasChanged = confluenceClient.attachmentHasChanged(mockedResponse, "somethingdifferent")
        then: "the attachment has changed"
            hasChanged == true
            hasNotChanged == false
    }
}
