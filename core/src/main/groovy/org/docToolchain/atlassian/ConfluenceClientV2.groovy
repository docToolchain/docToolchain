package org.docToolchain.atlassian

import groovyx.net.http.ContentType
import org.docToolchain.configuration.ConfigService

class ConfluenceClientV2 extends ConfluenceClient {

    private final String spaceId

    ConfluenceClientV2(ConfigService configService) {
        super(configService)
        String spaceKey = configService.getConfigProperty('confluence.spaceKey')
        this.spaceId = fetchSpaceIdByKey(spaceKey)
    }

    def fetchSpaceIdByKey(String spaceKey) {
            return restClient.get(
                path   : API_V2_PATH + "spaces",
                headers: headers,
                query  : [
                    'keys' : [spaceKey],
                    'status': "current",
                    'limit': 1,
                ]
            ).data?.results?.getAt(0)?.id
    }

    @Override
    def addLabel(Object pageId, Object label) {
        trythis {
            restClient.post(contentType: ContentType.JSON,
                path: API_V1_PATH + 'content/' + pageId + "/label", body: label, headers: headers)
        }
    }

    @Override
    def getAttachment(Object pageId, Object filename) {
        restClient.get(
            path: API_V2_PATH + 'pages/' + pageId + '/attachment',
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
            'depth'   : 'all',
            'limit'   : pageLimit,
            //TODO use the cursor over offset
            'cursor'  : 'none',
            'start'   : offset,
        ]
        trythis {
            restClient.get(
                'headers': headers,
                'path'   : API_V2_PATH + "spaces/${spaceId}/pages",
                'query'  : query
            )
        } ?: []
    }

    @Override
    def fetchPagesByAncestorId(String pageId, Integer offset, Integer pageLimit) {
        def query = [
            'limit'   : pageLimit,
            //TODO use the cursor over offset
            'cursor'  : 'none',
            'start'   : offset,
        ]
        trythis {
            restClient.get(
                'headers': headers,
                'path'   : "pages/${pageId}/children",
                'query'  : query
            )
        } ?: []
    }

    @Override
    def fetchPageByPageId(String id) {
        restClient.get(
            path   : API_V2_PATH + "pages/${id}",
            headers: headers
        )
    }

    @Override
    protected fetchPageIdByName(String name, String spaceKey) {
        def request = [
            'title'    : name,
            'status' : ["current"]
        ]
        restClient.get(
            [
                'headers': headers,
                'path'   : API_V2_PATH + "spaces/${spaceId}/pages",
                'query'  : request,
            ]
        ).data.results?.getAt(0)?.id
    }

    @Override
    def updatePage(String pageId, String title, String confluenceSpaceKey, Object localPage, Integer pageVersion, String pageVersionComment, String parentId) {
        def requestBody = [
            "id"      : pageId,
            "status"  : "current",
            "title"   : title,
            "spaceId"   : spaceId,
            "parentId": parentId ?: "",
            "body"    : [
                "PageBodyWrite": [
                    "value"          : localPage,
                    "representation": "storage"
                ]
            ],
            "version" : [
                "number" : pageVersion,
                "message": pageVersionComment
            ]
        ]
        trythis {
            restClient.put(contentType: ContentType.JSON,
                requestContentType : ContentType.JSON,
                path: API_V2_PATH + 'pages/' + pageId, body: requestBody, headers: headers)
        }
    }

    @Override
    def createPage(String title, String confluenceSpaceKey, Object localPage, String pageVersionComment, String parentId) {
        def requestBody = [
            "title"   : title,
            "status"  : "current",
            "spaceId"   : spaceId,
            "parentId": parentId ?: "",
            "body"    : [
                "PageBodyWrite": [
                    "value"          : localPage,
                    "representation": "storage"
                ]
            ],
            "version" : [
                "number" : 1,
                "message": pageVersionComment
            ]
        ]
        trythis {
            restClient.post(contentType: ContentType.JSON,
                requestContentType: ContentType.JSON,
                path: API_V2_PATH + 'pages', body: requestBody, headers: headers)
        }
    }
}
