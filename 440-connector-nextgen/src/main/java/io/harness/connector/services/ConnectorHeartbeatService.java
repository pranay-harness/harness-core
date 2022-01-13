/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.services;

import io.harness.delegate.beans.connector.ConnectorValidationParameterResponse;
import io.harness.perpetualtask.PerpetualTaskId;

public interface ConnectorHeartbeatService {
  PerpetualTaskId createConnectorHeatbeatTask(String accountIndentifier, String connectorOrgIdentifier,
      String connectorProjectIdentifier, String connectorIdentifier);
  boolean deletePerpetualTask(String accountIdentifier, String perpetualTaskId, String connectorFQN);
  ConnectorValidationParameterResponse getConnectorValidationParams(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
  void resetPerpetualTask(String accountIdentifier, String perpetualTaskId);
}
