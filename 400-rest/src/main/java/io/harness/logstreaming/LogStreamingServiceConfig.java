package io.harness.logstreaming;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LogStreamingServiceConfig {
  private String baseUrl;
  private String serviceToken;
}
