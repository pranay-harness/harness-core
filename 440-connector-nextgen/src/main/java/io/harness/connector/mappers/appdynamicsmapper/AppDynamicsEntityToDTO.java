package io.harness.connector.mappers.appdynamicsmapper;

import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.SecretRefHelper;

public class AppDynamicsEntityToDTO extends ConnectorEntityToDTOMapper<AppDynamicsConnectorDTO, AppDynamicsConnector> {
  @Override
  public AppDynamicsConnectorDTO createConnectorDTO(AppDynamicsConnector connector) {
    return AppDynamicsConnectorDTO.builder()
        .accountname(connector.getAccountname())
        .controllerUrl(connector.getControllerUrl())
        .username(connector.getUsername())
        .passwordRef(SecretRefHelper.createSecretRef(connector.getPasswordRef()))
        .accountId(connector.getAccountId())
        .build();
  }
}
