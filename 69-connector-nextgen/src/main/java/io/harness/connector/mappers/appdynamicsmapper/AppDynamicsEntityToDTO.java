package io.harness.connector.mappers.appdynamicsmapper;

import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;

public class AppDynamicsEntityToDTO implements ConnectorEntityToDTOMapper<AppDynamicsConnector> {
  @Override
  public AppDynamicsConnectorDTO createConnectorDTO(AppDynamicsConnector connector) {
    return AppDynamicsConnectorDTO.builder()
        .accountname(connector.getAccountname())
        .controllerUrl(connector.getControllerUrl())
        .username(connector.getUsername())
        .passwordReference(connector.getPasswordReference())
        .accountId(connector.getAccountId())
        .build();
  }
}
