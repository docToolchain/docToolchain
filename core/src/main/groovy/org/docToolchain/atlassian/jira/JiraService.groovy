package org.docToolchain.atlassian.jira

import org.docToolchain.atlassian.jira.clients.JiraClient
import org.docToolchain.atlassian.jira.clients.JiraServerClient
import org.docToolchain.atlassian.jira.converter.AsciiDocConverter
import org.docToolchain.atlassian.jira.converter.ExcelConverter
import org.docToolchain.atlassian.jira.converter.IssueConverter
import org.docToolchain.atlassian.jira.legacy.LegacyJiraProcessor
import org.docToolchain.configuration.ConfigService

import java.util.logging.Logger

class JiraService {

    private static final Logger LOGGER = Logger.getLogger(JiraService.class.getName())

    private final String DEFAULT_FIELDS = 'priority,created,resolutiondate,summary,assignee,status'

    final File targetFolder
    protected JiraClient jiraClient
    private jiraConfig
    private final List<IssueConverter> converters = []

    JiraService(ConfigService configService) {
        this.jiraClient = new JiraServerClient(configService)
        this.jiraConfig = configService.getFlatConfigSubTree('jira')
        String taskSubFolderName = jiraConfig.resultsFolder
        //TODO targetDir is currently dependend on Gradle
        String targetDir = configService.getConfigProperty("targetDir")
        this.targetFolder = new File(targetDir + File.separator + taskSubFolderName)
        registerConverters()
    }

    def exportJira(){
        if(jiraConfig.iql){
            LegacyJiraProcessor.processLegacyRequests(targetFolder, jiraConfig, jiraClient)
        }
        HashSet jiraExports = jiraConfig.exports as HashSet ?: []
        transformAndMergeLegacyConfiguration(jiraExports)
        jiraExports.each {export ->
                process(export)
            }
    }

    private void registerConverters(){
        if(jiraConfig.saveAsciidoc){
            converters.add(new AsciiDocConverter(targetFolder))
        } else {
            println("Set saveAsciidoc=true your config to save results in AsciiDoc file")
        }
        if(jiraConfig.saveExcel){
            converters.add(new ExcelConverter(targetFolder))
        } else {
            println("Set saveExcel=true your config to save results in Excel file")
        }
    }

    private void transformAndMergeLegacyConfiguration(HashSet jiraExports){
        HashSet legacyConfig = jiraConfig.requests as HashSet
        if(legacyConfig){
            LOGGER.info("Merging legacy Jira config with new Jira config")
            jiraExports.addAll(LegacyJiraProcessor.transformLegacyConfiguration(legacyConfig))
        }
    }

    protected process(export) {
        String jiraRoot = jiraConfig.api
        String jiraProject = jiraConfig.project
        String jiraLabel = jiraConfig.label
        String jiraDateTimeFormatParse = jiraConfig.dateTimeFormatParse
        String jiraDateTimeOutput = jiraConfig.dateTimeFormatOutput
        String jql = export.get("jql")
        String targetFileName = export.get("filename")
        Map<String,String> customFields = export.get("customfields")

        LOGGER.fine("Request to Jira API for '${targetFileName}' with query: '${jql}'")

        def allHeaders = "${DEFAULT_FIELDS},${customFields.values().join(",")}"
        def allFieldIds = "${DEFAULT_FIELDS},${customFields.keySet().join(",")}"
        LOGGER.finer("Preparing headers for default & custom fields: ${allHeaders}")
        LOGGER.finer("Preparing field IDs for default & custom fields: ${allFieldIds}")
        converters.each { converter ->
            converter.initialize(targetFileName, allHeaders)
        }

        jiraClient.getIssuesByJql(
            jql.replaceAll('%jiraProject%', jiraProject).replaceAll('%jiraLabel%', jiraLabel),
            allFieldIds
        ).issues.each { issue ->
            converters.each { converter ->
                converter.convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, customFields)
            }
        }
    }
}
