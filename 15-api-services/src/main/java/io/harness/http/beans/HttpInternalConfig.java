package io.harness.http.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpInternalConfig {
  String method;
  String url;
  String header;
  String body;
  int socketTimeoutMillis;
  boolean useProxy;
}
