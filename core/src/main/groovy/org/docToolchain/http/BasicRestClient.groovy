package org.docToolchain.http

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.HttpClientResponseHandler

abstract class BasicRestClient {

    protected HttpClientBuilder httpClientBuilder

    BasicRestClient() {
        this.httpClientBuilder = HttpClientBuilder.create()
    }

    def doRequest(HttpHost targetHost, ClassicHttpRequest httpRequest, HttpClientResponseHandler<String> responseHandler) {
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            return Optional.ofNullable(httpClient.execute(targetHost, httpRequest, responseHandler))
        } catch (IOException e) {
            println("Error while executing request: \n" +
                "request:" + httpRequest.getMethod() + " " + httpRequest.getUri() + ",\n" +
                "targetHost:" + targetHost.toURI() + "\n" +
                "reason:" + e.getMessage() + "\n"
            )
            throw new RuntimeException(e)
        }
    }

    protected getHttpClientBuilder() {
        return httpClientBuilder
    }
}
