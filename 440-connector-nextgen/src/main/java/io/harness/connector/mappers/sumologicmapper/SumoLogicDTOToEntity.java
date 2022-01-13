/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.sumologicmapper;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.sumologic.SumoLogicConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.sumologic.SumoLogicConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(CV)
public class SumoLogicDTOToEntity implements ConnectorDTOToEntityMapper<SumoLogicConnectorDTO, SumoLogicConnector> {
  @Override
  public SumoLogicConnector toConnectorEntity(SumoLogicConnectorDTO connectorDTO) {
    return SumoLogicConnector.builder()
        .url(connectorDTO.getUrl())
        .accessIdRef(SecretRefHelper.getSecretConfigString(connectorDTO.getAccessIdRef()))
        .accessKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getAccessKeyRef()))
        .build();
  }
}
