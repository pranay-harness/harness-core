/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.servicenow;

import io.harness.connector.entities.embedded.servicenow.ServiceNowConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class ServiceNowEntityToDTO implements ConnectorEntityToDTOMapper<ServiceNowConnectorDTO, ServiceNowConnector> {
  @Override
  public ServiceNowConnectorDTO createConnectorDTO(ServiceNowConnector connector) {
    return ServiceNowConnectorDTO.builder()
        .serviceNowUrl(connector.getServiceNowUrl())
        .username(connector.getUsername())
        .usernameRef(SecretRefHelper.createSecretRef(connector.getUsernameRef()))
        .passwordRef(SecretRefHelper.createSecretRef(connector.getPasswordRef()))
        .build();
  }
}
