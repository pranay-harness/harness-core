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
