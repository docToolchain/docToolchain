package org.docToolchain.tasks

import java.util.logging.Logger

class VerifyConfluenceApiAccessTask extends AbstractConfluenceTask {

    Logger LOGGER = Logger.getLogger(VerifyConfluenceApiAccessTask.class.getName())

    VerifyConfluenceApiAccessTask(ConfigObject config) {
        super(config)
    }

    @Override
    void execute() {
        LOGGER.info("Verifying confluence API access...")
        confluenceClient.verifyCredentials()
    }
}
