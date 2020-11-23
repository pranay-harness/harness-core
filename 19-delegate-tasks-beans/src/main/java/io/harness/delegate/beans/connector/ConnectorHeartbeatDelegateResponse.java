package io.harness.delegate.beans.connector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorHeartbeatDelegateResponse {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String identifier;
  String name;
  ConnectorValidationResult connectorValidationResult;
}
