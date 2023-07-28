package org.doctoolchain.integration.atlassian

import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import groovyx.net.http.EncoderRegistry
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.ContentType
import groovyx.net.http.Method

abstract class ConfluenceClient {

    private final String editorVersion
    protected String baseApiUrl
    Map headers = ['X-Atlassian-Token':'no-check']
    RESTClient restClient;

    ConfluenceClient(baseApiUrl, editorVersion) {
        this.baseApiUrl = baseApiUrl
        this.restClient = new RESTClient(baseApiUrl)
        this.editorVersion = editorVersion
        restClient.encoderRegistry = new EncoderRegistry( charset: 'utf-8' )
    }

    def addHeader(key, value) {
        this.headers.put(key, value)
    }

    def setProxy(host, port, schema) {
        restClient.setProxy(host, port, schema)
    }

    abstract addLabel(pageId, label)

    abstract getAttachment(pageId, filename)

    abstract updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash)

    abstract createAttachment(String pageId, InputStream inputStream, String fileName, String note, String localHash)

    def uploadAttachment(uri, InputStream inputStream, String fileName, note, localHash) {
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

    protected abstract fetchPageIdByName(String name, String spaceKey)

    protected abstract doUpdatePageRequest(String pageId, Map requestBody)

    protected abstract doCreatePageRequest(Map requestBody)

    def getDefaultModifyPageRequestBody(String title, String confluenceSpaceKey, Object localPage, String parentId) {
        def requestBody = [
                type : 'page',
                title: title,
                metadata: [
                        properties: [
                                editor: [
                                        value: editorVersion
                                ],
                                "content-appearance-draft": [
                                        value: "full-width"
                                ],
                                "content-appearance-published": [
                                        value: "full-width"
                                ]
                        ]
                ],
                space: [
                        key: confluenceSpaceKey
                ],
                body : [
                        storage: [
                                value         : localPage,
                                representation: 'storage'
                        ]
                ]
        ]
        if (parentId) {
            requestBody.ancestors = [
                    [ type: 'page', id: parentId]
            ]
        }
        requestBody
    }

    def updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId = null){
        def requestBody = getDefaultModifyPageRequestBody(title, confluenceSpaceKey, localPage, parentId)
        requestBody.id      = pageId
        requestBody.version = [number: pageVersion, message: pageVersionComment ?: '']
        trythis {
            doUpdatePageRequest(pageId, requestBody)
        }
    }

    def createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId = null){
        def requestBody = getDefaultModifyPageRequestBody(title, confluenceSpaceKey, localPage, parentId)
        requestBody.version = [message: pageVersionComment ?: '']
        trythis {
            doCreatePageRequest(requestBody)
        }
    }

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
