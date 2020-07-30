package io.harness.connector.mappers.appdynamicsmapper;

import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConfig;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConfigDTO;

public class AppDynamicsEntityToDTO implements ConnectorEntityToDTOMapper<AppDynamicsConfig> {
  @Override
  public AppDynamicsConfigDTO createConnectorDTO(AppDynamicsConfig connector) {
    return AppDynamicsConfigDTO.builder()
        .accountname(connector.getAccountname())
        .controllerUrl(connector.getControllerUrl())
        .username(connector.getUsername())
        .passwordReference(connector.getPasswordReference())
        .accountId(connector.getAccountId())
        .build();
  }
}
