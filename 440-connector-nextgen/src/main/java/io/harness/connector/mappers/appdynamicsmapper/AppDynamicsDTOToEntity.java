package io.harness.connector.mappers.appdynamicsmapper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class AppDynamicsDTOToEntity
    implements ConnectorDTOToEntityMapper<AppDynamicsConnectorDTO, AppDynamicsConnector> {
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
