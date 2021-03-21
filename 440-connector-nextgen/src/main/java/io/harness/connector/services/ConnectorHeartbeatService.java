package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.perpetualtask.PerpetualTaskId;

@OwnedBy(DX)
public interface ConnectorHeartbeatService {
  PerpetualTaskId createConnectorHeatbeatTask(String accountIndentifier, String connectorOrgIdentifier,
      String connectorProjectIdentifier, String connectorIdentifier);
  boolean deletePerpetualTask(String accountIdentifier, String perpetualTaskId, String connectorFQN);
  ConnectorValidationParams getConnectorValidationParams(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
}