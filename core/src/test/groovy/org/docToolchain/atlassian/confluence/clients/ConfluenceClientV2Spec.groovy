package org.docToolchain.atlassian.confluence.clients

import groovy.json.JsonSlurper
import org.docToolchain.configuration.ConfigService
import org.docToolchain.util.TestUtils

class ConfluenceClientV2Spec extends ConfluenceClientSpec {

    final confluenceClientMockResponse = [
        results: [
            [
                id: "123456"
            ]
        ]
    ]

    @Override
    ConfluenceClient getConfluenceClient(ConfigService configService) {
        def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
        mock.doRequestAndFailIfNot20x(_) >> confluenceClientMockResponse
        return new ConfluenceClientV2(configService)
    }

    @Override
    ConfluenceClient setupConfluenceClientToFetchPagesBySpaceKey(ConfigService configService) {
        def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
        mock.doRequestAndFailIfNot20x(_)  >> confluenceClientMockResponse
            >> new JsonSlurper().parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV2/space.json"))
            >> [results: []]
        return new ConfluenceClientV2(configService)
    }

    @Override
    ConfluenceClient setupConfluenceClientToFetchPagesByAncestorIdKey(ConfigService configService) {
        def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
        JsonSlurper jsonSlurper = new JsonSlurper()
        mock.doRequestAndFailIfNot20x(_) >> confluenceClientMockResponse
            >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV2/ancestorId.json"))
            >> [results: []]
            >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/asciidoc2confluence/json/apiV2/ancestorId_child.json"))
            >> [results: []]
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

    def "test attachment diff can be detected correctly"() {
        def mockedResponse =   [results:[[mediaTypeDescription:"PNG Image",
                                          webuiLink:"", downloadLink:"", createdAt:"null", id:"att17858747",
                                          comment:"automatically uploaded #a937de4e4d3a556d0b8886d7ecb5e0c5#",
                                          version: [number:1,
                                                    message:"automatically uploaded #a937de4e4d3a556d0b8886d7ecb5e0c5#",
                                                    minorEdit:false, authorId:"557058:5d896875-errew-85a4-werwerwerwerwe",
                                                    createdAt:2
                                          ],
                                          title:"01_2_iso-25010-topics-DE.drawio.png", fileSize:100109, status:"current",
                                          mediaType:"image/png", pageId:17695008, fileId:"e79-rtzrtz-tzrz-d7aebea2986f",
                                          _links:[
                                              download:"", webui:""]]], _links:[:]]
        when: "i create a ConfluenceClientV2"
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
