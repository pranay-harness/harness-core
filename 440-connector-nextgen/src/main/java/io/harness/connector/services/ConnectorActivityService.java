package io.harness.connector.services;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.ng.core.activityhistory.NGActivityType;

@OwnedBy(DX)
public interface ConnectorActivityService {
  void create(String accountIdentifier, ConnectorInfoDTO connector, NGActivityType ngActivityType);
  void deleteAllActivities(String accountIdentifier, String connectorFQN);
}
