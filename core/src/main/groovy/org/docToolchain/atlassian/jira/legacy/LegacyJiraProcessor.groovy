package org.docToolchain.atlassian.jira.legacy

import org.docToolchain.atlassian.jira.clients.JiraClient

import java.util.logging.Logger

class LegacyJiraProcessor {

    private static final Logger LOGGER = Logger.getLogger(LegacyJiraProcessor.class.getName())

    final static private String SELECTED_FIELDS = 'created,resolutiondate,priority,summary,timeoriginalestimate, assignee'

    @Deprecated
    def static transformLegacyConfiguration(HashSet jiraRequests){
        HashSet transformedRequests = []
            jiraRequests.each {rq ->
                transformedRequests.add([
                    filename: rq.filename,
                    jql: rq.jql,
                    customfields: rq.customfields
                ])
            }
        LOGGER.info("Tranformed ${transformedRequests.size()} legacy Jira config items")
        return transformedRequests
    }

    @Deprecated
    def static processLegacyRequests(File targetFolder, jiraConfig, JiraClient jiraClient){
        println("Found legacy Jira requests. Please migrate to the new Jira configuration ASAP. Old config with jql will be removed soon")
        String resultsFilename = "${jiraConfig.resultsFilename}_legacy.adoc"

        File outputFile = new File(targetFolder, "${resultsFilename}")
        outputFile.write(".Table {Title}\n", 'utf-8')
        outputFile.append("|=== \n")
        outputFile.append("|Key |Priority |Created | Assignee | Summary\n", 'utf-8')
        def legacyJql = jiraConfig.jql.replaceAll('%jiraProject%', jiraConfig.project).replaceAll('%jiraLabel%', jiraConfig.label)
        println("Results for legacy query '${legacyJql}' will be saved in '${resultsFilename}' file")
        jiraClient.getIssuesByJql(legacyJql, SELECTED_FIELDS).issues.each { issue ->
            outputFile.append("| ${jiraConfig.api}/browse/${issue.key}[${issue.key}] ", 'utf-8')
            outputFile.append("| ${issue.fields.priority.name} ", 'utf-8')
            outputFile.append("| ${Date.parse(jiraConfig.dateTimeFormatParse, issue.fields.created).format(jiraConfig.dateTimeFormatOutput)} ", 'utf-8')
            outputFile.append("| ${issue.fields.assignee ? issue.fields.assignee.displayName : 'not assigned'}", 'utf-8')
            outputFile.append("| ${issue.fields.summary} ", 'utf-8')
        }
        outputFile.append("|=== \n")
    }
}
