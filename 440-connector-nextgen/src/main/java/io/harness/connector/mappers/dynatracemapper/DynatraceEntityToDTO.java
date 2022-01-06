/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.dynatracemapper;

import io.harness.connector.entities.embedded.dynatraceconnector.DynatraceConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.dynatrace.DynatraceConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class DynatraceEntityToDTO implements ConnectorEntityToDTOMapper<DynatraceConnectorDTO, DynatraceConnector> {
  @Override
  public DynatraceConnectorDTO createConnectorDTO(DynatraceConnector connector) {
    return DynatraceConnectorDTO.builder()
        .url(connector.getUrl())
        .apiTokenRef(SecretRefHelper.createSecretRef(connector.getApiTokenRef()))
        .build();
  }
}
