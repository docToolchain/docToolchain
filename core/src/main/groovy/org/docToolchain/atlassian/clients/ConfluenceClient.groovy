package org.docToolchain.atlassian.clients

import groovyx.net.http.EncoderRegistry
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import org.docToolchain.configuration.ConfigService

abstract class ConfluenceClient {

    protected final String API_V1_DEFAULT_PATH = "/wiki/rest/api/"
    protected final String API_V2_DEFAULT_PATH = "/wiki/api/v2/"

    protected final String editorVersion
    protected String baseApiUrl
    Map headers
    RESTClient restClient
    //TODO this is a workaround for the fact that the RESTClient does not support mulitpart/form-data from Groovy 3.0.0 on
    Map proxyConfig = [:]

    ConfluenceClient(ConfigService configService) {
        this.baseApiUrl = configService.getConfigProperty('confluence.api')
        this.restClient = new RESTClient(baseApiUrl)
        restClient.setEncoderRegistry(new EncoderRegistry( charset: 'utf-8' ))
        this.headers = ['X-Atlassian-Token':'no-check']
        this.editorVersion = determineEditorVersion(configService)
        if(configService.getFlatConfigSubTree('confluence.proxy')){
            def proxy = configService.getFlatConfigSubTree('confluence.proxy')
            //TODO this is a workaround for the fact that the RESTClient does not support mulitpart/form-data from Groovy 3.0.0 on
            proxyConfig.put('host', proxy.host as String)
            proxyConfig.put('port', proxy.port as Integer)
            proxyConfig.put('schema', proxy.schema  as String?: 'http')
            //END WORKAROUND
            restClient.setProxy(proxy.host as String, proxy.port as Integer, proxy.schema  as String?: 'http')
        }
        if(configService.getConfigProperty('confluence.bearerToken')){
            headers.put('Authorization', 'Bearer ' + configService.getConfigProperty('confluence.bearerToken'))
            println 'Start using bearer auth'
        } else {
            headers.put('Authorization', 'Basic ' + configService.getConfigProperty('confluence.credentials'))
            //Add api key and value to REST API request header if configured - required for authentification.
            if (configService.getConfigProperty('confluence.apikey')){
                headers.put('keyid', configService.getConfigProperty('confluence.apikey'))
            }
        }
    }

    def addHeader(key, value) {
        this.headers.put(key, value)
    }

    abstract verifyCredentials()

    abstract addLabel(pageId, label)

    abstract getAttachment(pageId, filename)

    abstract updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash)

    abstract createAttachment(String pageId, InputStream inputStream, String fileName, String note, String localHash)

    protected uploadAttachment(uri, InputStream inputStream, String fileName, note, localHash) {
        def builder = new HTTPBuilder(baseApiUrl + uri)
        //TODO this is a workaround for the fact that the RESTClient does not support mulitpart/form-data from Groovy 3.0.0 on
        if (!proxyConfig.isEmpty()) {
            builder.setProxy(proxyConfig.get("host") as String, proxyConfig.get("port") as Integer, proxyConfig.get("schema") as String)
        }
        //END WORKAROUND
        builder.request(Method.POST) { req ->
            requestContentType: "multipart/form-data"
            MultipartEntity multiPartContent = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE)
            // Adding Multi-part file parameter "file"
            multiPartContent.addPart("file", new InputStreamBody(inputStream, fileName))
            // Adding another string parameter "comment"
            multiPartContent.addPart("comment", new StringBody(note + "\r\n#" + localHash + "#"))
            req.setEntity(multiPartContent)
            this.headers.each { key, value ->
                req.addHeader(key, value)
            }
        }
    }

    abstract fetchPagesBySpaceKey(String spaceKey, Integer pageLimit)

    abstract fetchPagesByAncestorId(List<String> pageIds, Integer pageLimit)

    abstract fetchPageByPageId(String id)

    abstract updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId)

    abstract createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId)

    protected abstract fetchPageIdByName(String name, String spaceKey)

    def retrieveFullPageById(String pageId) {
        trythis {
            fetchPageByPageId(pageId).data
        } ?: [:]
    }

    def retrievePageIdByName(String name, String spaceKey){
        trythis {
            fetchPageIdByName(name, spaceKey)
        } ?: null
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
