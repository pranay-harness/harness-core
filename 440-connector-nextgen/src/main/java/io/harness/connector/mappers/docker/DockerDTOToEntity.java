package io.harness.connector.mappers.docker;

import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerConnector.DockerConnectorBuilder;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
public class DockerDTOToEntity extends ConnectorDTOToEntityMapper<DockerConnectorDTO, DockerConnector> {
  @Override
  public DockerConnector toConnectorEntity(DockerConnectorDTO configDTO) {
    DockerAuthType dockerAuthType = configDTO.getAuth().getAuthType();
    DockerConnectorBuilder dockerConnectorBuilder = DockerConnector.builder()
                                                        .url(configDTO.getDockerRegistryUrl())
                                                        .providerType(configDTO.getProviderType())
                                                        .authType(dockerAuthType);
    if (dockerAuthType == DockerAuthType.USER_PASSWORD) {
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
          (DockerUserNamePasswordDTO) configDTO.getAuth().getCredentials();
      dockerConnectorBuilder.dockerAuthentication(createDockerAuthentication(dockerUserNamePasswordDTO));
    }

    DockerConnector dockerConnector = dockerConnectorBuilder.build();
    dockerConnector.setType(ConnectorType.DOCKER);
    return dockerConnector;
  }

  private DockerUserNamePasswordAuthentication createDockerAuthentication(
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO) {
    return DockerUserNamePasswordAuthentication.builder()
        .username(dockerUserNamePasswordDTO.getUsername())
        .passwordRef(SecretRefHelper.getSecretConfigString(dockerUserNamePasswordDTO.getPasswordRef()))
        .build();
  }
}
