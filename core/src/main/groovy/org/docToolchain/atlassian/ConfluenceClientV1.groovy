package org.docToolchain.atlassian

import groovyx.net.http.ContentType
import org.docToolchain.configuration.ConfigService

class ConfluenceClientV1 extends ConfluenceClient {

    ConfluenceClientV1(ConfigService configService) {
        super(configService)
    }

    @Override
    def addLabel(pageId, label) {
        trythis {
            restClient.post(contentType: ContentType.JSON,
                path: API_V1_PATH + 'content/' + pageId + "/label", body: label, headers: headers)
        }
    }

    @Override
    def getAttachment(Object pageId, Object fileName) {
        restClient.get(
            path: API_V1_PATH + 'content/' + pageId + '/child/attachment',
            query: [
                'filename': fileName,
            ], headers: headers)
    }

    @Override
    def updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash) {
        def uri = API_V1_PATH + 'content/' + pageId + '/child/attachment/' + attachmentId + '/data'
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
                    'path'   : API_V1_PATH + "content",
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
                    'path'   : API_V1_PATH + "content/${pageId}/child/page",
                    'query'  : query
            )
        } ?: []
    }

    @Override
    def fetchPageByPageId(String id) {
        restClient.get(
                path   : API_V1_PATH + "content/${id}",
                query: [
                        'expand': 'body.storage,version,ancestors'
                ], headers: headers)
    }

    @Override
    protected fetchPageIdByName(String name, String spaceKey) {
        def request = [
                'title'    : name,
                'spaceKey' : spaceKey
        ]
        restClient.get(
                [
                        'headers': headers,
                        'path'   : API_V1_PATH + "content",
                        'query'  : request,
                ]
        ).data.results?.getAt(0)?.id
    }

    @Override
    def updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId = null){
        def requestBody = getDefaultModifyPageRequestBody(title, confluenceSpaceKey, localPage, parentId)
        requestBody.id      = pageId
        requestBody.version = [number: pageVersion, message: pageVersionComment ?: '']
        trythis {
            restClient.put(contentType: ContentType.JSON,
                requestContentType : ContentType.JSON,
                path: API_V1_PATH + 'content/' + pageId, body: requestBody, headers: headers)
        }
    }

    @Override
    def createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId = null){
        def requestBody = getDefaultModifyPageRequestBody(title, confluenceSpaceKey, localPage, parentId)
        requestBody.version = [message: pageVersionComment ?: '']
        trythis {
            restClient.post(contentType: ContentType.JSON,
                requestContentType: ContentType.JSON,
                path: API_V1_PATH + 'content', body: requestBody, headers: headers)
        }
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
