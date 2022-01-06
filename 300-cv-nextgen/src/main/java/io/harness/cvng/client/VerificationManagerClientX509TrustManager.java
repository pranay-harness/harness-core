/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.client;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

class VerificationManagerClientX509TrustManager implements X509TrustManager {
  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[] {
        // internal network
    };
  }

  @Override
  public void checkClientTrusted(X509Certificate[] certs, String authType) {
    // internal network so no need to check
  }

  @Override
  public void checkServerTrusted(X509Certificate[] certs, String authType) {
    // internal network so no need to check
  }
}
