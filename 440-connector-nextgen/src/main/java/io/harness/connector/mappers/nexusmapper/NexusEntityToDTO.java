package io.harness.connector.mappers.nexusmapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class NexusEntityToDTO implements ConnectorEntityToDTOMapper<NexusConnectorDTO, NexusConnector> {
  @Override
  public NexusConnectorDTO createConnectorDTO(NexusConnector nexusConnector) {
    NexusAuthenticationDTO nexusAuthenticationDTO = null;
    if (nexusConnector.getAuthType() != NexusAuthType.ANONYMOUS || nexusConnector.getNexusAuthentication() != null) {
      NexusUserNamePasswordAuthentication nexusCredentials =
          (NexusUserNamePasswordAuthentication) nexusConnector.getNexusAuthentication();
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
          NexusUsernamePasswordAuthDTO.builder()
              .username(nexusCredentials.getUsername())
              .usernameRef(SecretRefHelper.createSecretRef(nexusCredentials.getUsernameRef()))
              .passwordRef(SecretRefHelper.createSecretRef(nexusCredentials.getPasswordRef()))
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
