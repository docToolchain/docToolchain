package org.docToolchain.tasks

import org.docToolchain.atlassian.confluence.clients.ConfluenceClient
import org.docToolchain.atlassian.confluence.clients.ConfluenceClientV1
import org.docToolchain.atlassian.confluence.clients.ConfluenceClientV2

abstract class AbstractConfluenceTask extends DocToolchainTask {

    ConfluenceClient confluenceClient

    AbstractConfluenceTask(ConfigObject config) {
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
}
