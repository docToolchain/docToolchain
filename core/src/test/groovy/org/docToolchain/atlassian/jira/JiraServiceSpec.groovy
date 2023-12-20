package org.docToolchain.atlassian.jira

import groovy.json.JsonSlurper
import org.docToolchain.atlassian.jira.clients.RestClient
import org.docToolchain.configuration.ConfigService
import org.docToolchain.util.TestUtils
import spock.lang.Specification

class JiraServiceSpec extends Specification {

    private static final TARGET_DIR = "build/test/jira"
    private static final RESULT_DIR_CHANGELOG = "changelog"
    private static final RESULT_DIR_JIRA_EXPORT = "iiraRequests"

    private static ConfigObject buildDefaultConfig() {
        ConfigObject config = new ConfigObject()
        config.targetDir = TARGET_DIR
        config.jira = [
            api: "https://jira.atlassian.com",
            credentials: "user:password",
            project: 'PLAYG',
            dateTimeFormatParse: "yyyy-MM-dd'T'H:m:s.SSSz",
            dateTimeFormatOutput: "dd.MM.yyyy HH:mm:ss z",
            resultsFilename: 'JiraTicketsContent',
            label: "",
            saveAsciidoc: true,
            saveExcel: true,
            resultsFolder: RESULT_DIR_JIRA_EXPORT,
            exports: [
                [
                    filename:'CurrentSprint',
                    jql:"project='%jiraProject%' AND Sprint in openSprints() ORDER BY priority DESC, duedate ASC",
                    customfields: [customfield_10026:'Story Points']
                ],
            ]
        ]
        config.sprintChangelog = [
            sprintState: 'closed',
            ticketStatus: "Done, Closed",
            sprintBoardId: "TEST",
            showAssignee: false,
            showTicketStatus: false,
            showTicketType: true,
            resultsFolder: RESULT_DIR_CHANGELOG,
            allSprintsFilename: 'Sprints_Changelogs'
        ]
        return config
    }

    def "test exportJiraSprintChangelog"() {
        setup: "a JiraService instance in a clean environment"
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = buildDefaultConfig()
            ConfigService configService = new ConfigService(config)
            def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
            mock.doRequestAndFailIfNot20x(_) >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/getAllSprints.json"))
             >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/getBoardIssuesForSprint.json"))
            JiraService jiraService = new JiraService(configService)
        when: 'we export the Jira sprint changelog'
            jiraService.exportJiraSprintChangelog()
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'the expected files are created'
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_1.adoc").exists()
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_2.adoc").exists()
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/Sprints_Changelogs.xlsx").exists()
        and: 'the expected content is written to the files'
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_1.adoc").text.equals(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/expectedFiles/sprint_1.adoc").text)
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_2.adoc").text.equals(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/expectedFiles/sprint_2.adoc").text)
            //TODO check Excel file content
    }

    def "test exportJiraSprintChangelog but disabled Excel-Export"() {
        setup: "a JiraService instance in a clean environment"
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = buildDefaultConfig()
            config.jira.saveExcel = false
            ConfigService configService = new ConfigService(config)
            def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
            mock.doRequestAndFailIfNot20x(_) >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/getAllSprints.json"))
                >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/getBoardIssuesForSprint.json"))
            JiraService jiraService = new JiraService(configService)
        when: 'we export the Jira sprint changelog'
            jiraService.exportJiraSprintChangelog()
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'the expected files are created'
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_1.adoc").exists()
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_1.adoc").exists()
        and: 'disabled export of Excel file is not created'
            !new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/Sprints_Changelogs.xlsx").exists()
    }

    def "test exportJiraSprintChangelog but disabled Asciidoc-Export"() {
        setup: "a JiraService instance in a clean environment"
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = buildDefaultConfig()
            config.jira.saveAsciidoc = false
            ConfigService configService = new ConfigService(config)
            def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
            mock.doRequestAndFailIfNot20x(_) >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/getAllSprints.json"))
                >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/getBoardIssuesForSprint.json"))
            JiraService jiraService = new JiraService(configService)
        when: 'we export the Jira sprint changelog'
            jiraService.exportJiraSprintChangelog()
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'the expected files are created'
            new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/Sprints_Changelogs.xlsx").exists()
        and: 'disabled export of AsciiDoc file is not created'
            !new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_1.adoc").exists()
            !new File("${TARGET_DIR}/${RESULT_DIR_CHANGELOG}/sprint_1.adoc").exists()
    }

    def "test exportJira"() {
        setup: "a JiraService instance in a clean environment"
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = buildDefaultConfig()
            ConfigService configService = new ConfigService(config)
            def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
            mock.doRequestAndFailIfNot20x(_) >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/searchForIssueByJQL.json"))
            JiraService jiraService = new JiraService(configService)
        when: 'we export the Jira issues'
            jiraService.exportJira()
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'the expected files are created'
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}/CurrentSprint.adoc").exists()
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}/CurrentSprint.xlsx").exists()
        and: 'the expected content is written to the files'
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}/CurrentSprint.adoc").text.equals(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/expectedFiles/CurrentSprint.adoc").text)
    }

    def "test exportJira but disabled Excel-Export"() {
        setup: "a JiraService instance in a clean environment"
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = buildDefaultConfig()
            config.jira.saveExcel = false
            ConfigService configService = new ConfigService(config)
            def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
            mock.doRequestAndFailIfNot20x(_) >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/searchForIssueByJQL.json"))
            JiraService jiraService = new JiraService(configService)
        when: 'we export the Jira issues'
            jiraService.exportJira()
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'the expected files are created'
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}/CurrentSprint.adoc").exists()
        and: 'disabled export of Excel file is not created'
            !new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}/CurrentSprint.xlsx").exists()
    }

    def "test exportJira but disabled Asciidoc-Export"() {
        setup: "a JiraService instance in a clean environment"
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = buildDefaultConfig()
            config.jira.saveAsciidoc = false
            ConfigService configService = new ConfigService(config)
            def mock = GroovySpy(RestClient, global: true, constructorArgs: [configService])
            mock.doRequestAndFailIfNot20x(_) >> jsonSlurper.parse(new File("${TestUtils.TEST_RESOURCES_DIR}/jira/api/fixtures/searchForIssueByJQL.json"))
            JiraService jiraService = new JiraService(configService)
        when: 'we export the Jira issues'
            jiraService.exportJira()
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'the expected files are created'
            new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}/CurrentSprint.xlsx").exists()
        and: 'disabled export of AsciiDoc file is not created'
            !new File("${TARGET_DIR}/${RESULT_DIR_JIRA_EXPORT}/CurrentSprint.adoc").exists()
    }

}
