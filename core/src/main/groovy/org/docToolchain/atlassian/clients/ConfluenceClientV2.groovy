package org.docToolchain.atlassian.clients

import groovyx.net.http.ContentType
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
        trythis {
            restClient.get(
                path: API_V1_PATH + '/user/current', headers: headers
            )
        }
    }

    def fetchSpaceIdByKey(String spaceKey) {
            return restClient.get(
                path   : API_V2_PATH + "/spaces",
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
                path: API_V1_PATH + '/content/' + pageId + "/label", body: label, headers: headers)
        }
    }

    @Override
    def getAttachment(Object pageId, Object fileName) {
        //TODO NOT Found
        restClient.get(
            path: API_V2_PATH + '/pages/' + pageId + '/attachments',
            query: [
                'filename': fileName,
            ], headers: headers)
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
        trythis {
            while (morePages){
                def query = [
                    'depth'   : 'all',
                    'limit'   : pageLimit,
                ]
                if(cursor){
                    query['cursor'] = cursor
                }

                    def response = restClient.get(
                        'headers': headers,
                        'path'   : API_V2_PATH + "/spaces/${spaceId}/pages",
                        'query'  : query
                    ).data
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
            def query = [
                'depth'   : 'all',
                'limit'   : pageLimit,
            ]
            if(cursor){
                query['cursor'] = cursor
            }
            trythis {
                def response = restClient.get(
                    'headers': headers,
                    'path': API_V2_PATH + "/pages/${pageId}/children",
                    'query': query
                ).data
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
            } ?: []
        }
        return allPages
    }

    @Override
    def fetchPageByPageId(String id) {
        def query = [
            'body-format'   : 'storage'
        ]
        restClient.get(
            path   : API_V2_PATH + "/pages/${id}",
            headers: headers,
            query  : query
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
                'path'   : API_V2_PATH + "/spaces/${spaceId}/pages",
                'query'  : request,
            ]
        ).data.results?.getAt(0)?.id
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
        trythis {
            restClient.put(contentType: ContentType.JSON,
                requestContentType : ContentType.JSON,
                path: API_V2_PATH + '/pages/' + pageId, body: requestBody, headers: headers)
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
                "value"          : localPage,
                "representation": "storage"
            ],
            "version" : [
                "number" : 1,
                "message": pageVersionComment
            ]
        ]
        trythis {
            restClient.post(contentType: ContentType.JSON,
                requestContentType: ContentType.JSON,
                path: API_V2_PATH + '/pages', body: requestBody, headers: headers)
        }
    }
}
