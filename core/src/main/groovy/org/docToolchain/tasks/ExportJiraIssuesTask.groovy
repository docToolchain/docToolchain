package org.docToolchain.tasks

import org.docToolchain.atlassian.jira.JiraService

import java.util.logging.Logger

class ExportJiraIssuesTask extends DocToolchainTask {

    Logger LOGGER = Logger.getLogger(ExportJiraIssuesTask.class.getName())

    JiraService jiraService

    ExportJiraIssuesTask(ConfigObject config) {
        super(config)
        this.jiraService = new JiraService(configService)
    }

    @Override
    void execute() {
        LOGGER.info("Starting Jira Export...")
        jiraService.exportIssues()
    }
}
