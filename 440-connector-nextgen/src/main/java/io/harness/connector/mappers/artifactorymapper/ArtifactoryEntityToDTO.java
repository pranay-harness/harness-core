package io.harness.connector.mappers.artifactorymapper;

import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryConnector;
import io.harness.connector.entities.embedded.artifactoryconnector.ArtifactoryUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class ArtifactoryEntityToDTO
    implements ConnectorEntityToDTOMapper<ArtifactoryConnectorDTO, ArtifactoryConnector> {
  private SecretRefService secretRefService;

  @Override
  public ArtifactoryConnectorDTO createConnectorDTO(ArtifactoryConnector artifactoryConnector) {
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = null;
    if (artifactoryConnector.getAuthType() != ArtifactoryAuthType.ANONYMOUS
        || artifactoryConnector.getArtifactoryAuthentication() != null) {
      ArtifactoryUserNamePasswordAuthentication artifactoryCredentials =
          (ArtifactoryUserNamePasswordAuthentication) artifactoryConnector.getArtifactoryAuthentication();
      ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
          ArtifactoryUsernamePasswordAuthDTO.builder()
              .username(artifactoryCredentials.getUsername())
              .usernameRef(secretRefService.createSecretRef(artifactoryCredentials.getUsernameRef()))
              .passwordRef(secretRefService.createSecretRef(artifactoryCredentials.getPasswordRef()))
              .build();
      artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder()
                                         .authType(artifactoryConnector.getAuthType())
                                         .credentials(artifactoryUsernamePasswordAuthDTO)
                                         .build();
    } else {
      artifactoryAuthenticationDTO =
          ArtifactoryAuthenticationDTO.builder().authType(artifactoryConnector.getAuthType()).build();
    }

    return ArtifactoryConnectorDTO.builder()
        .artifactoryServerUrl(artifactoryConnector.getUrl())
        .auth(artifactoryAuthenticationDTO)
        .build();
  }
}
