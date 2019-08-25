package com.pan.nettyproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jdk.nashorn.internal.runtime.linker.Bootstrap;

public class ProxyServer {
    private static final HttpResponseStatus SUCCESS = new HttpResponseStatus(200,
            "Connection established");

    private ProxyServerConfig proxyServerConfig;
    private EventLoopGroup boosGroup;
    private EventLoopGroup workGroup;

    public ProxyServer setProxyServerConfig(ProxyServerConfig proxyServerConfig){
        this.proxyServerConfig = proxyServerConfig;
        return this;
    }

    public void start(int port){
        proxyServerConfig.setProxyLoopGroup(new NioEventLoopGroup(proxyServerConfig.getProxyGroupThreads()));
        boosGroup = new NioEventLoopGroup(proxyServerConfig.getBossGroupThreads());
        workGroup = new NioEventLoopGroup(proxyServerConfig.getWorkerGroupThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boosGroup,workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast("httpCodec",new HttpServerCodec());
                            channel.pipeline().addLast("serverHandler",new ProxyServerHandler(proxyServerConfig));
                        }
                    }).bind(port).sync().channel().closeFuture().sync();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            boosGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}
