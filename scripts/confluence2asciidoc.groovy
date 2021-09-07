//WIP
// some dependencies
/**
@Grapes(
        [@Grab('org.jsoup:jsoup:1.8.2'),
         @Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.6' ),
         @Grab('org.apache.httpcomponents:httpmime:4.5.1')]
)
**/
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Document.OutputSettings
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.EncoderRegistry
import groovyx.net.http.ContentType
import java.security.MessageDigest
//to upload attachments:
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.HttpMultipartMode
import groovyx.net.http.Method

def CDATA_PLACEHOLDER_START = '<cdata-placeholder>'
def CDATA_PLACEHOLDER_END = '</cdata-placeholder>'

def baseUrl

// configuration

def confluenceSpaceKey

// helper functions

def MD5(String s) {
    MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex().toString()
}

// for getting better error message from the REST-API
void trythis (Closure action) {
    try {
        action.run()
    } catch (HttpResponseException error) {
        println "something went wrong - got an http response code "+error.response.status+":"
        println error.response.data
        //throw error
    }
}

def fetchPage = { pageId ->
    def api = new RESTClient(config.confluence.api)
    def headers = [
            'Authorization': 'Basic ' + config.confluence.credentials,
            'Content-Type':'application/json; charset=utf-8'
    ]
    //this fixes the encoding
    api.encoderRegistry = new EncoderRegistry( charset: 'utf-8' )

    def page
    trythis {
        page = api.get(path: 'content/'+pageId,
                query: [
                        'expand'  : 'body.storage,version'
                ], headers: headers).data
    }
    println page
    return page
}

def page = fetchPage(579633280)
""
