package org.docToolchain.atlassian.clients

import org.docToolchain.configuration.ConfigService

class ConfluenceClientV1Spec extends ConfluenceClientSpec {

    @Override
    ConfluenceClient getConfluenceClient(ConfigService configService) {
        return new ConfluenceClientV1(configService)
    }
}
