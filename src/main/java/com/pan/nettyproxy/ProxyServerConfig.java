package com.pan.nettyproxy;

import io.netty.channel.EventLoopGroup;

import java.util.Map;

public class ProxyServerConfig {

    private EventLoopGroup proxyLoopGroup;
    private int bossGroupThreads;
    private int workerGroupThreads;
    private int proxyGroupThreads;
    private int defaultPort;
    private int webSocketPort;

    public int getDefaultPort() {
        return defaultPort;
    }

    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
    }

    public int getWebSocketPort() {
        return webSocketPort;
    }

    public void setWebSocketPort(int webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    public EventLoopGroup getProxyLoopGroup() {
        return proxyLoopGroup;
    }

    public void setProxyLoopGroup(EventLoopGroup proxyLoopGroup) {
        this.proxyLoopGroup = proxyLoopGroup;
    }

    public int getBossGroupThreads() {
        return bossGroupThreads;
    }

    public void setBossGroupThreads(int bossGroupThreads) {
        this.bossGroupThreads = bossGroupThreads;
    }

    public int getWorkerGroupThreads() {
        return workerGroupThreads;
    }

    public void setWorkerGroupThreads(int workerGroupThreads) {
        this.workerGroupThreads = workerGroupThreads;
    }

    public int getProxyGroupThreads() {
        return proxyGroupThreads;
    }

    public void setProxyGroupThreads(int proxyGroupThreads) {
        this.proxyGroupThreads = proxyGroupThreads;
    }

}
