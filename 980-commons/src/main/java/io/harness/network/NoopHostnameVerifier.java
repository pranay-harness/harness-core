/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.network;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class NoopHostnameVerifier implements HostnameVerifier {
  @Override
  public boolean verify(String hostname, SSLSession session) {
    if (log.isDebugEnabled()) {
      log.debug("Approving hostname " + hostname);
    }
    return true;
  }
}
