package org.docToolchain.tasks

import org.docToolchain.atlassian.jira.JiraService

import java.util.logging.Logger

class ExportJiraSprintChangelogTask extends DocToolchainTask {

    Logger LOGGER = Logger.getLogger(ExportJiraSprintChangelogTask.class.getName())

    JiraService jiraService

    ExportJiraSprintChangelogTask(ConfigObject config) {
        super(config)
        this.jiraService = new JiraService(configService)
    }

    @Override
    void execute() {
        LOGGER.info("Starting Jira Sprint Changelog Export...")
        jiraService.exportJiraSprintChangelog()
    }
}
