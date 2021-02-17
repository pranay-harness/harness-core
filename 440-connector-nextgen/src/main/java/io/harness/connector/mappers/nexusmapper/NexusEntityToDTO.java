package io.harness.connector.mappers.nexusmapper;

import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NexusEntityToDTO implements ConnectorEntityToDTOMapper<NexusConnectorDTO, NexusConnector> {
  private SecretRefService secretRefService;

  @Override
  public NexusConnectorDTO createConnectorDTO(NexusConnector nexusConnector) {
    NexusAuthenticationDTO nexusAuthenticationDTO = null;
    if (nexusConnector.getAuthType() != NexusAuthType.ANONYMOUS || nexusConnector.getNexusAuthentication() != null) {
      NexusUserNamePasswordAuthentication nexusCredentials =
          (NexusUserNamePasswordAuthentication) nexusConnector.getNexusAuthentication();
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
          NexusUsernamePasswordAuthDTO.builder()
              .username(nexusCredentials.getUsername())
              .usernameRef(secretRefService.createSecretRef(nexusCredentials.getUsernameRef()))
              .passwordRef(secretRefService.createSecretRef(nexusCredentials.getPasswordRef()))
              .build();
      nexusAuthenticationDTO = NexusAuthenticationDTO.builder()
                                   .authType(nexusConnector.getAuthType())
                                   .credentials(nexusUsernamePasswordAuthDTO)
                                   .build();
    } else {
      nexusAuthenticationDTO = NexusAuthenticationDTO.builder().authType(nexusConnector.getAuthType()).build();
    }

    return NexusConnectorDTO.builder()
        .nexusServerUrl(nexusConnector.getUrl())
        .auth(nexusAuthenticationDTO)
        .version(nexusConnector.getNexusVersion())
        .build();
  }
}
