package io.harness.delegate.task.mixin;

import static org.apache.commons.lang3.StringUtils.isBlank;

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

      return HttpConnectionExecutionCapability.builder()
          .scheme(uri.getScheme())
          .hostName(getHostName(uri))
          .port(uri.getPort() == -1 ? null : Integer.toString(uri.getPort()))
          .url(urlString)
          .build();

    } catch (Exception e) {
      logger.warn("conversion to java.net.URI failed for url: " + urlString);
      // This is falling back to existing approach, where we test for entire URL
      return HttpConnectionExecutionCapability.builder().url(urlString).scheme(null).port(null).hostName(null).build();
    }
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapabilityForKms(String region) {
    String kmsUrl = KmsUtils.generateKmsUrl(region);
    return buildHttpConnectionExecutionCapability(kmsUrl);
  }

  private static String getHostName(URI uri) {
    if (isBlank(uri.getScheme()) && isBlank(uri.getHost())) {
      return uri.toString();
    }

    return uri.getHost();
  }
}
