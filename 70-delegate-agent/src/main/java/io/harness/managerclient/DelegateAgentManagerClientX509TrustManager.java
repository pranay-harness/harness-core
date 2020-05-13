package io.harness.managerclient;

import javax.net.ssl.X509TrustManager;

class DelegateAgentManagerClientX509TrustManager implements X509TrustManager {
  @Override
  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    return new java.security.cert.X509Certificate[] {};
  }

  @Override
  public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

  @Override
  public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
}
