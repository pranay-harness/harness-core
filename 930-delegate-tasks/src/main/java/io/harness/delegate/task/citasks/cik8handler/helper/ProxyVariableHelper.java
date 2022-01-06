/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler.helper;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.ci.pod.SecretParams.Type.TEXT;

import io.harness.delegate.beans.ci.pod.SecretParams;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ProxyVariableHelper {
  public static final String PROXY_STRING_FORMAT_WITH_CREDS = "%s://%s:%s@%s:%s";
  public static final String PROXY_STRING_FORMAT = "%s://%s:%s";

  public static final String PROXY_HOST = "PROXY_HOST";
  public static final String PROXY_PORT = "PROXY_PORT";
  public static final String PROXY_SCHEME = "PROXY_SCHEME";
  public static final String NO_PROXY = "NO_PROXY";
  public static final String PROXY_MANAGER = "PROXY_MANAGER";
  public static final String PROXY_USER = "PROXY_USER";
  public static final String PROXY_PASSWORD = "PROXY_PASSWORD";

  public static final String HTTP_PROXY_VARL = "http_proxy";
  public static final String HTTPS_PROXY_VARL = "https_proxy";
  public static final String NO_PROXY_VARL = "no_proxy";
  public static final String HTTP_PROXY_VARU = "HTTP_PROXY";
  public static final String HTTPS_PROXY_VARU = "HTTPS_PROXY";
  public static final String NO_PROXY_VARU = "NO_PROXY";

  public boolean checkIfProxyIsConfigured() {
    try {
      String proxyHost = System.getenv(PROXY_HOST);
      String proxyPort = System.getenv(PROXY_PORT);
      String proxyScheme = System.getenv(PROXY_SCHEME);
      if (isNotEmpty(proxyHost) && isNotEmpty(proxyPort) && isNotEmpty(proxyScheme)) {
        log.info("Setting env variables for proxy configuration");
        return true;
      } else {
        return false;
      }
    } catch (SecurityException e) {
      log.error("Don't have sufficient permission to query proxy env variables", e);
      return false;
    }
  }

  public Map<String, SecretParams> getProxyConfiguration() {
    String proxyHost = System.getenv(PROXY_HOST);
    String proxyPort = System.getenv(PROXY_PORT);
    String proxyScheme = System.getenv(PROXY_SCHEME);
    String proxyUser = System.getenv(PROXY_USER);
    String proxyPassword = System.getenv(PROXY_PASSWORD);
    String noProxyL = System.getenv(NO_PROXY_VARL);
    String noProxyU = System.getenv(NO_PROXY_VARU);

    String proxyString = null;
    if (isNotEmpty(proxyHost) && isNotEmpty(proxyPort) && isNotEmpty(proxyScheme)) {
      if (isNotEmpty(proxyUser) && isNotEmpty(proxyPassword)) {
        proxyString =
            String.format(PROXY_STRING_FORMAT_WITH_CREDS, proxyScheme, proxyUser, proxyPassword, proxyHost, proxyPort);
      } else {
        proxyString = String.format(PROXY_STRING_FORMAT, proxyScheme, proxyHost, proxyPort);
      }
    }

    if (isNotEmpty(proxyString)) {
      Map<String, SecretParams> proxyConfiguration = new HashMap<>();
      proxyConfiguration.put(HTTP_PROXY_VARL,
          SecretParams.builder().secretKey(HTTP_PROXY_VARL).value(encodeBase64(proxyString)).type(TEXT).build());
      proxyConfiguration.put(HTTPS_PROXY_VARL,
          SecretParams.builder().secretKey(HTTPS_PROXY_VARL).value(encodeBase64(proxyString)).type(TEXT).build());
      proxyConfiguration.put(HTTP_PROXY_VARU,
          SecretParams.builder().secretKey(HTTP_PROXY_VARU).value(encodeBase64(proxyString)).type(TEXT).build());
      proxyConfiguration.put(HTTPS_PROXY_VARU,
          SecretParams.builder().secretKey(HTTPS_PROXY_VARU).value(encodeBase64(proxyString)).type(TEXT).build());
      if (isNotEmpty(noProxyL)) {
        proxyConfiguration.put(NO_PROXY_VARL,
            SecretParams.builder().secretKey(NO_PROXY_VARL).value(encodeBase64(noProxyL)).type(TEXT).build());
      }
      if (isNotEmpty(noProxyU)) {
        proxyConfiguration.put(NO_PROXY_VARU,
            SecretParams.builder().secretKey(NO_PROXY_VARU).value(encodeBase64(noProxyU)).type(TEXT).build());
      }
      return proxyConfiguration;
    } else {
      return Collections.emptyMap();
    }
  }
}
