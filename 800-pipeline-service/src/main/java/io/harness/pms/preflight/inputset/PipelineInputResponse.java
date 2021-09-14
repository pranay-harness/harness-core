/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.preflight.inputset;

import io.harness.pms.preflight.PreFlightEntityErrorInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineInputResponse {
  PreFlightEntityErrorInfo errorInfo;
  boolean success;
  String fqn;
  String stageName;
  String stepName;
}
