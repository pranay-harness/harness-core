package io.harness.delegate.task.mixin;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.utils.KmsUtils;
import io.harness.expression.DummySubstitutor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@UtilityClass
@Slf4j
public class HttpConnectionExecutionCapabilityGenerator {
  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapability(String urlString) {
    try {
      URI uri = new URI(DummySubstitutor.substitute(urlString));

      if (isNotBlank(uri.getScheme()) && isNotBlank(uri.getHost())) {
        HttpConnectionExecutionCapability httpConnectionExecutionCapability =
            HttpConnectionExecutionCapability.builder()
                .scheme(uri.getScheme())
                .host(uri.getHost())
                .port(uri.getPort())
                .path(getPath(uri))
                .build();
        if (!httpConnectionExecutionCapability.fetchCapabilityBasis().contains(DummySubstitutor.DUMMY_UUID)) {
          return httpConnectionExecutionCapability;
        }
      }
    } catch (Exception e) {
      logger.error("conversion to java.net.URI failed for url: " + urlString, e);
    }
    // This is falling back to existing approach, where we test for entire URL
    return HttpConnectionExecutionCapability.builder().url(urlString).build();
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapabilityForKms(String region) {
    String kmsUrl = KmsUtils.generateKmsUrl(region);
    return buildHttpConnectionExecutionCapability(kmsUrl);
  }

  private static String getPath(URI uri) {
    if (isBlank(uri.getPath())) {
      return null;
    }
    return uri.getPath().substring(1);
  }
}
