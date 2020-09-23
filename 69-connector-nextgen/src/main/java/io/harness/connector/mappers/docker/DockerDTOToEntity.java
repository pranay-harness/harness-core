package io.harness.connector.mappers.docker;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerConnector.DockerConnectorBuilder;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;

import java.util.Collections;

@Singleton
public class DockerDTOToEntity implements ConnectorDTOToEntityMapper<DockerConnectorDTO> {
  @Override
  public DockerConnector toConnectorEntity(DockerConnectorDTO configDTO) {
    DockerAuthType dockerAuthType =
        configDTO.getAuth() != null ? configDTO.getAuth().getAuthType() : DockerAuthType.NO_AUTH;
    DockerConnectorBuilder dockerConnectorBuilder =
        DockerConnector.builder().url(configDTO.getDockerRegistryUrl()).authType(dockerAuthType);
    if (dockerAuthType == DockerAuthType.USER_PASSWORD) {
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
          (DockerUserNamePasswordDTO) configDTO.getAuth().getCredentials();
      dockerConnectorBuilder.dockerAuthentication(createDockerAuthentication(dockerUserNamePasswordDTO));
    }

    DockerConnector dockerConnector = dockerConnectorBuilder.build();
    dockerConnector.setCategories(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER));
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
