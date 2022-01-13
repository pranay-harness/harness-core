/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.sumologicmapper;

import io.harness.connector.entities.embedded.sumologic.SumoLogicConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.sumologic.SumoLogicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class SumoLogicEntityToDTO implements ConnectorEntityToDTOMapper<SumoLogicConnectorDTO, SumoLogicConnector> {
  @Override
  public SumoLogicConnectorDTO createConnectorDTO(SumoLogicConnector connector) {
    return SumoLogicConnectorDTO.builder()
        .url(connector.getUrl())
        .accessIdRef(SecretRefHelper.createSecretRef(connector.getAccessIdRef()))
        .accessKeyRef(SecretRefHelper.createSecretRef(connector.getAccessKeyRef()))
        .build();
  }
}
