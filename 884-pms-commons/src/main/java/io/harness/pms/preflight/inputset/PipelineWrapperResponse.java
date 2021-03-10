package io.harness.pms.preflight.inputset;

import io.harness.pms.preflight.PreFlightStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipelineWrapperResponse {
  List<PipelineInputResponse> pipelineInputResponse;
  PreFlightStatus status;
  String label;
}
