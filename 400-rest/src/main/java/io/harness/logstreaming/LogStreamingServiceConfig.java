package io.harness.logstreaming;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._980_COMMONS)
public class LogStreamingServiceConfig {
  private String baseUrl;
  private String serviceToken;
}
