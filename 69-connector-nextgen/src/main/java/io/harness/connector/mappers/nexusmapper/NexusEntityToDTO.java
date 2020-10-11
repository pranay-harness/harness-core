package io.harness.connector.mappers.nexusmapper;

import com.google.inject.Singleton;
import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthenticationDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;

@Singleton
public class NexusEntityToDTO implements ConnectorEntityToDTOMapper<NexusConnector> {
  @Override
  public NexusConnectorDTO createConnectorDTO(NexusConnector nexusConnector) {
    NexusAuthenticationDTO nexusAuthenticationDTO = null;
    if (nexusConnector.getAuthType() != NexusAuthType.NO_AUTH || nexusConnector.getNexusAuthentication() != null) {
      NexusUserNamePasswordAuthentication nexusCredentials =
          (NexusUserNamePasswordAuthentication) nexusConnector.getNexusAuthentication();
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
          NexusUsernamePasswordAuthDTO.builder()
              .username(nexusCredentials.getUsername())
              .passwordRef(SecretRefHelper.createSecretRef(nexusCredentials.getPasswordRef()))
              .build();
      nexusAuthenticationDTO = NexusAuthenticationDTO.builder()
                                   .authType(nexusConnector.getAuthType())
                                   .credentials(nexusUsernamePasswordAuthDTO)
                                   .build();
    }

    return NexusConnectorDTO.builder()
        .nexusServerUrl(nexusConnector.getUrl())
        .auth(nexusAuthenticationDTO)
        .version(nexusConnector.getNexusVersion())
        .build();
  }
}