package org.docToolchain.atlassian.clients

import org.apache.hc.core5.http.HttpResponse

class RequestFailedException extends RuntimeException{

    RequestFailedException(HttpResponse response, Exception reason) {
        super(buildMessage(response, reason), reason);
    }

    private static String buildMessage(HttpResponse response, Exception reason) {
        String responseLog = response != null ? statusLine(response) : "<none>";
        String reasonLog = reason != null ? reason.getMessage() : "<none>";

        return "request failed" + " (" +
            "response: " + responseLog + ", " +
            "reason: " + reasonLog +
            ")";
    }

    private static String statusLine(HttpResponse response) {
        return response.getCode() + " " + response.getReasonPhrase();
    }
}
