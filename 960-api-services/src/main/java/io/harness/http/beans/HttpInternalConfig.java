package io.harness.http.beans;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpInternalConfig {
  String method;
  String url;
  String header;
  Map<String, String> requestHeaders;
  String body;
  int socketTimeoutMillis;
  boolean useProxy;
  boolean isCertValidationRequired;
}
