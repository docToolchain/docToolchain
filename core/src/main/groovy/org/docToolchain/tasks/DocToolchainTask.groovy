package org.docToolchain.tasks

import org.docToolchain.configuration.ConfigService

abstract class DocToolchainTask {

    ConfigService configService

    DocToolchainTask(ConfigObject config) {
        this.configService = new ConfigService(config)
    }

    abstract void execute()
}
