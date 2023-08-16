package org.docToolchain.atlassian

import groovyx.net.http.ContentType

class ConfluenceClientV1 extends ConfluenceClient {

    ConfluenceClientV1(Object baseApiUrl, Object editorVersion) {
        super(baseApiUrl, editorVersion)
    }

    @Override
    def addLabel(pageId, label) {
        trythis {
            restClient.post(contentType: ContentType.JSON,
                path: 'content/' + pageId + "/label", body: label, headers: headers)
        }
    }

    @Override
    def getAttachment(Object pageId, Object fileName) {
        restClient.get(
            path: 'content/' + pageId + '/child/attachment',
            query: [
                'filename': fileName,
            ], headers: headers)
    }

    @Override
    def updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash) {
        def uri = 'content/' + pageId + '/child/attachment/' + attachmentId + '/data'
        uploadAttachment(uri, inputStream, fileName, note, localHash)
    }

    @Override
    def createAttachment(String pageId, InputStream inputStream, String fileName, String note, String localHash) {
        def uri = ""
        uploadAttachment(uri, inputStream, fileName, note, localHash)
    }

    @Override
    def fetchPagesBySpaceKey(String spaceKey, Integer offset, Integer pageLimit) {
        def query = [
                'type'    : 'page',
                'spaceKey': spaceKey,
                'expand'  : 'ancestors',
                'limit'   : pageLimit,
                'start'   : offset,
        ]
        trythis {
            restClient.get(
                    'headers': headers,
                    'path'   : "content",
                    'query'  : query
            )
        } ?: []
    }

    @Override
    def fetchPagesByAncestorId(String pageId, Integer offset, Integer pageLimit) {
        def query = [
                'type'    : 'page',
                'limit'   : pageLimit,
                'start'   : offset,
        ]
        trythis {
            restClient.get(
                    'headers': headers,
                    'path'   : "content/${pageId}/child/page",
                    'query'  : query
            )
        } ?: []
    }

    @Override
    def fetchPageByPageId(String id) {
        restClient.get(
                path   : "content/${id}",
                query: [
                        'expand': 'body.storage,version,ancestors'
                ], headers: headers)
    }

    @Override
    protected fetchPageIdByName(String name, String spaceKey) {
        def request = [
                'title'    : name,
                'spaceKey' : confluenceSpaceKey
        ]
        restClient.get(
                [
                        'headers': headers,
                        'path'   : "content",
                        'query'  : request,
                ]
        ).data.results?.getAt(0)?.id
    }

    @Override
    protected doUpdatePageRequest(String pageId, Map requestBody) {
        restClient.put(contentType: ContentType.JSON,
                requestContentType : ContentType.JSON,
                path: 'content/' + pageId, body: requestBody, headers: headers)
    }

    @Override
    protected doCreatePageRequest(Map requestBody) {
        restClient.post(contentType: ContentType.JSON,
                requestContentType: ContentType.JSON,
                path: 'content', body: requestBody, headers: headers)
    }
}
