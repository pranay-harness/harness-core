package io.harness.connector.mappers.nexusmapper;

import io.harness.connector.entities.embedded.nexusconnector.NexusConnector;
import io.harness.connector.entities.embedded.nexusconnector.NexusUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.nexusconnector.NexusAuthType;
import io.harness.delegate.beans.connector.nexusconnector.NexusConnectorDTO;
import io.harness.delegate.beans.connector.nexusconnector.NexusUsernamePasswordAuthDTO;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class NexusDTOToEntity implements ConnectorDTOToEntityMapper<NexusConnectorDTO> {
  @Override
  public NexusConnector toConnectorEntity(NexusConnectorDTO configDTO) {
    NexusAuthType nexusAuthType =
        configDTO.getAuth() != null ? configDTO.getAuth().getAuthType() : NexusAuthType.NO_AUTH;
    NexusConnector.NexusConnectorBuilder nexusConnectorBuilder = NexusConnector.builder()
                                                                     .url(configDTO.getNexusServerUrl())
                                                                     .nexusVersion(configDTO.getVersion())
                                                                     .authType(nexusAuthType);
    if (nexusAuthType == NexusAuthType.USER_PASSWORD) {
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO =
          (NexusUsernamePasswordAuthDTO) configDTO.getAuth().getCredentials();
      nexusConnectorBuilder.nexusAuthentication(createNexusAuthentication(nexusUsernamePasswordAuthDTO));
    }
    return nexusConnectorBuilder.build();
  }

  @Override
  public List<ConnectorCategory> getConnectorCategory() {
    return Collections.singletonList(ConnectorCategory.ARTIFACTORY);
  }

  private NexusUserNamePasswordAuthentication createNexusAuthentication(
      NexusUsernamePasswordAuthDTO nexusUsernamePasswordAuthDTO) {
    return NexusUserNamePasswordAuthentication.builder()
        .username(nexusUsernamePasswordAuthDTO.getUsername())
        .passwordRef(SecretRefHelper.getSecretConfigString(nexusUsernamePasswordAuthDTO.getPasswordRef()))
        .build();
  }
}
