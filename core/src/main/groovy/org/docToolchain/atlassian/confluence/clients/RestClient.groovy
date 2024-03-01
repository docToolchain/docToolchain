package org.docToolchain.atlassian.confluence.clients

import com.google.common.util.concurrent.RateLimiter
import groovy.json.JsonSlurper
import org.apache.hc.client5.http.ClientProtocolException
import org.apache.hc.core5.annotation.Contract
import org.apache.hc.core5.annotation.ThreadingBehavior
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpException
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.ParseException
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.net.URIBuilder
import org.docToolchain.configuration.ConfigService
import org.docToolchain.http.BasicRestClient
import org.docToolchain.http.RequestFailedException

class RestClient extends BasicRestClient {

    private final ConfigService configService
    private RateLimiter rateLimiter
    protected HttpHost targetHost
    protected HttpHost proxyHost
    protected Set<Header> headers

    RestClient(ConfigService configService) {
        super()
        this.configService = configService
        initialize()
    }

    def doRequestAndFailIfNot20x(ClassicHttpRequest httpRequest){
        return doRequest(httpRequest, (ClassicHttpResponse response, HttpEntity entity) -> {
            if (response.getCode() < HttpStatus.SC_OK || response.getCode() > HttpStatus.SC_PARTIAL_CONTENT) {
                EntityUtils.consume(entity)
                throw new RequestFailedException(response, null)
            }
        })
    }

    def doRequestAndReturnOrNull(ClassicHttpRequest httpRequest){
        return doRequest(httpRequest, (ClassicHttpResponse response, HttpEntity entity) -> {
            if (response.getCode() < HttpStatus.SC_OK || response.getCode() > HttpStatus.SC_PARTIAL_CONTENT) {
                EntityUtils.consume(entity)
                println("Got status code ${response.getCode()}")
                return null
            } else if (response.getCode() >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                EntityUtils.consume(entity)
                throw new RequestFailedException(response, null)
            }
        })
    }

    private doRequest(ClassicHttpRequest httpRequest, Closure callback){
        rateLimiter.acquire()
        return doRequest(targetHost, httpRequest, new RestClientResponseHandler(callback))
            .map(response -> new JsonSlurper().parseText(response))
            .orElse(null)
    }

    private initialize(){
        this.rateLimiter = RateLimiter.create(configService.getConfigProperty('confluence.rateLimit') as Double ?: 10)
        this.headers = constructDefaultHeaders()
        this.targetHost = constructTargetHost()
        if(configService.getFlatConfigSubTree('confluence.proxy')){
            configureProxy()
        }
        httpClientBuilder.setDefaultHeaders(headers)
    }

    private constructDefaultHeaders(){
        HashSet<Header> headers = new HashSet<>([
            new BasicHeader('X-Atlassian-Token', 'no-check'),
        ])
        if(configService.getConfigProperty('confluence.bearerToken')){
            headers.add(new BasicHeader('Authorization', 'Bearer ' + configService.getConfigProperty('confluence.bearerToken')))
            println 'Start using bearer auth'
        } else {
            headers.add(new BasicHeader('Authorization', 'Basic ' + configService.getConfigProperty('confluence.credentials')))
            //Add api key and value to REST API request header if configured - required for authentification.
            if (configService.getConfigProperty('confluence.apikey')){
                headers.add(new BasicHeader('keyid', configService.getConfigProperty('confluence.apikey') as String))
            }
        }
        return headers
    }

    private configureProxy(){
        def proxy = configService.getFlatConfigSubTree('confluence.proxy')
        this.proxyHost = new HttpHost(proxy.schema  as String?: 'http', proxy.host as String, proxy.port as Integer)
        httpClientBuilder.setProxy(proxyHost)
    }

    private constructTargetHost(){
        String apiConfigItem = configService.getConfigProperty('confluence.api')
        URIBuilder builder = new URIBuilder(apiConfigItem)
        return new HttpHost(builder.getScheme(), builder.getHost(), builder.getPort())
    }

    def getTargetHost() {
        return targetHost
    }

    def getHeaders() {
        return headers
    }

    def getProxyHost() {
        return proxyHost
    }

    @Contract(threading = ThreadingBehavior.STATELESS)
    private class RestClientResponseHandler implements HttpClientResponseHandler<String> {

        Closure callback

        RestClientResponseHandler(Closure callback) {
            this.callback = callback
        }

        @Override
        String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
            final HttpEntity entity = response.getEntity()
            callback(response, entity)
            return entity == null ? null : handleEntity(entity)
        }

        private String handleEntity(final HttpEntity entity) throws IOException {
            try {
                return EntityUtils.toString(entity)
            } catch (final ParseException ex) {
                throw new ClientProtocolException(ex)
            } finally {
                EntityUtils.consumeQuietly(entity)
            }
        }
    }
}

