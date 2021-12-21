package io.harness.logstreaming;

import io.harness.secret.ConfigSecret;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(makeFinal = false)
public class LogStreamingServiceConfiguration {
  private String baseUrl;
  @ConfigSecret private String serviceToken;
}
