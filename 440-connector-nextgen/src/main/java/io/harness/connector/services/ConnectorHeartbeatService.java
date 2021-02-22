package io.harness.connector.services;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.perpetualtask.PerpetualTaskId;

public interface ConnectorHeartbeatService {
  PerpetualTaskId createConnectorHeatbeatTask(String accountIndentifier, String connectorOrgIdentifier,
      String connectorProjectIdentifier, String connectorIdentifier);

  boolean isHarnessManagedSecretManager(ConnectorInfoDTO connector);

  boolean deletePerpetualTask(String accountIdentifier, String perpetualTaskId, String connectorFQN);
  ConnectorValidationParams getConnectorValidationParams(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}