package io.harness.connector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "ConnectorConnectivityDetailsKeys")
public class ConnectorConnectivityDetails {
  ConnectivityStatus status;
  String errorMessage;
  long lastTestedAt;
  long lastConnectedAt;
}
