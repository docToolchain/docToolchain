package org.docToolchain.atlassian.jira.clients

import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.net.URIBuilder
import org.docToolchain.configuration.ConfigService

class JiraServerClient extends JiraClient {

    protected final String API_PATH = "/rest"

    JiraServerClient(ConfigService configService) {
        super(configService)
    }

    @Override
    def getIssuesByJql(String jql, String selectedFields){
        URI uri = new URIBuilder(API_PATH + '/api/2/search')
            .addParameter('jql', jql)
            .addParameter('maxResults', '1000')
            .addParameter('fields', selectedFields)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }

    @Override
    def getSprintsByBoardAndState(String boardId, String sprintState) {
        URI uri = new URIBuilder(API_PATH + "/agile/latest/board/${boardId}/sprint")
            .addParameter('state', sprintState)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }

    @Override
    def getIssuesForSprint(String boardId, String sprintId, String issueStatus, String ticketFields) {
        URI uri = new URIBuilder(API_PATH + "/agile/latest/board/${boardId}/sprint/${sprintId}/issue")
            .addParameter('jql', "status in (${issueStatus}) ORDER BY type DESC, status ASC")
            .addParameter('maxResults', '1000')
            .addParameter('fields', ticketFields)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }
}
