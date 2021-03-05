package io.harness.logstreaming;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._980_COMMONS)
public class LogStreamingServiceConfiguration {
  private String baseUrl;
  private String serviceToken;
}
