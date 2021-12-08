package io.harness.logstreaming;

import io.harness.secret.ConfigSecret;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(makeFinal = false)
@Builder
public class LogStreamingServiceConfig {
  private String baseUrl;
  @ConfigSecret private String serviceToken;
}
