/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.mappers.docker;

import static io.harness.delegate.beans.connector.docker.DockerAuthType.ANONYMOUS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class DockerEntityToDTO implements ConnectorEntityToDTOMapper<DockerConnectorDTO, DockerConnector> {
  @Override
  public DockerConnectorDTO createConnectorDTO(DockerConnector dockerConnector) {
    DockerAuthenticationDTO dockerAuthenticationDTO = null;
    if (dockerConnector.getAuthType() != ANONYMOUS || dockerConnector.getDockerAuthentication() != null) {
      DockerUserNamePasswordAuthentication dockerCredentials =
          (DockerUserNamePasswordAuthentication) dockerConnector.getDockerAuthentication();
      DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
          DockerUserNamePasswordDTO.builder()
              .username(dockerCredentials.getUsername())
              .usernameRef(SecretRefHelper.createSecretRef(dockerCredentials.getUsernameRef()))
              .passwordRef(SecretRefHelper.createSecretRef(dockerCredentials.getPasswordRef()))
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
