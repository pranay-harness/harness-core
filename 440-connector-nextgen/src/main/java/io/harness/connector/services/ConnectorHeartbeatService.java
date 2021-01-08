package io.harness.connector.services;

import io.harness.delegate.beans.connector.apis.dto.ConnectorInfoDTO;
import io.harness.perpetualtask.PerpetualTaskId;

public interface ConnectorHeartbeatService {
  PerpetualTaskId createConnectorHeatbeatTask(String accountIdentifier, ConnectorInfoDTO connectorRequestDTO);
  void deletePerpetualTask(String accountIdentifier, String perpetualTaskId, String connectorFQN);
}