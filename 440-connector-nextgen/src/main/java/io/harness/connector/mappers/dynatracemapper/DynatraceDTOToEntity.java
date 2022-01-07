/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.dynatracemapper;

import io.harness.connector.entities.embedded.dynatraceconnector.DynatraceConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class DynatraceDTOToEntity implements ConnectorDTOToEntityMapper<DynatraceConnectorDTO, DynatraceConnector> {
  @Override
  public DynatraceConnector toConnectorEntity(DynatraceConnectorDTO connectorDTO) {
    return DynatraceConnector.builder()
        .url(connectorDTO.getUrl())
        .apiTokenRef(SecretRefHelper.getSecretConfigString(connectorDTO.getApiTokenRef()))
        .build();
  }
}
