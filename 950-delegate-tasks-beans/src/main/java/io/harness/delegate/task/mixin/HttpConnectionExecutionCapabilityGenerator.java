package io.harness.delegate.task.mixin;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.task.utils.KmsUtils;
import io.harness.expression.DummySubstitutor;
import io.harness.expression.ExpressionEvaluator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class HttpConnectionExecutionCapabilityGenerator {
  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapability(
      String urlString, ExpressionEvaluator maskingEvaluator) {
    return buildHttpConnectionExecutionCapability(urlString, HttpCapabilityDetailsLevel.PATH, maskingEvaluator);
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapabilityForKms(
      String region, ExpressionEvaluator maskingEvaluator) {
    String kmsUrl = KmsUtils.generateKmsUrl(region);
    return buildHttpConnectionExecutionCapability(kmsUrl, maskingEvaluator);
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapability(
      String urlString, HttpCapabilityDetailsLevel level, ExpressionEvaluator maskingEvaluator) {
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
      log.error("conversion to java.net.URI failed for url: {}", maskedUrlString(maskingEvaluator, urlString), e);
    }
    // This is falling back to existing approach, where we test for entire URL
    return HttpConnectionExecutionCapability.builder().url(maskedUrlString(maskingEvaluator, urlString)).build();
  }

  private static String maskedUrlString(ExpressionEvaluator maskingEvaluator, String urlString) {
    if (maskingEvaluator == null) {
      return urlString;
    }
    return maskingEvaluator.substitute(urlString, Collections.emptyMap());
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
