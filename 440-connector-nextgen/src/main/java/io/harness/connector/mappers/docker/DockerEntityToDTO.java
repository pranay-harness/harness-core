package io.harness.connector.mappers.docker;

import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;

import com.google.inject.Singleton;

@Singleton
public class DockerEntityToDTO implements ConnectorEntityToDTOMapper<DockerConnector> {
  @Override
  public DockerConnectorDTO createConnectorDTO(DockerConnector dockerConnector) {
    DockerAuthenticationDTO dockerAuthenticationDTO = null;
    if (dockerConnector.getAuthType() != DockerAuthType.NO_AUTH || dockerConnector.getDockerAuthentication() != null) {
      DockerUserNamePasswordAuthentication dockerCredentials =
          (DockerUserNamePasswordAuthentication) dockerConnector.getDockerAuthentication();
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
          DockerUserNamePasswordDTO.builder()
              .username(dockerCredentials.getUsername())
              .passwordRef(SecretRefHelper.createSecretRef(dockerCredentials.getPasswordRef()))
              .build();
      dockerAuthenticationDTO = DockerAuthenticationDTO.builder()
                                    .authType(dockerConnector.getAuthType())
                                    .credentials(dockerUserNamePasswordDTO)
                                    .build();
    }

    return DockerConnectorDTO.builder()
        .dockerRegistryUrl(dockerConnector.getUrl())
        .auth(dockerAuthenticationDTO)
        .build();
  }
}
