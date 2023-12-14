package org.docToolchain.atlassian.jira

import org.docToolchain.atlassian.jira.clients.JiraClient
import org.docToolchain.atlassian.jira.clients.JiraServerClient
import org.docToolchain.atlassian.jira.converter.AsciiDocConverter
import org.docToolchain.atlassian.jira.converter.ExcelConverter
import org.docToolchain.configuration.ConfigService

class JiraService {

    protected JiraClient jiraClient
    private final ConfigService configService
    final File targetFolder
    private final String defaultFields = 'priority,created,resolutiondate,summary,assignee,status'

    JiraService(ConfigService configService) {
        this.configService = configService
        this.jiraClient = new JiraServerClient(configService)
        String taskSubfolderName = configService.getConfigProperty("jira.resultsFolder")
        //TODO targetDir is currently dependend on Gradle
        String targetDir = configService.getConfigProperty("targetDir")
        this.targetFolder = new File(targetDir + File.separator + taskSubfolderName)
        if (!targetFolder.exists()){
            targetFolder.mkdirs()
        }
        println("Output folder for 'exportJiraIssues' task is: '${targetFolder}'")
    }

    def exportIssues(){
        if(configService.getConfigProperty("jira.jql")){
            processLegacyRequests()
        }
        def jiraRequests = configService.getConfigProperty('jira.requests')
        jiraRequests.each {rq ->
                processJiraRequests(rq)
            }
    }

    protected processJiraRequests(rq) {
        println(">>>Found Jira request '${rq}'")
        def jiraConfig = configService.getFlatConfigSubTree('jira')
        def jiraRoot = jiraConfig.api
        def jiraProject = jiraConfig.project
        def jiraLabel = jiraConfig.label
        def jiraDateTimeFormatParse = jiraConfig.dateTimeFormatParse
        def jiraDateTimeOutput = jiraConfig.dateTimeFormatOutput
        AsciiDocConverter asciiDocConverter = new AsciiDocConverter()
        ExcelConverter excelConverter = new ExcelConverter()

        println("Request to Jira API for '${rq.get("filename")}' with query: '${rq.get("jql")}'")

        def allHeaders = "${defaultFields},${rq.get("customfields").values().join(",")}"
        def allFieldIds = "${defaultFields},${rq.get("customfields").keySet().join(",")}"
        println("Preparing headers for default & custom fields: ${allHeaders}")
        println("Preparing field IDs for default & custom fields: ${allFieldIds}")
        if(configService.getConfigProperty("jira.saveAsciidoc")){
            asciiDocConverter.prepareOutputFile(rq.get("filename"), targetFolder, allHeaders)
        } else {
            println("Set saveAsciidoc=true your config to save results in AsciiDoc file")
        }
        if(configService.getConfigProperty("jira.saveExcel")){
            excelConverter.prepareOutputFile(rq.get("filename"), targetFolder, allHeaders)
        } else {
            println("Set saveExcel=true your config to save results in Excel file")

        }

        jiraClient.getIssuesByJql(
            rq.get("jql").replaceAll('%jiraProject%', jiraProject).replaceAll('%jiraLabel%', jiraLabel),
            allFieldIds
        ).issues.each { issue ->
            if(configService.getConfigProperty("jira.saveAsciidoc")){
                asciiDocConverter.convertAndAppend(asciiDocConverter.outputFile, issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, rq.get("customfields"))
            }
            if(configService.getConfigProperty("jira.saveExcel")){
                excelConverter.convertAndAppend(asciiDocConverter.outputFile, issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, rq.get("customfields"))
                for(int colNum = 0; colNum<allHeaders.size()+1;colNum++) {
                    excelConverter.ws.autoSizeColumn(colNum)
                }
                // Set summary column width slightly wider but fixed size, so it doesn't change with every summary update
                excelConverter.ws.setColumnWidth(4, 25*384)


                excelConverter.wb.write(excelConverter.jiraFos)
            }
        }
    }

    @Deprecated
    protected processLegacyRequests(){
        println(">>>Found legacy Jira requests. Please migrate to the new Jira configuration ASAP. Old config with jql will be removed soon")
        def jiraConfig = configService.getFlatConfigSubTree('jira')
        def resultsFilename = "${jiraConfig.resultsFilename}_legacy.adoc"

        def openIssues = new File(targetFolder, "${resultsFilename}")
        openIssues.write(".Table {Title}\n", 'utf-8')
        openIssues.append("|=== \n")
        openIssues.append("|Key |Priority |Created | Assignee | Summary\n", 'utf-8')
        def legacyJql = jiraConfig.jql.replaceAll('%jiraProject%', jiraConfig.project).replaceAll('%jiraLabel%', jiraConfig.label)
        println ("Results for legacy query '${legacyJql}' will be saved in '${resultsFilename}' file")
        def selectedFields = 'created,resolutiondate,priority,summary,timeoriginalestimate, assignee'
        jiraClient.getIssuesByJql(legacyJql, selectedFields).issues.each { issue ->
            openIssues.append("| ${jiraConfig.api}/browse/${issue.key}[${issue.key}] ", 'utf-8')
            openIssues.append("| ${issue.fields.priority.name} ", 'utf-8')
            openIssues.append("| ${Date.parse(jiraConfig.dateTimeFormatParse, issue.fields.created).format(jiraConfig.dateTimeFormatOutput)} ", 'utf-8')
            openIssues.append("| ${issue.fields.assignee ? issue.fields.assignee.displayName : 'not assigned'}", 'utf-8')
            openIssues.append("| ${issue.fields.summary} ", 'utf-8')
        }
        openIssues.append("|=== \n")
    }
}
