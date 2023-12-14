package org.docToolchain.atlassian.jira.converter

abstract class IssueConverter {

    File outputFile

    abstract def prepareOutputFile(String fileName, File targetFolder, String allHeaders)
    abstract def convertAndAppend(File jiraDataAsciidoc, issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Map<String, String> customFields)
}
