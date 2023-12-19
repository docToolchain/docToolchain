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

    private final String DEFAULT_FIELDS = 'key,priority,created,resolutiondate,summary,assignee,status,issuetype'

    final File outputFolder
    protected File targetFolder
    protected JiraClient jiraClient
    private jiraConfig
    private changeLogConfig
    private final List<IssueConverter> converters = []

    JiraService(ConfigService configService) {
        this.jiraClient = new JiraServerClient(configService)
        this.jiraConfig = configService.getFlatConfigSubTree('jira')
        this.changeLogConfig = configService.getFlatConfigSubTree('sprintChangelog')
        //TODO targetDir is currently dependend on Gradle
        String targetDir = configService.getConfigProperty("targetDir")
        this.outputFolder = new File(targetDir)
        if (!outputFolder.exists()){
            outputFolder.mkdirs()
        }
    }

    def exportJira(){
        //TODO this is currently a workaround, this will change in the future
        String taskSubFolderName = jiraConfig.resultsFolder
        this.targetFolder = new File(outputFolder.getAbsolutePath() + File.separator + taskSubFolderName)
        if (!targetFolder.exists()){
            targetFolder.mkdirs()
        }
        registerConverters()
        // end of workaround
        if(jiraConfig.iql){
            LegacyJiraProcessor.processLegacyRequests(targetFolder, jiraConfig, jiraClient)
        }
        HashSet jiraExports = jiraConfig.exports as HashSet ?: []
        transformAndMergeLegacyConfiguration(jiraExports)
        println("Jira exports: ${jiraExports}")
        jiraExports.each {export ->
                process(export)
            }
    }

    def exportJiraSprintChangelog(){
        def jiraRoot = jiraConfig.api
        //TODO this is currently a workaround, this will change in the future
        String taskSubFolderName = changeLogConfig.resultsFolder
        this.targetFolder = new File(outputFolder.getAbsolutePath() + File.separator + taskSubFolderName)
        if (!targetFolder.exists()){
            targetFolder.mkdirs()
        }
        registerConverters()
        // end of workaround
        def sprintState = changeLogConfig.sprintState
        def ticketStatusForReleaseNotes = changeLogConfig.ticketStatus
        def sprintBoardId = changeLogConfig.sprintBoardId
        def showAssignee = changeLogConfig.showAssignee
        def showTicketStatus = changeLogConfig.showTicketStatus
        def showTicketType = changeLogConfig.showTicketType
        def sprintName = changeLogConfig.sprintName
        def allSprintsFilename = changeLogConfig.allSprintsFilename

        LOGGER.info("\n==========================\nJira Release notes config\n==========================")
        LOGGER.info("Spring Board ID: ${sprintBoardId}")
        LOGGER.info("Show assignees: ${showAssignee}. Show ticket status: ${showTicketStatus}. Show ticket type: ${showTicketType}")
        LOGGER.info("Filtering for sprints with configured state: '${sprintState}'")
        LOGGER.info("Filtering for issues with configured statuses: ${ticketStatusForReleaseNotes}")
        LOGGER.info("Attempt to generate release notes for sprint with a name: '${sprintName}'")
        LOGGER.info("Filename used for all sprints: '${allSprintsFilename}'")
        def columns = DEFAULT_FIELDS.split(',').collect()
        //TODO this is currently a workaround, this will change in the future
        columns = columns.minus('priority').minus('created').minus('resolutiondate')
        // end of workaround
        if (!showAssignee) { columns = columns.minus('assignee')}
        if (!showTicketStatus) { columns = columns.minus('status')}
        if (!showTicketType) { columns = columns.minus('issuetype')}
        LOGGER.info("Release notes will contain following info: ${columns}")

        ((ExcelConverter) converters.find() { it instanceof ExcelConverter })?.prepareWorkbook(allSprintsFilename as String)


        def allMatchedSprints = jiraClient.getSprintsByBoardAndState(sprintBoardId, sprintState).values
        def foundExactSprint = allMatchedSprints.any {it.name == sprintName}
        LOGGER.info("All sprints that matched configuration: ${allMatchedSprints.size()}")

        def sprintsForChangelog = foundExactSprint ? allMatchedSprints.stream().filter() {it.name == sprintName} : allMatchedSprints
        LOGGER.info("Found exact Sprint with name '${sprintName}': ${foundExactSprint}.")
        sprintsForChangelog.each { sprint ->
            LOGGER.finer("\nSprint: $sprint.name [id: $sprint.id] state <$sprint.state>")
            converters.find() { it instanceof AsciiDocConverter }?.initialize(sprint.name.replaceAll(" ", "_") as String, columns.join(','), ".Table ${sprint.name} Changelog\n")
            converters.find() { it instanceof ExcelConverter }?.initialize(sprint.name as String, columns.join(','))
            jiraClient.getIssuesForSprint(sprintBoardId, sprint.id, ticketStatusForReleaseNotes, columns.join(',')).issues.each { issue ->
                converters.each { converter ->
                    converter.convertAndAppend(issue, jiraRoot, jiraConfig.dateTimeFormatParse, jiraConfig.dateTimeFormatOutput, showAssignee, showTicketStatus, showTicketType, false, false, false, [:])
                }
            }
            converters.each { converter ->
                converter.finalizeOutput()
            }
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

        def columns = "${DEFAULT_FIELDS},${customFields.values().join(",")}"
        def allFieldIds = "${DEFAULT_FIELDS},${customFields.keySet().join(",")}"
        LOGGER.finer("Preparing headers for default & custom fields: ${columns}")
        LOGGER.finer("Preparing field IDs for default & custom fields: ${allFieldIds}")
        converters.each { converter ->
            converter.initialize(targetFileName, columns)
        }
        jiraClient.getIssuesByJql(
            jql.replaceAll('%jiraProject%', jiraProject).replaceAll('%jiraLabel%', jiraLabel),
            allFieldIds
        ).issues.each { issue ->
            converters.each { converter ->
                converter.convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, true, true, true, true, true, true, customFields)
            }
        }
        converters.each { converter ->
            converter.finalizeOutput()
        }
    }
}
