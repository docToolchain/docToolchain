package org.docToolchain.atlassian.clients

import groovy.json.JsonBuilder
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.URIBuilder
import org.docToolchain.configuration.ConfigService


class ConfluenceClientV1 extends ConfluenceClient {

    ConfluenceClientV1(ConfigService configService) {
        super(configService)
    }

    @Override
    def verifyCredentials() {
        HttpRequest get = new HttpGet(API_V1_PATH + '/user/current')
        return callApiAndFailIfNot20x(get)
    }

    @Override
    def addLabel(pageId, label) {
        HttpRequest post = new HttpPost(API_V1_PATH + '/content/' + pageId + "/label")
        post.setHeader('Content-Type', 'application/json')
        // TODO test if this works
        post.setEntity(new StringEntity(new JsonBuilder([label]).toPrettyString()))
        return callApiAndFailIfNot20x(post)
    }

    @Override
    def getAttachment(Object pageId, Object fileName) {
        URI uri = new URIBuilder(API_V1_PATH + '/content/' + pageId + '/child/attachment')
            .addParameter('filename', fileName as String)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }

    @Override
    def updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash) {
        def uri = API_V1_PATH + '/content/' + pageId + '/child/attachment/' + attachmentId + '/data'
        uploadAttachment(uri, inputStream, fileName, note, localHash)
    }

    @Override
    def createAttachment(String pageId, InputStream inputStream, String fileName, String note, String localHash) {
        def uri = API_V1_PATH + '/content/' + pageId + '/child/attachment'
        println("Uploading attachment to ${uri}")
        uploadAttachment(uri, inputStream, fileName, note, localHash)
    }

    @Override
    def attachmentHasChanged(Object attachment, Object localHash) {
        def remoteHash = attachment.results[0].extensions.comment.replaceAll("(?sm).*#([^#]+)#.*",'$1')
        return remoteHash!=localHash
    }

    @Override
    def fetchPagesBySpaceKey(String spaceKey, Integer pageLimit) {
        def allPages = [:]
        Boolean morePages = true
        Integer offset = 0
        while (morePages) {
            def query = List.of(
                new BasicNameValuePair('type', 'page'),
                new BasicNameValuePair('spaceKey', spaceKey),
                new BasicNameValuePair('expand', 'ancestors'),
                new BasicNameValuePair('limit', pageLimit.toString()),
                new BasicNameValuePair('start', offset.toString())
            )
            URI uri = new URIBuilder(API_V1_PATH + '/content')
                .addParameters(query)
                .build()
            HttpRequest get = new HttpGet(uri)
            def results = callApiAndFailIfNot20x(get)
            results = results.results ?: []
            if (results.empty) {
                morePages = false
            } else {
                offset += results.size()
            }
            results.inject(allPages) { Map acc, Map match ->
                //unique page names in confluence, so we can get away with indexing by title
                def ancestors = match.ancestors.collect { it.id }
                acc[match.title.toLowerCase()] = [
                    title   : match.title,
                    id      : match.id,
                    parentId: ancestors.isEmpty() ? null : ancestors.last()
                ]
                acc
            }
        }
        return allPages
    }

    @Override
    def fetchPagesByAncestorId(List<String> pageIds, Integer pageLimit) {
        def allPages = [:]

        Integer offset = 0
        def ids = []
        String pageId = pageIds.remove(0)
        Boolean morePages = true
        while (morePages) {
            def query = List.of(
                new BasicNameValuePair('type', 'page'),
                new BasicNameValuePair('limit', pageLimit.toString()),
                new BasicNameValuePair('start', offset.toString())
            )
            URI uri = new URIBuilder(API_V1_PATH + "/content/${pageId}/child/page")
                .addParameters(query)
                .build()
            HttpRequest get = new HttpGet(uri)
            def results = callApiAndFailIfNot20x(get)
            results = results.results ?: []

            results.inject(allPages) { Map acc, Map match ->
                //unique page names in confluence, so we can get away with indexing by title
                ids.add(match.id)
                acc[match.title.toLowerCase()] = [
                    title   : match.title,
                    id      : match.id,
                    parentId: pageId
                ]
                acc
            }

            if (results.empty && ids.isEmpty()) {
                if(pageIds.isEmpty()) {
                    morePages = false
                } else {
                    pageId = pageIds.remove(0)
                }
            } else if (!results.empty) {
                offset += results.size()
            } else {
                offset = 0
                pageId = ids.remove(0)
            }
        }
        return allPages
    }

    @Override
    def fetchPageByPageId(String id) {
        def query = List.of(
            new BasicNameValuePair('expand', 'body.storage,version,ancestors')
        )
        URI uri = new URIBuilder(API_V1_PATH + "/content/${id}")
            .addParameters(query)
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }

    @Override
    protected fetchPageIdByName(String name, String spaceKey) {
        def query = List.of(
            new BasicNameValuePair('title', name),
            new BasicNameValuePair('spaceKey', spaceKey)
        )
        URI uri = new URIBuilder(API_V1_PATH + "/content")
            .addParameters(query)
            .build()
        HttpRequest get = new HttpGet(uri)
        def results = callApiAndFailIfNot20x(get)
        return results?.results.get(0)?.id
    }

    @Override
    def updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId = null){
        def requestBody = getDefaultModifyPageRequestBody(title, confluenceSpaceKey, localPage, parentId)
        requestBody.id      = pageId
        requestBody.version = [number: pageVersion, message: pageVersionComment ?: '']
        HttpPut put = new HttpPut(API_V1_PATH + '/content/' + pageId)
        put.setHeader('Content-Type', 'application/json')
        put.setEntity(new StringEntity(new JsonBuilder(requestBody).toPrettyString()))
        return callApiAndFailIfNot20x(put)
    }

    @Override
    def createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId = null){
        def requestBody = getDefaultModifyPageRequestBody(title, confluenceSpaceKey, localPage, parentId)
        requestBody.version = [message: pageVersionComment ?: '']
        HttpPost post = new HttpPost(API_V1_PATH + '/content')
        post.setHeader('Content-Type', 'application/json')
        post.setEntity(new StringEntity(new JsonBuilder(requestBody).toPrettyString()))
        return callApiAndFailIfNot20x(post)
    }

    protected getDefaultModifyPageRequestBody(String title, String confluenceSpaceKey, Object localPage, String parentId) {
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
}
