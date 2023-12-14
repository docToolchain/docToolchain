package org.docToolchain.atlassian.jira.converter

class AsciiDocConverter extends IssueConverter {

    @Override
    File prepareOutputFile(String fileName, File targetFolder, String allHeaders) {
        def extension = 'adoc'
        def jiraResultsFilename = "${fileName}.${extension}"
        println("Results will be saved in '${fileName}.${extension}' file")

        def jiraDataAsciidoc = new File(targetFolder, jiraResultsFilename)
        jiraDataAsciidoc.write(".${fileName}\n", 'utf-8')
        jiraDataAsciidoc.append("|=== \n")

        // AsciiDoc table headers (custom fields map needs values here)
        jiraDataAsciidoc.append("|Key ", 'utf-8')
        allHeaders.split(",").each {field ->
            jiraDataAsciidoc.append("|${field.capitalize()} ", 'utf-8')
        }
        jiraDataAsciidoc.append("\n", 'utf-8')
        this.outputFile = jiraDataAsciidoc
    }

    @Override
    def convertAndAppend(File jiraDataAsciidoc, issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Map<String, String> customFields) {
        jiraDataAsciidoc.append("| ${jiraRoot}/browse/${issue.key}[${issue.key}] ", 'utf-8')
        jiraDataAsciidoc.append("| ${issue.fields.priority.name} ", 'utf-8')
        jiraDataAsciidoc.append("| ${Date.parse(jiraDateTimeFormatParse, issue.fields.created).format(jiraDateTimeOutput)} ", 'utf-8')
        jiraDataAsciidoc.append("| ${issue.fields.resolutiondate ? Date.parse(jiraDateTimeFormatParse, issue.fields.resolutiondate).format(jiraDateTimeOutput) : ''} ", 'utf-8')
        jiraDataAsciidoc.append("| ${issue.fields.summary} ", 'utf-8')
        jiraDataAsciidoc.append("| ${issue.fields.assignee ? issue.fields.assignee.displayName : 'not assigned'}", 'utf-8')
        jiraDataAsciidoc.append("| ${issue.fields.status.name} ", 'utf-8')

        customFields.each { field ->
            def foundCustom = issue.fields.find {it.key == field.key}
            //logger.quiet("Examining issue '${issue.key}' for custom field '${field.key}' has found: '${foundCustom}'")
            jiraDataAsciidoc.append("| ${foundCustom ? foundCustom.value : '-'}\n", 'utf-8')
        }
    }
}
