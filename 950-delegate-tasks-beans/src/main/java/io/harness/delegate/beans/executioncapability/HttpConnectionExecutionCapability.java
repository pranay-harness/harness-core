/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.executioncapability;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.KeyValuePair;

import java.time.Duration;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.apache.http.client.utils.URIBuilder;

@Value
@Builder
public class HttpConnectionExecutionCapability implements ExecutionCapability {
  private final CapabilityType capabilityType = CapabilityType.HTTP;

  private String url;
  private List<KeyValuePair> headers;

  private String host;
  private String scheme;
  private int port;
  private String path;
  private String query;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    if (url != null) {
      return url;
    }
    URIBuilder uriBuilder = new URIBuilder();
    if (isNotBlank(scheme)) {
      uriBuilder.setScheme(scheme);
    }
    uriBuilder.setHost(host);
    if (port != -1) {
      uriBuilder.setPort(port);
    }
    if (isNotBlank(path)) {
      uriBuilder.setPath('/' + path);
    }
    if (isNotBlank(query)) {
      uriBuilder.setCustomQuery(query);
    }
    if (isNotEmpty(headers)) {
      for (KeyValuePair entry : headers) {
        uriBuilder.setParameter(entry.getKey(), entry.getValue());
      }
    }
    return uriBuilder.toString();
  }

  // This is used when capability basis and URL which is tested for connectivity are different.
  // Eg. When headers are included in the request, URL should remain unchanged.
  public String fetchConnectableUrl() {
    if (url != null) {
      return url;
    }
    URIBuilder uriBuilder = new URIBuilder();
    if (isNotBlank(scheme)) {
      uriBuilder.setScheme(scheme);
    }
    uriBuilder.setHost(host);
    if (port != -1) {
      uriBuilder.setPort(port);
    }
    if (isNotBlank(path)) {
      uriBuilder.setPath('/' + path);
    }
    if (isNotBlank(query)) {
      uriBuilder.setCustomQuery(query);
    }
    return uriBuilder.toString();
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
