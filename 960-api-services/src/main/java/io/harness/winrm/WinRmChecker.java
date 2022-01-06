/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.winrm;

import static java.lang.String.format;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import io.cloudsoft.winrm4j.client.WinRmClient;
import io.cloudsoft.winrm4j.client.WinRmClientBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WinRmChecker {
  // Checks whether we can make a winrm connection or not
  public static boolean checkConnectivity(String hostname, int port, boolean useSSL, String domain) {
    try {
      WinRmClientBuilder clientBuilder = WinRmClient.builder(getEndpoint(hostname, port, useSSL))
                                             .disableCertificateChecks(true)
                                             .authenticationScheme("Basic")
                                             .credentials(domain, "no-username", "no-password")
                                             .workingDirectory("%USERPROFILE%")
                                             .retriesForConnectionFailures(3)
                                             .operationTimeout(30 * 60 * 1000); // 30 minutes
      clientBuilder.build().createShell();
      return true;
    } catch (Exception e) {
      log.info("WinRM Check connection outcome: " + e);
      ResponseMessage processedException = WinRmHelperUtils.buildErrorDetailsFromWinRmClientException(e);
      if (processedException.getCode() == ErrorCode.SSL_HANDSHAKE_FAILED
          || processedException.getCode() == ErrorCode.INVALID_CREDENTIAL) {
        return true;
      } else {
        return false;
      }
    }
  }

  private static String getEndpoint(String hostname, int port, boolean useHttps) {
    return format("%s://%s:%d/wsman", useHttps ? "https" : "http", hostname, port);
  }

  private static String getUserPrincipal(String username, String domain) {
    if (username == null || domain == null) {
      throw new InvalidRequestException("Username or domain cannot be null", WingsException.USER);
    }
    if (username.contains("@")) {
      username = username.substring(0, username.indexOf('@'));
    }
    return format("%s@%s", username, domain.toUpperCase());
  }
}
