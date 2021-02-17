package io.harness.connector.mappers.artifactorymapper;

import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector.ArtifactoryConnectorBuilder;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ArtifactoryDTOToEntity
    implements ConnectorDTOToEntityMapper<ArtifactoryConnectorDTO, ArtifactoryConnector> {
  private SecretRefService secretRefService;

  @Override
  public ArtifactoryConnector toConnectorEntity(ArtifactoryConnectorDTO configDTO, NGAccess ngAccess) {
    ArtifactoryAuthType artifactoryAuthType = configDTO.getAuth().getAuthType();
    ArtifactoryConnectorBuilder artifactoryConnectorBuilder =
        ArtifactoryConnector.builder().url(configDTO.getArtifactoryServerUrl()).authType(artifactoryAuthType);
    if (artifactoryAuthType == ArtifactoryAuthType.USER_PASSWORD) {
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
          (ArtifactoryUsernamePasswordAuthDTO) configDTO.getAuth().getCredentials();
      artifactoryConnectorBuilder.artifactoryAuthentication(
          createArtifactoryAuthentication(artifactoryUsernamePasswordAuthDTO, ngAccess));
    }
    return artifactoryConnectorBuilder.build();
  }

  private ArtifactoryUserNamePasswordAuthentication createArtifactoryAuthentication(
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO, NGAccess ngAccess) {
    return ArtifactoryUserNamePasswordAuthentication.builder()
        .username(artifactoryUsernamePasswordAuthDTO.getUsername())
        .usernameRef(secretRefService.validateAndGetSecretConfigString(
            artifactoryUsernamePasswordAuthDTO.getUsernameRef(), ngAccess))
        .passwordRef(secretRefService.validateAndGetSecretConfigString(
            artifactoryUsernamePasswordAuthDTO.getPasswordRef(), ngAccess))
        .build();
  }
}
