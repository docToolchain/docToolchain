package org.docToolchain.tasks

import org.docToolchain.atlassian.confluence.ConfluenceService

import java.util.logging.Logger

class WipeConfluenceSpaceTask extends AbstractConfluenceTask {

    Logger LOGGER = Logger.getLogger(WipeConfluenceSpaceTask.class.getName())

    WipeConfluenceSpaceTask(ConfigObject config) {
        super(config)
    }

    @Override
    void execute() {
        LOGGER.warning("Wiping Confluence space...")
        new ConfluenceService(configService).wipeConfluenceSpace(confluenceClient)
    }
}
