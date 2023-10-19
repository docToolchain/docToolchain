package org.docToolchain.atlassian.clients

import groovyx.net.http.ContentType
import groovyx.net.http.URIBuilder
import org.docToolchain.configuration.ConfigService

class ConfluenceClientV1 extends ConfluenceClient {

    protected final String API_V1_PATH

    ConfluenceClientV1(ConfigService configService) {
        super(configService)
        API_V1_PATH = getRealApiPath()
    }

    protected String getRealApiPath() {
        if(this.baseApiUrl.contains("/rest/api")) {
            def path = new URIBuilder(this.baseApiUrl).getPath()
            return path.endsWith("/") ? path : path + "/"
        }
        return API_V1_DEFAULT_PATH
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
    def fetchPagesBySpaceKey(String spaceKey, Integer pageLimit) {
        def allPages = [:]
        Boolean morePages = true
        Integer offset = 0
        trythis {
            while (morePages) {
                def query = [
                    'type'    : 'page',
                    'spaceKey': spaceKey,
                    'expand'  : 'ancestors',
                    'limit'   : pageLimit,
                    'start'   : offset,
                ]
                def results = restClient.get(
                    'headers': headers,
                    'path'   : API_V1_PATH + "content",
                    'query'  : query
                ).data
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
            def query = [
                'type'    : 'page',
                'limit'   : pageLimit,
                'start'   : offset,
            ]
            trythis {
                def results = restClient.get(
                    'headers': headers,
                    'path'   : API_V1_PATH + "content/${pageId}/child/page",
                    'query'  : query
                )
                results = results.data.results ?: []

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
            } ?: []
        }
        return allPages
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
