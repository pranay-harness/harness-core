/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
