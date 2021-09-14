/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.preflight;

import io.harness.pms.preflight.connector.ConnectorWrapperResponse;
import io.harness.pms.preflight.inputset.PipelineWrapperResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreFlightDTO {
  PipelineWrapperResponse pipelineInputWrapperResponse;
  ConnectorWrapperResponse connectorWrapperResponse;
  PreFlightStatus status;
  PreFlightErrorInfo errorInfo;
}
