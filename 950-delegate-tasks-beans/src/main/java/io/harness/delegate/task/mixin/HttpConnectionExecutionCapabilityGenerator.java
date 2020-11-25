package io.harness.delegate.task.mixin;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.utils.KmsUtils;
import io.harness.expression.DummySubstitutor;

import java.net.URI;
import java.net.URISyntaxException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class HttpConnectionExecutionCapabilityGenerator {
  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapability(String urlString) {
    return buildHttpConnectionExecutionCapability(urlString, HttpCapabilityDetailsLevel.PATH);
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapabilityForKms(String region) {
    String kmsUrl = KmsUtils.generateKmsUrl(region);
    return buildHttpConnectionExecutionCapability(kmsUrl);
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapability(
      String urlString, HttpCapabilityDetailsLevel level) {
    try {
      URI uri = new URI(DummySubstitutor.substitute(urlString));

      if (isNotBlank(uri.getScheme()) && isNotBlank(uri.getHost())) {
        HttpConnectionExecutionCapability httpConnectionExecutionCapability =
            level.getHttpConnectionExecutionCapability(urlString);
        if (!httpConnectionExecutionCapability.fetchCapabilityBasis().contains(DummySubstitutor.DUMMY_UUID)) {
          return httpConnectionExecutionCapability;
        }
      }
    } catch (Exception e) {
      log.error("conversion to java.net.URI failed for url: " + urlString, e);
    }
    // This is falling back to existing approach, where we test for entire URL
    return HttpConnectionExecutionCapability.builder().url(urlString).build();
  }

  public enum HttpCapabilityDetailsLevel {
    DOMAIN(false, false),
    PATH(true, false),
    QUERY(true, true);
    private boolean usePath, useQuery;

    HttpCapabilityDetailsLevel(boolean usePath, boolean useQuery) {
      this.usePath = usePath;
      this.useQuery = useQuery;
    }

    private HttpConnectionExecutionCapability getHttpConnectionExecutionCapability(String urlString)
        throws URISyntaxException {
      URI uri = new URI(DummySubstitutor.substitute(urlString));
      return HttpConnectionExecutionCapability.builder()
          .scheme(uri.getScheme())
          .host(uri.getHost())
          .port(uri.getPort())
          .path(usePath ? getPath(uri) : null)
          .query(useQuery ? uri.getQuery() : null)
          .build();
    }
    private static String getPath(URI uri) {
      if (isBlank(uri.getPath())) {
        return null;
      }
      return uri.getPath().substring(1);
    }
  }
}
