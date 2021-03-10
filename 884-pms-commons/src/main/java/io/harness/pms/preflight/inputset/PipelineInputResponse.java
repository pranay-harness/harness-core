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
