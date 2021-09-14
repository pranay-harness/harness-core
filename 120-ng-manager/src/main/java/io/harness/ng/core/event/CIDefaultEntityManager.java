/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.event;

import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerRegistryProviderType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CIDefaultEntityManager {
  public static final String HARNESS_IMAGE = "harnessImage";
  @Inject @Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService connectorService;

  public void createCIDefaultEntities(String accountIdentifier, String organizer, String project) {
    createDockerConnector(accountIdentifier, organizer, project);
  }

  private void createDockerConnector(String accountId, String org, String project) {
    Optional<ConnectorResponseDTO> harnessImage = connectorService.get(accountId, org, project, HARNESS_IMAGE);
    if (harnessImage.isPresent()) {
      log.info(String.format(
          "skipping creating docker connector as its already present in account id: %s, org: %s, project: %s",
          accountId, org, project));
      return;
    }

    try {
      DockerAuthenticationDTO authenticationDTO =
          DockerAuthenticationDTO.builder().authType(DockerAuthType.ANONYMOUS).build();
      DockerConnectorDTO dockerConnectorDTO = DockerConnectorDTO.builder()
                                                  .dockerRegistryUrl("https://index.docker.io/v2/")
                                                  .auth(authenticationDTO)
                                                  .providerType(DockerRegistryProviderType.DOCKER_HUB)
                                                  .build();
      ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                              .connectorConfig(dockerConnectorDTO)
                                              .identifier(HARNESS_IMAGE)
                                              .connectorType(ConnectorType.DOCKER)
                                              .name("Harness Docker Connector")
                                              .description("Harness internal connector")
                                              .orgIdentifier(org)
                                              .projectIdentifier(project)
                                              .build();
      ConnectorDTO connectorDTO = ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build();
      connectorService.create(connectorDTO, accountId);
    } catch (Exception ex) {
      log.error(String.format("Failed to create docker connector in account: %s for org: %s for project: %s", accountId,
                    org, project),
          ex);
    }
  }
}
