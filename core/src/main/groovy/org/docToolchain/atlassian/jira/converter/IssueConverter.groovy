package org.docToolchain.atlassian.jira.converter

abstract class IssueConverter {

    File outputFile
    File targetFolder

    IssueConverter(File targetFolder) {
        this.targetFolder = targetFolder
    }

    abstract def initialize(String fileName, String allHeaders)
    abstract def convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Map<String, String> customFields)
}
