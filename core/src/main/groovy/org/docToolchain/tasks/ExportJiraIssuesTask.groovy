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
        String taskSubfolderName = configService.getConfigProperty("jira.resultsFolder")
        //TODO targetDir is currently dependend on Gradle
        String targetDir = configService.getConfigProperty("targetDir")
        File targetFolder = new File(targetDir + File.separator + taskSubfolderName)
        if (!targetFolder.exists()){
            targetFolder.mkdirs()
        }
        println("Output folder for 'exportJiraIssues' task is: '${targetFolder}'")
        jiraService.exportJira()
    }
}
