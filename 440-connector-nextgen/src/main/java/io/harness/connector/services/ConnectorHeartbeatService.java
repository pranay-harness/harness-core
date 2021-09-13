package io.harness.connector.services;

import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.perpetualtask.PerpetualTaskId;

import java.util.Optional;

public interface ConnectorHeartbeatService {
  PerpetualTaskId createConnectorHeatbeatTask(String accountIndentifier, String connectorOrgIdentifier,
      String connectorProjectIdentifier, String connectorIdentifier);
  boolean deletePerpetualTask(String accountIdentifier, String perpetualTaskId, String connectorFQN);
  Optional<ConnectorValidationParams> getConnectorValidationParams(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
  void resetPerpetualTask(String accountIdentifier, String perpetualTaskId);
}