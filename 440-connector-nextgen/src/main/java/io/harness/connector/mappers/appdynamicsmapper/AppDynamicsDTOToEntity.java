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
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CV)
public class AppDynamicsDTOToEntity
    implements ConnectorDTOToEntityMapper<AppDynamicsConnectorDTO, AppDynamicsConnector> {
  @Override
  public AppDynamicsConnector toConnectorEntity(AppDynamicsConnectorDTO connectorDTO) {
    return AppDynamicsConnector.builder()
        .username(connectorDTO.getUsername())
        .accountname(connectorDTO.getAccountname())
        .passwordRef(SecretRefHelper.getSecretConfigString(connectorDTO.getPasswordRef()))
        .controllerUrl(connectorDTO.getControllerUrl())
        .clientId(connectorDTO.getClientId())
        .clientSecret(SecretRefHelper.getSecretConfigString(connectorDTO.getClientSecretRef()))
        .authType(connectorDTO.getAuthType())
        .build();
  }
}
