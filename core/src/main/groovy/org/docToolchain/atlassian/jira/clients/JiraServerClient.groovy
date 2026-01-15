package org.docToolchain.atlassian.jira.clients

import org.docToolchain.configuration.ConfigService
import java.net.URI
import org.apache.hc.core5.net.URIBuilder
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.client5.http.classic.methods.HttpGet

class JiraServerClient extends JiraClient {

    /** Legacy relative path (always present) */
    protected final String API_PATH

    /** Absolute URL, only when jira.api includes scheme+host */
    protected final String API_URL

    JiraServerClient(ConfigService configService) {
        super(configService)
        String base = (configService.getConfigProperty("jira.api") as String)?.trim()
        def built = buildApiFromBase(base)
        this.API_PATH = built.apiPath
        this.API_URL  = built.apiUrl
    }

    private static Map<String, String> buildApiFromBase(String base) {
        if (!base) return [apiPath: "/rest", apiUrl: null]

        URI uri
        try {
            uri = URI.create(base.trim())
        } catch (IllegalArgumentException ignored) {
            String ctx = normalizeContext(base)
            return [apiPath: (ctx ?: "") + "/rest", apiUrl: null]
        }

        String ctx = normalizeContext(uri.path ?: "")
        if (uri.scheme == null) {
            return [apiPath: (ctx ?: "") + "/rest", apiUrl: null]
        }

        String origin = buildOrigin(uri)
        String path = (ctx ?: "") + "/rest"
        return [apiPath: path, apiUrl: origin + path]
    }

    private static String normalizeContext(String ctx) {
        if (!ctx) return ""
        String c = ctx.trim()
        if (!c.startsWith("/")) c = "/" + c
        if (c.endsWith("/")) c = c[0..-2]
        return c
    }

    private static String buildOrigin(URI u) {
        String port = (u.port > -1) ? ":${u.port}" : ""
        return "${u.scheme}://${u.host}${port}"
    }

    String api(String route) {
        return joinPath(API_PATH, route)
    }

    String apiAbs(String route) {
        String base = API_URL ?: API_PATH
        return joinPath(base, route)
    }

    private static String joinPath(String base, String route) {
        String b = (base ?: "").trim()
        String r = (route ?: "").trim()
        if (r && !r.startsWith("/")) r = "/" + r
        if (b.endsWith("/")) b = b[0..-2]
        return b + r
    }

    // ---------------- Original REST methods adapted ----------------

    @Override
    def getIssuesByJql(String jql, String selectedFields) {
        println("getIssuesByJql: " + apiAbs("/api/2/search"))
        URI uri = new URIBuilder(apiAbs("/api/2/search"))
            .addParameter("jql", jql)
            .addParameter("maxResults", "1000")
            .addParameter("fields", selectedFields)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }

    @Override
    def getSprintsByBoardAndState(String boardId, String sprintState) {
        URI uri = new URIBuilder(apiAbs("/agile/latest/board/${boardId}/sprint"))
            .addParameter("state", sprintState)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }

    @Override
    def getIssuesForSprint(String boardId, Integer sprintId, String issueStatus, String ticketFields) {
        URI uri = new URIBuilder(apiAbs("/agile/latest/board/${boardId}/sprint/${sprintId}/issue"))
            .addParameter("jql", "status in (${issueStatus}) ORDER BY type DESC, status ASC")
            .addParameter("maxResults", "1000")
            .addParameter("fields", ticketFields)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }
}
