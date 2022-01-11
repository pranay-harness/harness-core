/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.managerclient;

import javax.net.ssl.X509TrustManager;

public class DelegateAgentManagerClientX509TrustManager implements X509TrustManager {
  @Override
  public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    return new java.security.cert.X509Certificate[] {};
  }

  @Override
  public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

  @Override
  public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
}
