package io.harness.ci.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CIExecutionServiceConfig {
  String addonImageTag;
  String liteEngineImageTag;
  String defaultInternalImageConnector;
  String delegateServiceEndpointVariableValue;
  Integer defaultMemoryLimit;
  Integer defaultCPULimit;
  Integer pvcDefaultStorageSize;
}
