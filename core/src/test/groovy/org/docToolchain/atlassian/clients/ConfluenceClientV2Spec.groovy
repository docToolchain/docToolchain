package org.docToolchain.atlassian.clients

import groovyx.net.http.RESTClient
import org.docToolchain.configuration.ConfigService

class ConfluenceClientV2Spec extends ConfluenceClientSpec {

    @Override
    ConfluenceClient getConfluenceClient(ConfigService configService) {
        def mock = GroovySpy(RESTClient, global: true)
        mock.get(_) >> [
            data: [
                results: [
                    [
                        id: "123456"
                    ]
                ]
            ]
        ]
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
