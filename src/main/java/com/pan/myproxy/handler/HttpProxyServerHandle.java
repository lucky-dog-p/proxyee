package com.pan.myproxy.handler;

import com.github.monkeywie.proxyee.util.ProtoUtil.RequestProto;
import com.pan.myproxy.crt.CertPool;
import com.pan.myproxy.exception.HttpProxyExceptionHandle;
import com.pan.myproxy.intercept.HttpProxyIntercept;
import com.pan.myproxy.intercept.HttpProxyInterceptInitializer;
import com.pan.myproxy.intercept.HttpProxyInterceptPipeline;
import com.pan.myproxy.proxy.ProxyConfig;
import com.pan.myproxy.proxy.ProxyHandleFactory;
import com.pan.myproxy.server.HttpProxyServer;
import com.pan.myproxy.server.HttpProxyServerConfig;
import com.pan.myproxy.util.ProtoUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class HttpProxyServerHandle extends ChannelInboundHandlerAdapter {

  private ChannelFuture cf;
  private String host;
  private int port;
  private boolean isSsl = false;
  private int status = 0;
  private HttpProxyServerConfig serverConfig;
  private ProxyConfig proxyConfig;
  private HttpProxyInterceptInitializer interceptInitializer;
  private HttpProxyInterceptPipeline interceptPipeline;
  private HttpProxyExceptionHandle exceptionHandle;
  private List requestList;
  private boolean isConnect;

  public HttpProxyServerConfig getServerConfig() {
    return serverConfig;
  }

  public HttpProxyInterceptPipeline getInterceptPipeline() {
    return interceptPipeline;
  }

  public HttpProxyExceptionHandle getExceptionHandle() {
    return exceptionHandle;
  }

//  public HttpProxyServerHandle(HttpProxyServerConfig serverConfig,
//                               HttpProxyInterceptInitializer interceptInitializer,
//                               ProxyConfig proxyConfig, HttpProxyExceptionHandle exceptionHandle) {
//    this.serverConfig = serverConfig;
//    this.proxyConfig = proxyConfig;
//    this.interceptInitializer = interceptInitializer;
//    this.exceptionHandle = exceptionHandle;
//  }
  public HttpProxyServerHandle(HttpProxyServerConfig serverConfig,
                               HttpProxyInterceptInitializer interceptInitializer,
//                               ProxyConfig proxyConfig,
                               HttpProxyExceptionHandle exceptionHandle) {
    this.serverConfig = serverConfig;
//    this.proxyConfig = proxyConfig;
    this.interceptInitializer = interceptInitializer;
    this.exceptionHandle = exceptionHandle;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("我activate");
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;
      //第一次建立连接取host和端口号和处理代理握手
      if (status == 0) {
        ProtoUtil.RequestProto requestProto = ProtoUtil.getRequestProto(request);
//        if (requestProto == null) { //bad request
//          ctx.channel().close();
//          return;
//        }
        status = 1;
        this.host = requestProto.getHost();
//        this.host = "localhost";
        this.port = requestProto.getPort();
//        this.port = 8080;
//        System.out.println(request.uri()+"我是uri");
        if(request.uri().contains("/socket.io/")){
          System.out.println(port+"wodaiyinle");
////          this.port = 38888;
        }
//        this.host = "192.168.199.244";
//        this.port = 8081;
        if (
//                true
//                "CONNECT".equalsIgnoreCase(request.method().name())
                !request.uri().contains("/socket.io/")
        ) {//建立代理握手
          status = 2;
//          HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
//                  HttpProxyServer.SUCCESS);
//          ctx.writeAndFlush(response);
//          ctx.channel().pipeline().remove("httpCodec");
//          //fix issue #42
//          ReferenceCountUtil.release(msg);
//          return;
        }
      }
      interceptPipeline = buildPipeline();
//      interceptPipeline.setRequestProto(new RequestProto(host, port, isSsl));
//      interceptPipeline.setRequestProto(new RequestProto("localhost", 8080, isSsl));
      //fix issue #27
      if (request.uri().indexOf("/") != 0) {
        URL url = new URL(request.uri());
        request.setUri(url.getFile());
      }
      interceptPipeline.beforeRequest(ctx.channel(), request);
    } else if (msg instanceof HttpContent) {
      if (status != 2) {
        interceptPipeline.beforeRequest(ctx.channel(), (HttpContent) msg);
      } else {
        ReferenceCountUtil.release(msg);
        status = 1;
      }
    } else { //ssl和websocket的握手处理
//      if (serverConfig.isHandleSsl()) {
//        ByteBuf byteBuf = (ByteBuf) msg;
//        if (byteBuf.getByte(0) == 22) {//ssl握手
//          isSsl = true;
//          int port = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
//          SslContext sslCtx = SslContextBuilder
//              .forServer(serverConfig.getServerPriKey(), CertPool.getCert(port,this.host, serverConfig))
//              .build();
//          ctx.pipeline().addFirst("httpCodec", new HttpServerCodec());
//          ctx.pipeline().addFirst("sslHandle", sslCtx.newHandler(ctx.alloc()));
//          //重新过一遍pipeline，拿到解密后的的http报文
//          ctx.pipeline().fireChannelRead(msg);
//          return;
//        }
//      }
      handleProxyData(ctx.channel(), msg, false);
    }
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    if (cf != null) {
      cf.channel().close();
    }
    ctx.channel().close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cf != null) {
      cf.channel().close();
    }
    ctx.channel().close();
    exceptionHandle.beforeCatch(ctx.channel(), cause);
  }

  private void handleProxyData(Channel channel, Object msg, boolean isHttp)
      throws Exception {
    if (cf == null) {
      //connection异常 还有HttpContent进来，不转发
      if (isHttp && !(msg instanceof HttpRequest)) {
        return;
      }
//      ProxyHandler proxyHandler = ProxyHandleFactory.build(proxyConfig);
      /*
        添加SSL client hello的Server Name Indication extension(SNI扩展)
        有些服务器对于client hello不带SNI扩展时会直接返回Received fatal alert: handshake_failure(握手错误)
        例如：https://cdn.mdn.mozilla.net/static/img/favicon32.7f3da72dcea1.png
       */
      RequestProto requestProto = new RequestProto(host, port, isSsl);
      ChannelInitializer channelInitializer =
          isHttp ? new HttpProxyInitializer(channel, requestProto
//                  , proxyHandler
          )
              : new TunnelProxyInitializer(channel
//                  , proxyHandler
          );
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(serverConfig.getProxyLoopGroup()) // 注册线程池
          .channel(NioSocketChannel.class) // 使用NioSocketChannel来作为连接用的channel类
          .handler(channelInitializer);
//      if (proxyConfig != null) {
//        //代理服务器解析DNS和连接
//        bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
//      }
      requestList = new LinkedList();
      cf = bootstrap.connect(host, port);
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

  private HttpProxyInterceptPipeline buildPipeline() {
    HttpProxyInterceptPipeline interceptPipeline = new HttpProxyInterceptPipeline(
        new HttpProxyIntercept() {
          @Override
          public void beforeRequest(Channel clientChannel, HttpRequest httpRequest,
              HttpProxyInterceptPipeline pipeline) throws Exception {
            handleProxyData(clientChannel, httpRequest, true);
          }

          @Override
          public void beforeRequest(Channel clientChannel, HttpContent httpContent,
              HttpProxyInterceptPipeline pipeline) throws Exception {
            handleProxyData(clientChannel, httpContent, true);
          }

          @Override
          public void afterResponse(Channel clientChannel, Channel proxyChannel,
              HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) throws Exception {
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
              HttpContent httpContent, HttpProxyInterceptPipeline pipeline) throws Exception {
            clientChannel.writeAndFlush(httpContent);
          }
        });
    interceptInitializer.init(interceptPipeline);
    return interceptPipeline;
  }
}
