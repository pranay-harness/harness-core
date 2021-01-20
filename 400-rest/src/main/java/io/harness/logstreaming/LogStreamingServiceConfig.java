package io.harness.logstreaming;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._920_DELEGATE_SERVICE_BEANS)
public class LogStreamingServiceConfig {
  private String baseUrl;
  private String serviceToken;
}
