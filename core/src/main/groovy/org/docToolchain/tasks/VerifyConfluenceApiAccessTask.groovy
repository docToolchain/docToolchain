package org.docToolchain.tasks


import org.docToolchain.atlassian.clients.ConfluenceClient
import org.docToolchain.atlassian.clients.ConfluenceClientV1
import org.docToolchain.atlassian.clients.ConfluenceClientV2

import java.util.logging.Logger

class VerifyConfluenceApiAccessTask extends DocToolchainTask {

    Logger LOGGER = Logger.getLogger(VerifyConfluenceApiAccessTask.class.getName())

    ConfluenceClient confluenceClient

    VerifyConfluenceApiAccessTask(ConfigObject config) {
        super(config)
        Boolean useV1Api = configService.getConfigProperty('confluence.useV1Api')
        if(useV1Api){
            println("Using Confluence API V1")
            this.confluenceClient = new ConfluenceClientV1(configService)
        } else {
            println("Using Confluence API V2")
            this.confluenceClient = new ConfluenceClientV2(configService)
        }
    }

    @Override
    void execute() {
        LOGGER.info("Verifying confluence API access...")
        confluenceClient.verifyCredentials()
    }
}
