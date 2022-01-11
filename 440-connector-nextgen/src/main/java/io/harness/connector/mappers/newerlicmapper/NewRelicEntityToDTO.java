/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.newerlicmapper;

import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class NewRelicEntityToDTO implements ConnectorEntityToDTOMapper<NewRelicConnectorDTO, NewRelicConnector> {
  @Override
  public NewRelicConnectorDTO createConnectorDTO(NewRelicConnector connector) {
    return NewRelicConnectorDTO.builder()
        .newRelicAccountId(connector.getNewRelicAccountId())
        .apiKeyRef(SecretRefHelper.createSecretRef(connector.getApiKeyRef()))
        .url(connector.getUrl())
        .build();
  }
}
