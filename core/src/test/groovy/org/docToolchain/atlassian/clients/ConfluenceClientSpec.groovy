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
}
