/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
