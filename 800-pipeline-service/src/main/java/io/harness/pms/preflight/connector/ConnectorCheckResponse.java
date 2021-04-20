package io.harness.pms.preflight.connector;

import io.harness.pms.preflight.PreFlightEntityErrorInfo;
import io.harness.pms.preflight.PreFlightStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorCheckResponse {
  String connectorIdentifier;
  PreFlightEntityErrorInfo errorInfo;
  String fqn;
  String stageName;
  String stageIdentifier;
  String stepName;
  String stepIdentifier;
  PreFlightStatus status;
}
