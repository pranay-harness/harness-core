package io.harness.pms.preflight;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.preflight.connector.ConnectorWrapperResponse;
import io.harness.pms.preflight.inputset.PipelineWrapperResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
@Schema(name = "PreFlightDTO", description = "This contains the response of a Preflight Check for a Pipeline.")
public class PreFlightDTO {
  PipelineWrapperResponse pipelineInputWrapperResponse;
  ConnectorWrapperResponse connectorWrapperResponse;
  PreFlightStatus status;
  PreFlightErrorInfo errorInfo;
}
