package org.docToolchain.atlassian.clients

import groovy.json.JsonBuilder
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.classic.methods.HttpPut
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.io.entity.StringEntity
import org.apache.hc.core5.net.URIBuilder
import org.docToolchain.configuration.ConfigService

class ConfluenceClientV2 extends ConfluenceClient {

    private final String spaceId

    ConfluenceClientV2(ConfigService configService) {
        super(configService)
        String spaceKey = configService.getConfigProperty('confluence.spaceKey')
        this.spaceId = fetchSpaceIdByKey(spaceKey)
    }

    @Override
    def verifyCredentials() {
        HttpRequest get = new HttpGet(API_V1_PATH + '/user/current')
        return callApiAndFailIfNot20x(get)
    }

    def fetchSpaceIdByKey(String spaceKey) {
        URI uri = new URIBuilder(API_V2_PATH + '/spaces')
            .addParameter('keys', spaceKey)
            .addParameter('status', 'current')
            .addParameter('limit', "1")
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get).results?.getAt(0)?.id
    }

    @Override
    def addLabel(Object pageId, Object label) {
        HttpRequest post = new HttpPost(API_V1_PATH + '/content/' + pageId + "/label")
        post.setHeader('Content-Type', 'application/json')
        // TODO test if this works
        post.setEntity(new StringEntity(new JsonBuilder([label]).toPrettyString()))
        return callApiAndFailIfNot20x(post)
    }

    @Override
    def getAttachment(Object pageId, Object fileName) {
        URI uri = new URIBuilder(API_V2_PATH + '/pages/' + pageId + '/attachments')
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
        uploadAttachment(uri, inputStream, fileName, note, localHash)
    }

    @Override
    def attachmentHasChanged(attachment, localHash) {
        def remoteHash = attachment.results[0].comment.replaceAll("(?sm).*#([^#]+)#.*",'$1')
        return remoteHash!=localHash
    }

    @Override
    def fetchPagesBySpaceKey(String spaceKey, Integer pageLimit) {
        def allPages = [:]
        String cursor
        Boolean morePages = true
        while (morePages){
            URIBuilder uriBuilder = new URIBuilder(API_V2_PATH + "/spaces/${spaceId}/pages")
                .addParameter('depth', 'all')
                .addParameter('limit', pageLimit.toString())
            if(cursor){
                uriBuilder.addParameter('cursor', cursor)
            }
            URI uri = uriBuilder.build()
            HttpRequest get = new HttpGet(uri)
            def response =  callApiAndFailIfNot20x(get)
            def results = response.results ?: []
            if (results.empty || response._links.isEmpty()) {
                morePages = false
            } else {
                cursor = response._links.next.split("cursor=")[1]
            }
            results.inject(allPages) { Map acc, Map match ->
                //unique page names in confluence, so we can get away with indexing by title
                acc[match.title.toLowerCase()] = [
                    title   : match.title,
                    id      : match.id,
                    parentId: match.parentId
                ]
                acc
            }
        }
        return allPages
    }

    @Override
    def fetchPagesByAncestorId(List<String> pageIds, Integer pageLimit) {
        def allPages = [:]
        String cursor
        Boolean morePages = true
        def ids = []
        String pageId = pageIds.remove(0)
        while (morePages){
            URIBuilder uriBuilder = new URIBuilder(API_V2_PATH + "/pages/${pageId}/children")
                .addParameter('depth', 'all')
                .addParameter('limit', pageLimit.toString())
            if(cursor){
                uriBuilder.addParameter('cursor', cursor)
            }
            URI uri = uriBuilder.build()
            HttpRequest get = new HttpGet(uri)
            def response =  callApiAndFailIfNot20x(get)
            def results = response.results ?: []
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
            } else if (!results.empty && !response._links.isEmpty()) {
                cursor = response._links.next.split("cursor=")[1]
            } else {
                cursor = null
                pageId = ids.remove(0)
            }
        }
        return allPages
    }

    @Override
    def fetchPageByPageId(String id) {
        URI uri = new URIBuilder(API_V2_PATH + "/pages/${id}")
            .addParameter('body-format', 'storage')
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get)
    }

    @Override
    protected fetchPageIdByName(String name, String spaceKey) {
        URI uri = new URIBuilder(API_V2_PATH + "/spaces/${spaceId}/pages")
            .addParameter('title', name)
            .addParameter('status', "current")
            .build()
        HttpRequest get = new HttpGet(uri)
        return callApiAndFailIfNot20x(get).results?.getAt(0)?.id
    }

    // confluenceSpaceKey is not used in the V2 API
    @Override
    def updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId) {
        def requestBody = [
            "id"      : pageId,
            "status"  : "current",
            "title"   : title,
            "spaceId"   : spaceId,
            "parentId": parentId ?: "",
            "body"    : [
                "value"          : localPage,
                "representation": "storage"
            ],
            "version" : [
                "number" : pageVersion,
                "message": pageVersionComment
            ]
        ]
        HttpPut put = new HttpPut(API_V2_PATH + '/pages/' + pageId)
        put.setHeader('Content-Type', 'application/json')
        put.setEntity(new StringEntity(new JsonBuilder(requestBody).toPrettyString()))
        return callApiAndFailIfNot20x(put)
    }

    @Override
    def createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId) {
        def requestBody = [
            "title"   : title,
            "status"  : "current",
            "spaceId"   : spaceId,
            "parentId": parentId ?: "",
            "body"    : [
                "value"          : localPage,
                "representation": "storage"
            ],
            "version" : [
                "number" : 1,
                "message": pageVersionComment
            ]
        ]
        HttpPost post = new HttpPost(API_V2_PATH + '/pages')
        post.setHeader('Content-Type', 'application/json')
        post.setEntity(new StringEntity(new JsonBuilder(requestBody).toPrettyString()))
        return callApiAndFailIfNot20x(post)
    }
}
