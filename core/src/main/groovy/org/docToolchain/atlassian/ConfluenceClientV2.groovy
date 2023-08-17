package org.docToolchain.atlassian

import org.apache.commons.lang.NotImplementedException

class ConfluenceClientV2 extends ConfluenceClient {

    ConfluenceClientV2(Object baseApiUrl, Object editorVersion) {
        super(baseApiUrl, editorVersion)
        throw new NotImplementedException("ConfluenceClientV2 is not implemented yet")
    }

    @Override
    def addLabel(Object pageId, Object label) {
        return null
    }

    @Override
    def getAttachment(Object pageId, Object filename) {
        return null
    }

    @Override
    def updateAttachment(String pageId, String attachmentId, InputStream inputStream, String fileName, String note, String localHash) {
        return null
    }

    @Override
    def createAttachment(String pageId, InputStream inputStream, String fileName, String note, String localHash) {
        return null
    }

    @Override
    def fetchPagesBySpaceKey(String spaceKey, Integer offset, Integer pageLimit) {
        return null
    }

    @Override
    def fetchPagesByAncestorId(String pageId, Integer offset, Integer pageLimit) {
        return null
    }

    @Override
    def fetchPageByPageId(String id) {
        return null
    }

    @Override
    protected fetchPageIdByName(String name, String spaceKey) {
        return null
    }

    @Override
    protected doUpdatePageRequest(String pageId, Map requestBody) {
        return null
    }

    @Override
    protected doCreatePageRequest(Map requestBody) {
        return null
    }
}
