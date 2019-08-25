package com.github.monkeywie.proxyee;

//import com.github.monkeywie.proxyee.server.HttpProxyServer;
//import com.github.monkeywie.proxyee.server.ProxyServerConfig;
//import com.pan.myproxy.server.HttpProxyServer;
//import com.pan.myproxy.server.ProxyServerConfig;
import com.pan.nettyproxy.ProxyServer;
import com.pan.nettyproxy.ProxyServerConfig;

public class NormalHttpProxyServer2 {

  public static void main(String[] args) throws Exception {
   //new HttpProxyServer().start(9998);

    ProxyServerConfig config =  new ProxyServerConfig();
    config.setBossGroupThreads(1);
    config.setWorkerGroupThreads(1);
    config.setProxyGroupThreads(1);
    new ProxyServer()
        .setProxyServerConfig(config)
        .start(8888);
  }
}
