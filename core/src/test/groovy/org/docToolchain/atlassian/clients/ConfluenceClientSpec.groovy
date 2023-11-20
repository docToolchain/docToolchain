package org.docToolchain.atlassian.clients

import org.docToolchain.configuration.ConfigService
import spock.lang.Specification

abstract class ConfluenceClientSpec extends Specification {

    abstract ConfluenceClient getConfluenceClient(ConfigService configService)

    def "test ConfluenceClient with BasicAuth"() {
        when: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        then: "the client is created and configured with basic auth"
            confluenceClient != null
            confluenceClient.baseApiUrl == "https://confluence.atlassian.com"
            confluenceClient.API_V1_PATH == "/wiki/rest/api"
            confluenceClient.API_V2_PATH == "/wiki/api/v2"
            confluenceClient.headers.size() == 2
            confluenceClient.headers.get("X-Atlassian-Token") == "no-check"
            confluenceClient.headers.get("Authorization") == "Basic user:password"
            confluenceClient.restClient.getClient().getProperties().get("params").parameters.size() == 1
    }

    def "test ConfluenceClient with BasicAuth and API key"() {
        when: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password",
                apikey: "my-api-key"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        then: "the client is created with basic auth and api key"
            confluenceClient != null
            confluenceClient.baseApiUrl == "https://confluence.atlassian.com"
            confluenceClient.API_V1_PATH == "/wiki/rest/api"
            confluenceClient.API_V2_PATH == "/wiki/api/v2"
            confluenceClient.headers.size() == 3
            confluenceClient.headers.get("X-Atlassian-Token") == "no-check"
            confluenceClient.headers.get("Authorization") == "Basic user:password"
            confluenceClient.headers.get("keyid") == "my-api-key"
            confluenceClient.restClient.getClient().getProperties().get("params").parameters.size() == 1
    }

    def "test ConfluenceClient with Proxy"() {
        when: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password",
                proxy: [
                    host: "proxy.example.com",
                    port: 8080,
                    schema: "https"
                ]
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        then: "the client is created with proxy configured"
            confluenceClient != null
            confluenceClient.restClient.getClient().getProperties().get("params").parameters.size() == 2
            confluenceClient.restClient.getClient().getProperties().get("params").parameters
                .get("http.route.default-proxy").toString() == "https://proxy.example.com:8080"
    }

    def "test ConfluenceClient with Bearer auth"() {
        when: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password",
                bearerToken: "tokenXYZ"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        then: "the client is created with bearer token configure and basic auth is ignored"
            confluenceClient != null
            confluenceClient.restClient.getClient().getProperties().get("params").parameters.size() == 1
            confluenceClient.headers.size() == 2
            confluenceClient.headers.get("Authorization") == "Bearer tokenXYZ"
    }

    def "test ConfluenceClient that uses the new editor"() {
        when: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password",
                enforceNewEditor: "true"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        then: "the client is created and configured with new editor"
            confluenceClient != null
            confluenceClient.editorVersion == "v2"
    }

    def "test ConfluenceClient that uses the old editor"() {
        when: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password",
                enforceNewEditor: "false"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        then: "the client is created and configured with old editor"
            confluenceClient != null
            confluenceClient.editorVersion == "v1"
    }

    abstract ConfluenceClient setupConfluenceClientToFetchPagesBySpaceKey(ConfigService configService)

    def "test fetchPagesBySpaceKey"() {
        setup: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = setupConfluenceClientToFetchPagesBySpaceKey(configService)
        when: "i fetch pages by space key"
         def pages = confluenceClient.fetchPagesBySpaceKey("123456", 100)
        then: "the pages are given"
            pages.size() == 10
            pages == ['page old 1': [title: 'page old 1', id: '688183', parentId: '47456033'],
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

    abstract ConfluenceClient setupConfluenceClientToFetchPagesByAncestorIdKey(ConfigService configService)

    def "test fetchPagesByAncestorId"() {
        setup: "i create a ConfluenceClient"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = setupConfluenceClientToFetchPagesByAncestorIdKey(configService)
        when: "i fetch pages by ancestorId"
            def pages = confluenceClient.fetchPagesByAncestorId(['123'], 100)
        then: "the pages are given"
            pages.size() == 7
            pages == ['page 1': [title: 'page 1', id: '183954870', parentId: '123'],
                       'page 2': [title: 'page 2', id: '92996640', parentId: '123'],
                       'page 3': [title: 'page 3', id: '101845068', parentId: '123'],
                       'page 4': [title: 'page 4', id: '183954872', parentId: '123'],
                       'page 5': [title: 'page 5', id: '71210367', parentId: '183954870'],
                       'page 6': [title: 'page 6', id: '76418864', parentId: '183954870'],
                       'page 7': [title: 'page 7', id: '71208921', parentId: '183954870']]
    }

    def "test ConfluencClient API path without context"() {
        given: "i create a Confluence config and the API does not have a context path"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com/rest/api/",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        when: "the client has been created"
            def baseApiUrl = confluenceClient.baseApiUrl
        then: "the API BaseUrl is set without the context path"
            baseApiUrl == "https://confluence.atlassian.com"
            confluenceClient.API_V1_PATH == "/rest/api"
    }

    def "test default API path is set correctly if there are no trailing slashes"() {
        given: "i create a Confluence config with host only"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com/wiki/api/v2",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        when: "the client has been created"
            def baseApiUrl = confluenceClient.baseApiUrl
        then: "the default API path is set correctly"
            baseApiUrl == "https://confluence.atlassian.com"
            confluenceClient.API_V1_PATH == "/wiki/rest/api"
    }

    def "test ConfluencClient API path could be context aware"() {
        given: "i create a Confluence config that includes the API context path and has no trailing slash"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com/foo/rest/api/",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        when: "the client has been created"
            def baseApiUrl = confluenceClient.baseApiUrl
        then: "the API path is set with the context path and a trailing slash"
            baseApiUrl == "https://confluence.atlassian.com"
            confluenceClient.API_V1_PATH == "/foo/rest/api"
    }

    def "test ConfluencClient API path could contain context only"() {
        given: "i create a Confluence config that includes the API context only"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com/foo",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        when: "the client has been created"
            def baseApiUrl = confluenceClient.baseApiUrl
        then: "the API path is set with the context path and a trailing slash"
            baseApiUrl == "https://confluence.atlassian.com"
            confluenceClient.API_V1_PATH == "/foo/rest/api"
    }

    def "test default API path is set correctly"() {
        given: "i create a Confluence config with host only"
            ConfigObject config = new ConfigObject()
            config.confluence = [
                api: "https://confluence.atlassian.com",
                credentials: "user:password"
            ]
            ConfigService configService = new ConfigService(config)
            ConfluenceClient confluenceClient = getConfluenceClient(configService)
        when: "the client has been created"
            def baseApiUrl = confluenceClient.baseApiUrl
        then: "the default API path is set correctly"
            baseApiUrl == "https://confluence.atlassian.com"
            confluenceClient.API_V1_PATH == "/wiki/rest/api"
            confluenceClient.API_V2_PATH == "/wiki/api/v2"
    }
}
