package io.harness.ci.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CIExecutionServiceConfig {
  String addonImageTag; // Deprecated
  String liteEngineImageTag; // Deprecated
  String defaultInternalImageConnector;
  String delegateServiceEndpointVariableValue;
  Integer defaultMemoryLimit;
  Integer defaultCPULimit;
  Integer pvcDefaultStorageSize;
  String addonImage;
  String liteEngineImage;
  CIStepConfig stepConfig;
  boolean isLocal;
}
