package org.docToolchain.atlassian.jira.converter

import java.util.logging.Logger

class AsciiDocConverter extends IssueConverter {

    private static final Logger LOGGER = Logger.getLogger(AsciiDocConverter.class.getName())

    private static final String EXTENSION = 'adoc'

    AsciiDocConverter(File targetFolder) {
        super(targetFolder)
    }

    @Override
    initialize(String fileName, String allHeaders) {
        String jiraResultsFilename = "${fileName}.${EXTENSION}"
        println("Results will be saved in '${jiraResultsFilename}' file")

        this.outputFile = new File(targetFolder, jiraResultsFilename)
        outputFile.write(".${fileName}\n", 'utf-8')
        outputFile.append("|=== \n")

        // AsciiDoc table headers (custom fields map needs values here)
        outputFile.append("|Key ", 'utf-8')
        allHeaders.split(",").each {field ->
            outputFile.append("|${field.capitalize()} ", 'utf-8')
        }
        outputFile.append("\n", 'utf-8')
    }

    @Override
    def convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Map<String, String> customFields) {
        LOGGER.info("Converting issue '${issue.key}' and append to ${outputFile.getName()}")
        outputFile.append("| ${jiraRoot}/browse/${issue.key}[${issue.key}] ", 'utf-8')
        outputFile.append("| ${issue.fields.priority.name} ", 'utf-8')
        outputFile.append("| ${Date.parse(jiraDateTimeFormatParse, issue.fields.created).format(jiraDateTimeOutput)} ", 'utf-8')
        outputFile.append("| ${issue.fields.resolutiondate ? Date.parse(jiraDateTimeFormatParse, issue.fields.resolutiondate).format(jiraDateTimeOutput) : ''} ", 'utf-8')
        outputFile.append("| ${issue.fields.summary} ", 'utf-8')
        outputFile.append("| ${issue.fields.assignee ? issue.fields.assignee.displayName : 'not assigned'}", 'utf-8')
        outputFile.append("| ${issue.fields.status.name} ", 'utf-8')

        customFields.each { field ->
            def foundCustom = issue.fields.find {it.key == field.key}
            //logger.quiet("Examining issue '${issue.key}' for custom field '${field.key}' has found: '${foundCustom}'")
            outputFile.append("| ${foundCustom ? foundCustom.value : '-'}\n", 'utf-8')
        }
    }
}
