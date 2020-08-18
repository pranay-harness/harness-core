package io.harness.connector.mappers.appdynamicsmapper;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

@Singleton
public class AppDynamicsDTOToEntity implements ConnectorDTOToEntityMapper<AppDynamicsConnectorDTO> {
  @Override
  public AppDynamicsConnector toConnectorEntity(AppDynamicsConnectorDTO connectorDTO) {
    return AppDynamicsConnector.builder()
        .username(connectorDTO.getUsername())
        .accountname(connectorDTO.getAccountname())
        .passwordRef(SecretRefHelper.getSecretConfigString(connectorDTO.getPasswordRef()))
        .controllerUrl(connectorDTO.getControllerUrl())
        .accountId(connectorDTO.getAccountId())
        .build();
  }
}
