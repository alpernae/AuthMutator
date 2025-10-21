package model;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class RequestLogEntry {
    private final int id;
    private final String method;
    private final String url;
    private final int statusCode;
    private final HttpRequest originalRequest;
    private final HttpRequest modifiedRequest;
    private final HttpRequest unauthRequest;
    private final HttpResponse originalResponse;
    private final HttpResponse modifiedResponse;
    private final HttpResponse unauthResponse;
    private final long timestamp;
    private final boolean wasModified;
    private final boolean unauthenticatedTesting;
    private final boolean modifiedRequestSent;

    public RequestLogEntry(int id, HttpRequest originalRequest, HttpRequest modifiedRequest,
                           HttpResponse originalResponse, HttpResponse modifiedResponse,
                           boolean unauthenticatedTesting) {
        this(id, originalRequest, modifiedRequest, null, originalResponse, modifiedResponse, null,
                modifiedRequest != null, unauthenticatedTesting);
    }

    public RequestLogEntry(int id, HttpRequest originalRequest, HttpRequest modifiedRequest,
                           HttpRequest unauthRequest, HttpResponse originalResponse,
                           HttpResponse modifiedResponse, HttpResponse unauthResponse,
                           boolean modifiedRequestSent, boolean unauthenticatedTesting) {
        this.id = id;
        this.originalRequest = originalRequest;
        this.modifiedRequest = modifiedRequest;
        this.unauthRequest = unauthRequest;
        this.originalResponse = originalResponse;
        this.modifiedResponse = modifiedResponse;
        this.unauthResponse = unauthResponse;
        this.modifiedRequestSent = modifiedRequestSent;
        this.method = originalRequest.method();
        this.url = originalRequest.url();
        this.statusCode = computePrimaryStatusCode();
        this.timestamp = System.currentTimeMillis();
        this.wasModified = modifiedRequest != null && !originalRequest.toString().equals(modifiedRequest.toString());
        this.unauthenticatedTesting = unauthenticatedTesting;
    }

    public RequestLogEntry(int id, HttpRequest originalRequest, HttpRequest modifiedRequest,
                           HttpResponse originalResponse, HttpResponse modifiedResponse,
                           HttpResponse unauthResponse, boolean unauthenticatedTesting) {
        this(id, originalRequest, modifiedRequest, null, originalResponse, modifiedResponse, unauthResponse,
                modifiedRequest != null, unauthenticatedTesting);
    }

    public int getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Integer getOriginalStatusCode() {
        return originalResponse != null ? (int) originalResponse.statusCode() : null;
    }

    public Integer getModifiedStatusCode() {
        return modifiedResponse != null ? (int) modifiedResponse.statusCode() : null;
    }

    public HttpRequest getRequest() {
        if (modifiedRequestSent && modifiedRequest != null) {
            return modifiedRequest;
        }
        return originalRequest;
    }

    public HttpRequest getOriginalRequest() {
        return originalRequest;
    }

    public HttpRequest getModifiedRequest() {
        return modifiedRequest;
    }

    public HttpRequest getUnauthRequest() {
        return unauthRequest;
    }

    public HttpResponse getResponse() {
        if (modifiedRequestSent && modifiedResponse != null) {
            return modifiedResponse;
        }
        return originalResponse != null ? originalResponse : modifiedResponse;
    }

    public HttpResponse getOriginalResponse() {
        return originalResponse;
    }

    public HttpResponse getModifiedResponse() {
        return modifiedResponse;
    }

    public HttpResponse getUnauthResponse() {
        return unauthResponse;
    }

    public boolean wasModified() {
        return wasModified;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isUnauthenticatedTesting() {
        return unauthenticatedTesting;
    }

    public boolean wasModifiedRequestSent() {
        return modifiedRequestSent;
    }

    public String getCookieSummary() {
        HttpRequest req = getRequest();
        long cookieCount = req.parameters().stream()
                .filter(p -> p.type() == burp.api.montoya.http.message.params.HttpParameterType.COOKIE)
                .count();
        return cookieCount == 0 ? "None" : cookieCount + " cookie(s)";
    }

    public String getParameterSummary() {
        HttpRequest req = getRequest();
        int count = req.parameters().size();
        return count == 0 ? "None" : count + " param(s)";
    }

    public Integer getUnauthStatusCode() {
        return unauthResponse != null ? (int) unauthResponse.statusCode() : null;
    }

    private int computePrimaryStatusCode() {
        if (modifiedRequestSent && modifiedResponse != null) {
            return modifiedResponse.statusCode();
        }
        if (originalResponse != null) {
            return originalResponse.statusCode();
        }
        if (!modifiedRequestSent && modifiedResponse != null) {
            return modifiedResponse.statusCode();
        }
        if (unauthResponse != null) {
            return unauthResponse.statusCode();
        }
        return 0;
    }
}
