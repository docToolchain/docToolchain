package org.docToolchain.atlassian.clients

import org.apache.hc.client5.http.ClientProtocolException
import org.apache.hc.core5.annotation.Contract
import org.apache.hc.core5.annotation.ThreadingBehavior
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.HttpEntity
import org.apache.hc.core5.http.HttpException
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.ParseException
import org.apache.hc.core5.http.io.HttpClientResponseHandler
import org.apache.hc.core5.http.io.entity.EntityUtils

@Contract(threading = ThreadingBehavior.STATELESS)
class ConfluenceClientResponseHandler implements HttpClientResponseHandler<String> {

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
