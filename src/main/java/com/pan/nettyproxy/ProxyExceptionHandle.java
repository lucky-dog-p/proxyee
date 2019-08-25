package com.pan.nettyproxy;

import io.netty.channel.Channel;

public class ProxyExceptionHandle {

  public void beforeCatch(Channel clientChannel, Throwable cause) throws Exception {
    throw new Exception(cause);
  }

  public void afterCatch(Channel clientChannel, Channel proxyChannel, Throwable cause)
      throws Exception {
    throw new Exception(cause);
  }
}
