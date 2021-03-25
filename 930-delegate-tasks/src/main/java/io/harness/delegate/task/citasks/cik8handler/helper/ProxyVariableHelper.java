package io.harness.delegate.task.citasks.cik8handler.helper;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.HasPredicate.hasSome;
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

  public static final String HTTP_PROXY_VAR = "http_proxy";
  public static final String HTTPS_PROXY_VAR = "https_proxy";
  public static final String NO_PROXY_VAR = "no_proxy";

  public boolean checkIfProxyIsConfigured() {
    try {
      String proxyHost = System.getenv(PROXY_HOST);
      String proxyPort = System.getenv(PROXY_PORT);
      String proxyScheme = System.getenv(PROXY_SCHEME);
      if (hasSome(proxyHost) && hasSome(proxyPort) && hasSome(proxyScheme)) {
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
    String noProxy = System.getenv(NO_PROXY_VAR);

    String proxyString = null;
    if (hasSome(proxyHost) && hasSome(proxyPort) && hasSome(proxyScheme)) {
      if (hasSome(proxyUser) && hasSome(proxyPassword)) {
        proxyString =
            String.format(PROXY_STRING_FORMAT_WITH_CREDS, proxyScheme, proxyUser, proxyPassword, proxyHost, proxyPort);
      } else {
        proxyString = String.format(PROXY_STRING_FORMAT, proxyScheme, proxyHost, proxyPort);
      }
    }

    if (hasSome(proxyString)) {
      Map<String, SecretParams> proxyConfiguration = new HashMap<>();
      proxyConfiguration.put(HTTP_PROXY_VAR,
          SecretParams.builder().secretKey(HTTP_PROXY_VAR).value(encodeBase64(proxyString)).type(TEXT).build());
      proxyConfiguration.put(HTTPS_PROXY_VAR,
          SecretParams.builder().secretKey(HTTPS_PROXY_VAR).value(encodeBase64(proxyString)).type(TEXT).build());
      if (hasSome(noProxy)) {
        proxyConfiguration.put(NO_PROXY_VAR,
            SecretParams.builder().secretKey(NO_PROXY_VAR).value(encodeBase64(noProxy)).type(TEXT).build());
      }
      return proxyConfiguration;
    } else {
      return Collections.emptyMap();
    }
  }
}
