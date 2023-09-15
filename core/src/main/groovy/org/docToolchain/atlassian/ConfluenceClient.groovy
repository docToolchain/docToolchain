package org.docToolchain.atlassian

import groovyx.net.http.EncoderRegistry
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import groovyx.net.http.RESTClient
import org.apache.http.entity.ContentType
import org.docToolchain.configuration.ConfigService

abstract class ConfluenceClient {

    protected final String API_V1_PATH = "/wiki/rest/api/"
    protected final String API_V2_PATH = "/wiki/api/v2/"

    protected final String editorVersion
    protected String baseApiUrl
    Map headers
    RESTClient restClient;

    ConfluenceClient(ConfigService configService) {
        this.baseApiUrl = configService.getConfigProperty('confluence.api')
        this.restClient = new RESTClient(baseApiUrl)
        restClient.encoderRegistry = new EncoderRegistry( charset: 'utf-8' )
        this.headers = ['X-Atlassian-Token':'no-check']
        if(configService.getConfigProperty("confluence.enforceNewEditor")
            && configService.getConfigProperty("confluence.enforceNewEditor").toBoolean() == true){
            println "WARNING: You are using the new editor version v2. This is not yet fully supported by docToolchain."
            this.editorVersion = "v2"
        } else {
            this.editorVersion = "v1"
        }
        if(configService.getConfigProperty('confluence.proxy')){
            def proxy = configService.getConfigProperty('confluence.proxy')
            restClient.setProxy(proxy.host as String, proxy.port as int, proxy.schema  as String?: 'http')
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

    abstract addLabel(pageId, label)

    abstract getAttachment(pageId, filename)

    abstract updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash)

    abstract createAttachment(String pageId, InputStream inputStream, String fileName, String note, String localHash)

    protected uploadAttachment(uri, InputStream inputStream, String fileName, note, localHash) {
        restClient.request(uri, Method.POST, ContentType.MULTIPART_FORM_DATA){ req ->
            def multiPartContent = MultipartEntityBuilder.create()
            .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            .addBinaryBody("file", inputStream, ContentType.APPLICATION_OCTET_STREAM, fileName)
            .addTextBody("comment", note + "\r\n#" + localHash + "#")
            .build()
            req.setEntity(multiPartContent)
            headers.each { key, value ->
                req.addHeader(key, value)
            }
        }
    }

    abstract fetchPagesBySpaceKey(String spaceKey, Integer offset, Integer pageLimit)

    abstract fetchPagesByAncestorId(String pageId, Integer offset, Integer pageLimit)

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
                    println error.response.data.message
                    println "please check the ancestorId in your config file"
                    throw new Exception("Parent does not exist")
                    break
                default:
                    println error.response.data
            }
            null
        }
    }
}
