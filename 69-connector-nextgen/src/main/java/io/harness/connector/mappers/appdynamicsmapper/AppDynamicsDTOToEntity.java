package io.harness.connector.mappers.appdynamicsmapper;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

@Singleton
public class AppDynamicsDTOToEntity implements ConnectorDTOToEntityMapper<AppDynamicsConnectorDTO> {
  @Override
  public AppDynamicsConnector toConnectorEntity(AppDynamicsConnectorDTO connectorDTO) {
    return AppDynamicsConnector.builder()
        .username(connectorDTO.getUsername())
        .accountname(connectorDTO.getAccountname())
        .password(connectorDTO.getPassword())
        .passwordReference(connectorDTO.getPasswordReference())
        .controllerUrl(connectorDTO.getControllerUrl())
        .accountId(connectorDTO.getAccountId())
        .build();
  }
}
