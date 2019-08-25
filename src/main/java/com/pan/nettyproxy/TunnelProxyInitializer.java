package com.pan.nettyproxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;

public class TunnelProxyInitializer extends ChannelInitializer {

    private Channel clientChannel;

    public TunnelProxyInitializer(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {

        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx0, Object msg0) throws Exception {
                clientChannel.writeAndFlush(msg0);
            }

            @Override
            public void channelUnregistered(ChannelHandlerContext ctx0) throws Exception {
                ctx0.channel().close();
                clientChannel.close();
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx0, Throwable cause) throws Exception {
                ctx0.channel().close();
                clientChannel.close();
                ProxyExceptionHandle exceptionHandle = ((ProxyServerHandler) clientChannel.pipeline()
                        .get("serverHandle")).getProxyExceptionHandle();
                exceptionHandle.afterCatch(clientChannel,ctx0.channel(),cause);
            }
        });
    }
}
