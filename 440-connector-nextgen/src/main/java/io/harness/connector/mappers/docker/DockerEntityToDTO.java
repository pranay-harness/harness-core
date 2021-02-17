package io.harness.connector.mappers.docker;

import static io.harness.delegate.beans.connector.docker.DockerAuthType.ANONYMOUS;

import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class DockerEntityToDTO implements ConnectorEntityToDTOMapper<DockerConnectorDTO, DockerConnector> {
  private SecretRefService secretRefService;

  @Override
  public DockerConnectorDTO createConnectorDTO(DockerConnector dockerConnector) {
    DockerAuthenticationDTO dockerAuthenticationDTO = null;
    if (dockerConnector.getAuthType() != ANONYMOUS || dockerConnector.getDockerAuthentication() != null) {
      DockerUserNamePasswordAuthentication dockerCredentials =
          (DockerUserNamePasswordAuthentication) dockerConnector.getDockerAuthentication();
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
          DockerUserNamePasswordDTO.builder()
              .username(dockerCredentials.getUsername())
              .usernameRef(secretRefService.createSecretRef(dockerCredentials.getUsernameRef()))
              .passwordRef(secretRefService.createSecretRef(dockerCredentials.getPasswordRef()))
              .build();
      dockerAuthenticationDTO = DockerAuthenticationDTO.builder()
                                    .authType(dockerConnector.getAuthType())
                                    .credentials(dockerUserNamePasswordDTO)
                                    .build();
    } else {
      dockerAuthenticationDTO = DockerAuthenticationDTO.builder().authType(ANONYMOUS).build();
    }

    return DockerConnectorDTO.builder()
        .dockerRegistryUrl(dockerConnector.getUrl())
        .providerType(dockerConnector.getProviderType())
        .auth(dockerAuthenticationDTO)
        .build();
  }
}
