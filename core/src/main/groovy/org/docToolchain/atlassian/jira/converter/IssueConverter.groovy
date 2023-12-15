package org.docToolchain.atlassian.jira.converter

abstract class IssueConverter {

    File outputFile
    File targetFolder

    IssueConverter(File targetFolder) {
        this.targetFolder = targetFolder
    }

    abstract def initialize(String fileName, String columns)
    abstract def initialize(String fileName, String columns, String caption)
    abstract def convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Map<String, String> customFields)
}
