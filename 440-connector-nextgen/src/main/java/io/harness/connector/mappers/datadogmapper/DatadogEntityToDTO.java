/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.datadogmapper;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.datadogconnector.DatadogConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.datadog.DatadogConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(CV)
public class DatadogEntityToDTO implements ConnectorEntityToDTOMapper<DatadogConnectorDTO, DatadogConnector> {
  @Override
  public DatadogConnectorDTO createConnectorDTO(DatadogConnector connector) {
    return DatadogConnectorDTO.builder()
        .url(connector.getUrl())
        .apiKeyRef(SecretRefHelper.createSecretRef(connector.getApiKeyRef()))
        .applicationKeyRef(SecretRefHelper.createSecretRef(connector.getApplicationKeyRef()))
        .build();
  }
}
