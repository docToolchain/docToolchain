package org.docToolchain.atlassian.clients

import groovy.json.JsonSlurper
import org.apache.hc.client5.http.HttpResponseException
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode
import org.apache.hc.client5.http.entity.mime.InputStreamBody
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.entity.mime.StringBody
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.net.URIBuilder
import org.docToolchain.configuration.ConfigService

abstract class ConfluenceClient {

    private final String API_DEFAULT_CONTEXT = "wiki"
    private final String API_V1_IDENTIFIER = "/rest/api"
    private final String API_V2_IDENTIFIER = "/api/v2"
    protected final String API_V1_PATH
    protected final String API_V2_PATH
    protected final String editorVersion

    protected String baseApiUrl
    protected Set<Header> headers
    private String apiContext

    HttpClientBuilder httpClientBuilder
    HttpHost targetHost

    ConfluenceClient(ConfigService configService) {
        String apiConfigItem = configService.getConfigProperty('confluence.api')
        this.targetHost = buildApiBaseUrlAndSetAPIContextFromConfigItem(apiConfigItem)
        this.API_V1_PATH = apiContext + API_V1_IDENTIFIER
        this.API_V2_PATH = apiContext + API_V2_IDENTIFIER
        this.httpClientBuilder = HttpClientBuilder.create()
        this.headers = new HashSet<>([
            new BasicHeader('X-Atlassian-Token', 'no-check'),
            new BasicHeader('Content-Type', 'application/json;charset=utf-8')
        ])
        this.editorVersion = determineEditorVersion(configService)
        if(configService.getFlatConfigSubTree('confluence.proxy')){
            def proxy = configService.getFlatConfigSubTree('confluence.proxy')
            httpClientBuilder.setProxy(new HttpHost(proxy.schema  as String?: 'http', proxy.host as String, proxy.port as Integer))
        }
        if(configService.getConfigProperty('confluence.bearerToken')){
            headers.add(new BasicHeader('Authorization', 'Bearer ' + configService.getConfigProperty('confluence.bearerToken')))
            println 'Start using bearer auth'
        } else {
            headers.add(new BasicHeader('Authorization', 'Basic ' + configService.getConfigProperty('confluence.credentials')))
            //Add api key and value to REST API request header if configured - required for authentification.
            if (configService.getConfigProperty('confluence.apikey')){
                headers.add(new BasicHeader('keyid', configService.getConfigProperty('confluence.apikey') as String))
            }
        }
        httpClientBuilder.setDefaultHeaders(headers)
    }

    protected HttpHost buildApiBaseUrlAndSetAPIContextFromConfigItem(String configItem) {
        URIBuilder builder = new URIBuilder(configItem)
        HttpHost targetHost = new HttpHost(builder.getScheme(), builder.getHost(), builder.getPort())
        String apiContext = determineApiContext(builder.getPath())
        if(!apiContext.isEmpty()){
            setAPIContext("/" + apiContext)
        } else {
            setAPIContext("")
        }
        return targetHost
    }

    private setAPIContext(String apiContext) {
        this.apiContext = apiContext
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
       //TODO we could implement rate limiting here
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            return Optional.ofNullable(httpClient.execute(targetHost, httpRequest, new ConfluenceClientResponseHandler()))
                .map(response -> new JsonSlurper().parseText(response))
                .orElse(null);
        } catch (IOException e) {
            //TODO handle exception
        }
    }

    abstract fetchPagesBySpaceKey(String spaceKey, Integer pageLimit)

    abstract fetchPagesByAncestorId(List<String> pageIds, Integer pageLimit)

    abstract fetchPageByPageId(String id)

    abstract updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId)

    abstract createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId)

    protected abstract fetchPageIdByName(String name, String spaceKey)

    def retrieveFullPageById(String pageId) {
        fetchPageByPageId(pageId) ?: [:]
    }

    def retrievePageIdByName(String name, String spaceKey){
      return fetchPageIdByName(name, spaceKey)
    }

    // for getting better error message from the REST-API
    // LuisMuniz: return the action's result, if successful.
    protected def trythis(Closure action) {
        try {
            action.call()
        } catch (HttpResponseException error) {
            println "something went wrong - got an http response code "+error.response.status+":"
            switch (error.response.status) {
                case '401':
                    println (error.response.data.toString().replaceAll("^.*Reason","Reason"))
                    println "please check your confluence credentials in config file or passed parameters"
                    throw new Exception("missing authentication credentials")
                    break
                case '400':
                    println error.response.data
                    println "please check the ancestorId in your config file"
                    throw new Exception("Parent does not exist")
                    break
                default:
                    println error.response.data
            }
            null
        }
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
