package org.docToolchain.atlassian.jira

import groovy.json.JsonSlurper
import org.docToolchain.atlassian.jira.clients.RestClient
import org.docToolchain.configuration.ConfigService
import org.docToolchain.util.TestUtils
import spock.lang.Specification

class JiraServiceSpec extends Specification {
    def "test exportJiraSprintChangelog"() {
        setup: "a JiraService instance in a clean environment"
            new File("foo/JiraRequests").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = new ConfigObject()
        new File("foo/JiraRequests").mkdirs()
        config.targetDir = 'foo'
            config.jira = [
                    api: "https://jira.atlassian.com",
                    credentials: "user:password",
                    project: 'PLAYG',
                    dateTimeFormatParse: "yyyy-MM-dd'T'H:m:s.SSSz",
                    dateTimeFormatOutput: "dd.MM.yyyy HH:mm:ss z",
                    resultsFilename: 'JiraTicketsContent',
                    saveAsciidoc: true,
                    saveExcel: true,
                    resultsFolder: 'JiraRequests'
                ]
            config.sprintChangelog = [
                    sprintState: 'closed',
                    ticketStatus: "Done, Closed",
                    sprintBoardId: "TEST",
                    showAssignee: false,
                    showTicketStatus: false,
                    showTicketType: true,
                    resultsFolder: 'Sprints',
                    allSprintsFilename: 'Sprints_Changelogs'
                ]
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
            new File("foo/JiraRequests/sprint_1.adoc").exists()
            new File("foo/JiraRequests/sprint_2.adoc").exists()
            new File("foo/JiraRequests/Sprints_Changelogs.xlsx").exists()
    }

    def "test exportJiraSprintChangelog but disabled Excel-Export"() {
        setup: "a JiraService instance in a clean environment"
            new File("foo/JiraRequests").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = new ConfigObject()
            new File("foo/JiraRequests").mkdirs()
            config.targetDir = 'foo'
            config.jira = [
                api: "https://jira.atlassian.com",
                credentials: "user:password",
                project: 'PLAYG',
                dateTimeFormatParse: "yyyy-MM-dd'T'H:m:s.SSSz",
                dateTimeFormatOutput: "dd.MM.yyyy HH:mm:ss z",
                resultsFilename: 'JiraTicketsContent',
                saveAsciidoc: true,
                saveExcel: false,
                resultsFolder: 'JiraRequests'
            ]
            config.sprintChangelog = [
                sprintState: 'closed',
                ticketStatus: "Done, Closed",
                sprintBoardId: "TEST",
                showAssignee: false,
                showTicketStatus: false,
                showTicketType: true,
                resultsFolder: 'Sprints',
                allSprintsFilename: 'Sprints_Changelogs'
            ]
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
            new File("foo/JiraRequests/sprint_1.adoc").exists()
            new File("foo/JiraRequests/sprint_1.adoc").exists()
        and: 'disabled export of Excel file is not created'
            !new File("foo/JiraRequests/Sprints_Changelogs.xlsx").exists()
    }

    def "test exportJiraSprintChangelog but disabled Excel-Export"() {
        setup: "a JiraService instance in a clean environment"
            new File("foo/JiraRequests").deleteDir()
            JsonSlurper jsonSlurper = new JsonSlurper()
            ConfigObject config = new ConfigObject()
            new File("foo/JiraRequests").mkdirs()
            config.targetDir = 'foo'
            config.jira = [
                api: "https://jira.atlassian.com",
                credentials: "user:password",
                project: 'PLAYG',
                dateTimeFormatParse: "yyyy-MM-dd'T'H:m:s.SSSz",
                dateTimeFormatOutput: "dd.MM.yyyy HH:mm:ss z",
                resultsFilename: 'JiraTicketsContent',
                saveAsciidoc: false,
                saveExcel: true,
                resultsFolder: 'JiraRequests'
            ]
            config.sprintChangelog = [
                sprintState: 'closed',
                ticketStatus: "Done, Closed",
                sprintBoardId: "TEST",
                showAssignee: false,
                showTicketStatus: false,
                showTicketType: true,
                resultsFolder: 'Sprints',
                allSprintsFilename: 'Sprints_Changelogs'
            ]
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
            new File("foo/JiraRequests/Sprints_Changelogs.xlsx").exists()
        and: 'disabled export of AsciiDoc file is not created'
            !new File("foo/JiraRequests/sprint_1.adoc").exists()
            !new File("foo/JiraRequests/sprint_1.adoc").exists()
    }

}
