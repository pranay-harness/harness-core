package io.harness.connector.mappers.nexusmapper;

import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector.NexusConnectorBuilder;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NexusDTOToEntity implements ConnectorDTOToEntityMapper<NexusConnectorDTO, NexusConnector> {
  private SecretRefService secretRefService;

  @Override
  public NexusConnector toConnectorEntity(NexusConnectorDTO configDTO, NGAccess ngAccess) {
    NexusAuthType nexusAuthType = configDTO.getAuth().getAuthType();
    NexusConnectorBuilder nexusConnectorBuilder = NexusConnector.builder()
                                                      .url(configDTO.getNexusServerUrl())
                                                      .nexusVersion(configDTO.getVersion())
                                                      .authType(nexusAuthType);
    if (nexusAuthType == NexusAuthType.USER_PASSWORD) {
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
          (NexusUsernamePasswordAuthDTO) configDTO.getAuth().getCredentials();
      nexusConnectorBuilder.nexusAuthentication(createNexusAuthentication(nexusUsernamePasswordAuthDTO, ngAccess));
    }
    return nexusConnectorBuilder.build();
  }

  private NexusUserNamePasswordAuthentication createNexusAuthentication(
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO, NGAccess ngAccess) {
    return NexusUserNamePasswordAuthentication.builder()
        .username(nexusUsernamePasswordAuthDTO.getUsername())
        .usernameRef(
            secretRefService.validateAndGetSecretConfigString(nexusUsernamePasswordAuthDTO.getUsernameRef(), ngAccess))
        .passwordRef(
            secretRefService.validateAndGetSecretConfigString(nexusUsernamePasswordAuthDTO.getPasswordRef(), ngAccess))
        .build();
  }
}
