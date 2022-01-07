/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.newerlicmapper;

import io.harness.connector.entities.embedded.newrelicconnector.NewRelicConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class NewRelicDTOToEntity implements ConnectorDTOToEntityMapper<NewRelicConnectorDTO, NewRelicConnector> {
  @Override
  public NewRelicConnector toConnectorEntity(NewRelicConnectorDTO connectorDTO) {
    return NewRelicConnector.builder()
        .apiKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getApiKeyRef()))
        .newRelicAccountId(connectorDTO.getNewRelicAccountId())
        .url(connectorDTO.getUrl())
        .build();
  }
}
