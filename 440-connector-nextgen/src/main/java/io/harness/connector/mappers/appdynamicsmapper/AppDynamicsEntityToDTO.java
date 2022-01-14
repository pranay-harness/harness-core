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
