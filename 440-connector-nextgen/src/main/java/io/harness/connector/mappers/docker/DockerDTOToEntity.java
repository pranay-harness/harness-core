package io.harness.connector.mappers.docker;

import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerConnector.DockerConnectorBuilder;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.ng.core.NGAccess;
import io.harness.ng.service.SecretRefService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class DockerDTOToEntity implements ConnectorDTOToEntityMapper<DockerConnectorDTO, DockerConnector> {
  private SecretRefService secretRefService;

  @Override
  public DockerConnector toConnectorEntity(DockerConnectorDTO configDTO, NGAccess ngAccess) {
    DockerAuthType dockerAuthType = configDTO.getAuth().getAuthType();
    DockerConnectorBuilder dockerConnectorBuilder = DockerConnector.builder()
                                                        .url(configDTO.getDockerRegistryUrl())
                                                        .providerType(configDTO.getProviderType())
                                                        .authType(dockerAuthType);
    if (dockerAuthType == DockerAuthType.USER_PASSWORD) {
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
          (DockerUserNamePasswordDTO) configDTO.getAuth().getCredentials();
      dockerConnectorBuilder.dockerAuthentication(createDockerAuthentication(dockerUserNamePasswordDTO, ngAccess));
    }

    DockerConnector dockerConnector = dockerConnectorBuilder.build();
    dockerConnector.setType(ConnectorType.DOCKER);
    return dockerConnector;
  }

  private DockerUserNamePasswordAuthentication createDockerAuthentication(
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO, NGAccess ngAccess) {
    return DockerUserNamePasswordAuthentication.builder()
        .username(dockerUserNamePasswordDTO.getUsername())
        .usernameRef(
            secretRefService.validateAndGetSecretConfigString(dockerUserNamePasswordDTO.getUsernameRef(), ngAccess))
        .passwordRef(
            secretRefService.validateAndGetSecretConfigString(dockerUserNamePasswordDTO.getPasswordRef(), ngAccess))
        .build();
  }
}
