package io.harness.connector.services;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.ng.core.activityhistory.NGActivityType;

public interface ConnectorActivityService {
  void create(String accountIdentifier, ConnectorInfoDTO connector, NGActivityType ngActivityType);
  void deleteAllActivities(String accountIdentifier, String connectorFQN);
}
