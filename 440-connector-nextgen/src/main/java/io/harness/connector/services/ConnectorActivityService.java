/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.services;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.ng.core.activityhistory.NGActivityType;

public interface ConnectorActivityService {
  void create(String accountIdentifier, ConnectorInfoDTO connector, NGActivityType ngActivityType);
  void deleteAllActivities(String accountIdentifier, String connectorFQN);
}
