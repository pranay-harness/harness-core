package io.harness.logstreaming;

import io.harness.expression.ConfigSecret;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LogStreamingServiceConfig {
  private String baseUrl;
  @ConfigSecret private String serviceToken;
}
