package org.docToolchain.atlassian.jira.clients

import com.google.common.util.concurrent.RateLimiter
import groovy.json.JsonSlurper
import org.apache.hc.client5.http.ClientProtocolException
import org.apache.hc.core5.annotation.Contract
import org.apache.hc.core5.annotation.ThreadingBehavior
import org.apache.hc.core5.http.*
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
    protected Set<Header> headers

    RestClient(ConfigService configService) {
        super()
        this.configService = configService
        initialize()
    }

    def doRequestAndFailIfNot20x(ClassicHttpRequest httpRequest){
        rateLimiter.acquire()
        return doRequestAndFailIfNot20x(targetHost, httpRequest, new RestClientResponseHandler())
            .map(response -> new JsonSlurper().parseText(response))
            .orElse(null)
    }

    private initialize(){
        this.rateLimiter = RateLimiter.create(configService.getConfigProperty('jira.rateLimit') as Double ?: 10)
        this.headers = constructDefaultHeaders()
        this.targetHost = constructTargetHost()
        httpClientBuilder.setDefaultHeaders(headers)
    }

    private constructDefaultHeaders(){
        String basicAuthCredentials = configService.getConfigProperty('jira.credentials')
        HashSet<Header> headers = new HashSet<>([
            new BasicHeader('Content-Type', 'application/json;charset=utf-8'),
            new BasicHeader('Authorization', 'Basic ' + basicAuthCredentials)
        ])
        return headers
    }

    private constructTargetHost(){
        String apiConfigItem = configService.getConfigProperty('jira.api')
        URIBuilder builder = new URIBuilder(apiConfigItem)
        return new HttpHost(builder.getScheme(), builder.getHost(), builder.getPort())
    }

    def getTargetHost() {
        return targetHost
    }

    def getHeaders() {
        return headers
    }

    @Contract(threading = ThreadingBehavior.STATELESS)
    private class RestClientResponseHandler implements HttpClientResponseHandler<String> {

        @Override
        String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
            final HttpEntity entity = response.getEntity();
            if (response.getCode() < HttpStatus.SC_OK || response.getCode() > HttpStatus.SC_PARTIAL_CONTENT) {
                EntityUtils.consume(entity)
                println(response.getHeaders())
                throw new RequestFailedException(response, null)
            }
            return entity == null ? null : handleEntity(entity);
        }

        private String handleEntity(final HttpEntity entity) throws IOException {
            try {
                return EntityUtils.toString(entity);
            } catch (final ParseException ex) {
                throw new ClientProtocolException(ex);
            }
        }
    }
}

