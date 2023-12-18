package org.docToolchain.atlassian.jira.converter

import org.docToolchain.atlassian.jira.utils.DateUtil

import java.util.logging.Logger

class AsciiDocConverter extends IssueConverter {

    private static final Logger LOGGER = Logger.getLogger(AsciiDocConverter.class.getName())

    private static final String EXTENSION = 'adoc'

    AsciiDocConverter(File targetFolder) {
        super(targetFolder)
    }

    @Override
    def initialize(String fileName, String columns) {
        initialize(fileName, columns, fileName)
    }

    @Override
    def initialize(String fileName, String columns, String caption) {
        String jiraResultsFilename = "${fileName}.${EXTENSION}"
        println("Results will be saved in '${jiraResultsFilename}' file")

        this.outputFile = new File(targetFolder, jiraResultsFilename)
        outputFile.write(".${caption}\n", 'utf-8')
        outputFile.append("|=== \n")

        // AsciiDoc table headers (custom fields map needs values here)
        columns.split(",").each { field ->
            outputFile.append("|${field.capitalize()} ", 'utf-8')
        }
        outputFile.append("\n", 'utf-8')
    }



    @Override
    def convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Boolean showAssignee, Boolean showTicketStatus, Boolean showTicketType, Map<String, String> customFields) {
        LOGGER.info("Converting issue '${issue.key}' and append to ${outputFile.getName()}")
        outputFile.append("\n", 'utf-8')
        outputFile.append("| ${jiraRoot}/browse/${issue.key}[${issue.key}] ", 'utf-8')
        outputFile.append("| ${issue.fields.priority.name} ", 'utf-8')
        outputFile.append("| ${DateUtil.format(issue.fields.created, jiraDateTimeFormatParse, jiraDateTimeOutput)} ", 'utf-8')
        outputFile.append("| ${jiraDateTimeFormatParse} ", 'utf-8')
        outputFile.append("| ${issue.fields.resolutiondate ? DateUtil.format(issue.fields.resolutiondate, jiraDateTimeFormatParse, jiraDateTimeOutput) : ''} ", 'utf-8')
        outputFile.append("| ${issue.fields.summary} ", 'utf-8')
        if (showAssignee) {
            outputFile.append("| ${issue.fields.assignee ? issue.fields.assignee.displayName : 'not assigned'}", 'utf-8')
        }
        if (showTicketStatus) {
            outputFile.append("| ${issue.fields.status.name} ", 'utf-8')
        }
        if (showTicketType) {
            outputFile.append("| ${issue.fields.issuetype.name} ", 'utf-8')
        }

        customFields.each { field ->
            def foundCustom = issue.fields.find {it.key == field.key}
            //logger.quiet("Examining issue '${issue.key}' for custom field '${field.key}' has found: '${foundCustom}'")
            outputFile.append("| ${foundCustom ? foundCustom.value : '-'}\n", 'utf-8')
        }
    }

    @Override
    def finalizeOutput() {
        outputFile.append("\n|=== \n",'utf-8')
    }
}
