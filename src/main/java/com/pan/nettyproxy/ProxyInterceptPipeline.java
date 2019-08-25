package com.pan.nettyproxy;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.util.LinkedList;
import java.util.List;

public class ProxyInterceptPipeline {

    private List<ProxyIntercept> intercepts;
    private ProxyIntercept defaultIntercept;

    private int posBeforeHead = 0;
    private int posBeforeContent = 0;
    private int posAfterHead = 0;
    private int posAfterContent = 0;

    private HttpRequest httpRequest;
    private HttpResponse httpResponse;

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    
    public ProxyInterceptPipeline(ProxyIntercept defaultIntercept) {
        this.intercepts = new LinkedList<>();
        this.defaultIntercept = defaultIntercept;
        this.intercepts.add(defaultIntercept);
    }
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest) throws Exception {
        this.httpRequest = httpRequest;
        if (this.posBeforeHead < intercepts.size()) {
            ProxyIntercept intercept = intercepts.get(this.posBeforeHead++);
            intercept.beforeRequest(clientChannel, this.httpRequest, this);
        }
        this.posBeforeHead = 0;
    }

    public void beforeRequest(Channel clientChannel, HttpContent httpContent) throws Exception {
        if (this.posBeforeContent < intercepts.size()) {
            ProxyIntercept intercept = intercepts.get(this.posBeforeContent++);
            intercept.beforeRequest(clientChannel, httpContent, this);
        }
        this.posBeforeContent = 0;
    }

    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse)
            throws Exception {
        this.httpResponse = httpResponse;
        if (this.posAfterHead < intercepts.size()) {
            ProxyIntercept intercept = intercepts.get(this.posAfterHead++);
            intercept.afterResponse(clientChannel, proxyChannel, this.httpResponse, this);
        }
        this.posAfterHead = 0;
    }

    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpContent httpContent)
            throws Exception {
        if (this.posAfterContent < intercepts.size()) {
            ProxyIntercept intercept = intercepts.get(this.posAfterContent++);
            intercept.afterResponse(clientChannel, proxyChannel, httpContent, this);
        }
        this.posAfterContent = 0;
    }
}
