package io.harness.connector.entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorConnectivityDetails {
  ConnectivityStatus status;
  String errorMessage;
}
