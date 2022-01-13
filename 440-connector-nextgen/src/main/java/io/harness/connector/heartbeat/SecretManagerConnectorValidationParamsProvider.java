/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.heartbeat;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.services.NGConnectorSecretManagerService;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;

import com.google.inject.Inject;

@OwnedBy(PL)
public class SecretManagerConnectorValidationParamsProvider {
  @Inject NGConnectorSecretManagerService ngConnectorSecretManagerService;

  public ConnectorConfigDTO getDecryptedConnectorConfigDTO(
      ConnectorInfoDTO connectorConfigDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ConnectorDTO connectorDTO = ngConnectorSecretManagerService.decrypt(accountIdentifier, orgIdentifier,
        projectIdentifier, ConnectorDTO.builder().connectorInfo(connectorConfigDTO).build());
    return connectorDTO.getConnectorInfo().getConnectorConfig();
  }
}
