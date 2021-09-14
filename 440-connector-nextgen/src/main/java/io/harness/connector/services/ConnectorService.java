/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.stats.ConnectorStatistics;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.DX)
public interface ConnectorService extends ConnectorCrudService, ConnectorValidationService, GitRepoConnectorService {
  boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);

  ConnectorStatistics getConnectorStatistics(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  String getHeartbeatPerpetualTaskId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  /**
   * @return Accepts a list of pairs (accountId, perpetualTaskId) and resets the perpetual task for given config
   */
  void resetHeartbeatForReferringConnectors(List<Pair<String, String>> connectorPerpetualTaskInfoList);
}
