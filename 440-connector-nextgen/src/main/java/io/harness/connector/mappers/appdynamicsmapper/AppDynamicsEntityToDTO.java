/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.appdynamicsmapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsAuthType;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.SecretRefHelper;

@OwnedBy(HarnessTeam.CV)
public class AppDynamicsEntityToDTO
    implements ConnectorEntityToDTOMapper<AppDynamicsConnectorDTO, AppDynamicsConnector> {
  @Override
  public AppDynamicsConnectorDTO createConnectorDTO(AppDynamicsConnector connector) {
    return AppDynamicsConnectorDTO.builder()
        .accountname(connector.getAccountname())
        .controllerUrl(connector.getControllerUrl())
        .username(connector.getUsername())
        .passwordRef(SecretRefHelper.createSecretRef(connector.getPasswordRef()))
        .authType(connector.getAuthType() == null ? AppDynamicsAuthType.USERNAME_PASSWORD : connector.getAuthType())
        .clientId(connector.getClientId())
        .clientSecretRef(SecretRefHelper.createSecretRef(connector.getClientSecret()))
        .build();
  }
}
