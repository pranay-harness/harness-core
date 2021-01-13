package io.harness.connector.mappers.artifactorymapper;

import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector.ArtifactoryConnectorBuilder;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
public class ArtifactoryDTOToEntity extends ConnectorDTOToEntityMapper<ArtifactoryConnectorDTO, ArtifactoryConnector> {
  @Override
  public ArtifactoryConnector toConnectorEntity(ArtifactoryConnectorDTO configDTO) {
    ArtifactoryAuthType artifactoryAuthType =
        configDTO.getAuth() != null ? configDTO.getAuth().getAuthType() : ArtifactoryAuthType.NO_AUTH;
    ArtifactoryConnectorBuilder artifactoryConnectorBuilder =
        ArtifactoryConnector.builder().url(configDTO.getArtifactoryServerUrl()).authType(artifactoryAuthType);
    if (artifactoryAuthType == ArtifactoryAuthType.USER_PASSWORD) {
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
          (ArtifactoryUsernamePasswordAuthDTO) configDTO.getAuth().getCredentials();
      artifactoryConnectorBuilder.artifactoryAuthentication(
          createArtifactoryAuthentication(artifactoryUsernamePasswordAuthDTO));
    }
    return artifactoryConnectorBuilder.build();
  }

  private ArtifactoryUserNamePasswordAuthentication createArtifactoryAuthentication(
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO) {
    return ArtifactoryUserNamePasswordAuthentication.builder()
        .username(artifactoryUsernamePasswordAuthDTO.getUsername())
        .passwordRef(SecretRefHelper.getSecretConfigString(artifactoryUsernamePasswordAuthDTO.getPasswordRef()))
        .build();
  }
}
