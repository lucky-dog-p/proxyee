package com.pan.nettyproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class ProxyServerHandler extends ChannelInboundHandlerAdapter {

    private ChannelFuture cf;
    private String host = "192.168.199.234";
    private volatile int port = 8081;
    private boolean isConnect;
    private int status;
    private List requestList;
    private ProxyServerConfig proxyServerConfig;
    private ProxyInterceptPipeline proxyInterceptPipeline;
    private ProxyExceptionHandle proxyExceptionHandle = new ProxyExceptionHandle();

    public ProxyExceptionHandle getProxyExceptionHandle(){
        return proxyExceptionHandle;
    }

    public ProxyInterceptPipeline getProxyInterceptPipeline() {
        return proxyInterceptPipeline;
    }

    public ProxyServerHandler(ProxyServerConfig proxyServerConfig){
        this.proxyServerConfig = proxyServerConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        int port = 8080;
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest)msg;
            if(httpRequest.headers().get(HttpHeaderNames.CONNECTION).equalsIgnoreCase("Upgrade")){
                System.out.println("38888经过了");
                port = 38888;
            }
            proxyInterceptPipeline = buildProxyInterceptPipeline();
            if (httpRequest.uri().indexOf("/") != 0) {
                URL url = new URL(httpRequest.uri());
                httpRequest.setUri(url.getFile());
            }
            proxyInterceptPipeline.beforeRequest(ctx.channel(), httpRequest);
        } else if (msg instanceof HttpContent) {
//            if(status != 2){
                proxyInterceptPipeline.beforeRequest(ctx.channel(), (HttpContent) msg);
//            } else {
//                ReferenceCountUtil.release(msg);
//                status = 1;
//            }
        }else {
            handleProxyData(ctx.channel(), msg,false);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (cf != null){
            cf.channel().close();
        }
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cf != null){
            cf.channel().close();
        }
        ctx.channel().close();
    }

    private void handleProxyData(Channel channel, Object msg, boolean isHttp){
        if (cf == null){
            if (isHttp && !(msg instanceof HttpRequest)) {
                return;
            }
            ChannelInitializer channelInitializer =
                    isHttp ? new HttpProxyInitializer(channel) : new TunnelProxyInitializer(channel);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(proxyServerConfig.getProxyLoopGroup()) // 注册线程池
                    .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
                    .handler(channelInitializer);
            requestList = new LinkedList();
            cf = bootstrap.connect(this.host, port);
            cf.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(msg);
                    synchronized (requestList) {
                        requestList.forEach(obj -> future.channel().writeAndFlush(obj));
                        requestList.clear();
                        isConnect = true;
                    }
                } else {
                    requestList.forEach(obj -> ReferenceCountUtil.release(obj));
                    requestList.clear();
                    future.channel().close();
                    channel.close();
                }
            });
        } else {
            synchronized (requestList) {
                if (isConnect) {
                    cf.channel().writeAndFlush(msg);
                } else {
                    requestList.add(msg);
                }
            }
        }
    }

    private ProxyInterceptPipeline buildProxyInterceptPipeline(){
        ProxyInterceptPipeline proxyInterceptPipeline = new ProxyInterceptPipeline(new ProxyIntercept(){

            @Override
            public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,
                                      ProxyInterceptPipeline pipeline) throws Exception {
                handleProxyData(clientChannel, httpRequest,true);
            }

            @Override
            public void beforeRequest(Channel clientChannel, HttpContent httpContent,
                                      ProxyInterceptPipeline pipeline) throws Exception {
                handleProxyData(clientChannel, httpContent,true);
            }

            @Override
            public void afterResponse(Channel clientChannel, Channel proxyChannel,
                                      HttpResponse httpResponse, ProxyInterceptPipeline pipeline) throws Exception {
                clientChannel.writeAndFlush(httpResponse);
                if (HttpHeaderValues.WEBSOCKET.toString()
                        .equals(httpResponse.headers().get(HttpHeaderNames.UPGRADE))) {
                    //websocket转发原始报文
                    proxyChannel.pipeline().remove("httpCodec");
                    clientChannel.pipeline().remove("httpCodec");
                }
            }

            @Override
            public void afterResponse(Channel clientChannel, Channel proxyChannel,
                                      HttpContent httpContent, ProxyInterceptPipeline pipeline) throws Exception {
                clientChannel.writeAndFlush(httpContent);
            }
        });
        return proxyInterceptPipeline;
    }


}
