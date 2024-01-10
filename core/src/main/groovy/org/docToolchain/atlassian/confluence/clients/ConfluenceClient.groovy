package org.docToolchain.atlassian.confluence.clients

import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.InputStreamBody
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.entity.mime.StringBody
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.net.URIBuilder
import org.docToolchain.configuration.ConfigService

abstract class ConfluenceClient {

    private final String API_DEFAULT_CONTEXT = "wiki"
    private final String API_V1_IDENTIFIER = "/rest/api"
    private final String API_V2_IDENTIFIER = "/api/v2"
    protected final String API_V1_PATH
    protected final String API_V2_PATH
    protected final String editorVersion
    protected RestClient restClient

    private String apiContext

    ConfluenceClient(ConfigService configService) {
        this.restClient = new RestClient(configService)
        String apiConfigItem = configService.getConfigProperty('confluence.api')
        this.apiContext = constructApiContext(apiConfigItem)
        this.API_V1_PATH = apiContext + API_V1_IDENTIFIER
        this.API_V2_PATH = apiContext + API_V2_IDENTIFIER
        this.editorVersion = determineEditorVersion(configService)
    }

    private String constructApiContext(String configItem) {
        URIBuilder builder = new URIBuilder(configItem)
        String apiContext = determineApiContext(builder.getPath())
        if(!apiContext.isEmpty()){
            return "/" + apiContext
        } else {
            return ""
        }
    }

    private determineApiContext(String apiPath) {
        if(apiPath.length() == 0){
            // no context has been set
            return API_DEFAULT_CONTEXT
        }
        // remove leading slash, remove api versions identifier from path
        apiPath = apiPath
            .substring(1)
            .replace(API_V1_IDENTIFIER, "")
            .replace(API_V2_IDENTIFIER, "")
        // then split by slash to get the context
        String[] pathParts = apiPath.split("/")
        if(pathParts.size() == 1){
            // context has been set
            return pathParts[0]
        }
        // assume that context has been omitted intentionally https://docs.atlassian.com/ConfluenceServer/rest/8.6.1/
        return ""
    }

    abstract verifyCredentials()

    abstract addLabel(pageId, label)

    abstract getAttachment(pageId, filename)

    abstract updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash)

    abstract createAttachment(String pageId, InputStream inputStream, String fileName, String note, String localHash)

    abstract attachmentHasChanged(attachment, localHash)

    protected uploadAttachment(uri, InputStream inputStream, String fileName, note, localHash) {
        HttpPost post = new HttpPost(uri)
        HttpEntity entity = MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.EXTENDED)
            .addPart("file", new InputStreamBody(inputStream, fileName))
            .addPart("comment", new StringBody(note + "\r\n#" + localHash + "#", ContentType.TEXT_PLAIN))
            .build()
        post.setEntity(entity)
        callApiAndFailIfNot20x(post)
    }

    protected callApiAndFailIfNot20x(ClassicHttpRequest httpRequest) {
        return restClient.doRequestAndFailIfNot20x(httpRequest)
    }

    abstract fetchPagesBySpaceKey(String spaceKey, Integer pageLimit)

    abstract fetchPagesByAncestorId(List<String> pageIds, Integer pageLimit)

    abstract fetchPageByPageId(String id)

    abstract deletePage(String id)

    abstract updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId)

    abstract createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId)

    protected abstract fetchPageIdByName(String name, String spaceKey)

    def retrieveFullPageById(String pageId) {
        fetchPageByPageId(pageId) ?: [:]
    }

    def retrievePageIdByName(String name, String spaceKey){
      return fetchPageIdByName(name, spaceKey)
    }

    private String determineEditorVersion(ConfigService configService){
        if(configService.getConfigProperty("confluence.enforceNewEditor")
            && configService.getConfigProperty("confluence.enforceNewEditor").toBoolean() == true){
            println "WARNING: You are using the new editor version v2. This is not yet fully supported by docToolchain."
            return "v2"
        } else {
            return "v1"
        }
    }
}
