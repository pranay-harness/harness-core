package io.harness.pms.preflight.connector;

import io.harness.pms.preflight.PreFlightStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorWrapperResponse {
  List<ConnectorCheckResponse> checkResponses;
  PreFlightStatus status;
  String label;
}
