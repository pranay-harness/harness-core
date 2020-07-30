package io.harness.connector.mappers.appdynamicsmapper;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConfig;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConfigDTO;

@Singleton
public class AppDynamicsDTOToEntity implements ConnectorDTOToEntityMapper<AppDynamicsConfigDTO> {
  @Override
  public AppDynamicsConfig toConnectorEntity(AppDynamicsConfigDTO connectorDTO) {
    return AppDynamicsConfig.builder()
        .username(connectorDTO.getUsername())
        .accountname(connectorDTO.getAccountname())
        .password(connectorDTO.getPassword())
        .passwordReference(connectorDTO.getPasswordReference())
        .controllerUrl(connectorDTO.getControllerUrl())
        .accountId(connectorDTO.getAccountId())
        .build();
  }
}
