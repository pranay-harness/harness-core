package io.harness.connector.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorConnectivityDetails {
  ConnectivityStatus status;
  String errorMessage;
  long lastTestedAt;
  long lastConnectedAt;
}
