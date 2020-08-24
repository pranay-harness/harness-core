package io.harness.connector.mappers.docker;

import com.google.inject.Singleton;

import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
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
    DockerAuthType dockerAuthType = configDTO.getAuthScheme().getAuthType();
    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        (DockerUserNamePasswordDTO) configDTO.getAuthScheme().getCredentials();
    DockerConnector dockerConnector =
        DockerConnector.builder()
            .url(configDTO.getUrl())
            .authType(dockerAuthType)
            .username(dockerUserNamePasswordDTO.getUsername())
            .passwordRef(dockerUserNamePasswordDTO.getPasswordRef().toSecretRefStringValue())
            .build();
    dockerConnector.setCategories(Collections.singletonList(ConnectorCategory.CLOUD_PROVIDER));
    dockerConnector.setType(ConnectorType.DOCKER);
    return dockerConnector;
  }
}
