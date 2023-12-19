package org.docToolchain.atlassian.jira.converter

abstract class IssueConverter {

    File outputFile
    File targetFolder

    IssueConverter(File targetFolder) {
        this.targetFolder = targetFolder
    }

    abstract def initialize(String fileName, String columns)
    abstract def initialize(String fileName, String columns, String caption)
    abstract def convertAndAppend(issue, jiraRoot, jiraDateTimeFormatParse, jiraDateTimeOutput, Boolean showAssignee, Boolean showTicketStatus, Boolean showTicketType, Boolean showPriority, Boolean showCreatedDate, Boolean showResolvedDate, Map<String, String> customFields)
    abstract def finalizeOutput()
}
