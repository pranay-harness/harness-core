package io.harness.connector.mappers.docker;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;

@Singleton
public class DockerEntityToDTO implements ConnectorEntityToDTOMapper<DockerConnector> {
  @Override
  public DockerConnectorDTO createConnectorDTO(DockerConnector dockerConnector) {
    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        DockerUserNamePasswordDTO.builder()
            .username(dockerConnector.getUsername())
            .passwordRef(SecretRefHelper.createSecretRef(dockerConnector.getPasswordRef()))
            .build();
    DockerAuthenticationDTO dockerAuthenticationDTO = DockerAuthenticationDTO.builder()
                                                          .authType(dockerConnector.getAuthType())
                                                          .credentials(dockerUserNamePasswordDTO)
                                                          .build();
    return DockerConnectorDTO.builder().url(dockerConnector.getUrl()).authScheme(dockerAuthenticationDTO).build();
  }
}
