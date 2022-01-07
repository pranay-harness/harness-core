/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.utils;

import static io.harness.encryption.Scope.ACCOUNT;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DockerConnectorTestHelper {
  public Connector createDockerConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, Scope scope) {
    String dockerRegistryUrl = "url";
    String dockerUserName = "dockerUserName";
    String passwordRef = ACCOUNT.getYamlRepresentation() + ".passwordRef";

    DockerConnector dockerConnector = DockerConnector.builder()
                                          .authType(DockerAuthType.USER_PASSWORD)
                                          .url(dockerRegistryUrl)
                                          .dockerAuthentication(DockerUserNamePasswordAuthentication.builder()
                                                                    .username(dockerUserName)
                                                                    .passwordRef(passwordRef)
                                                                    .build())
                                          .build();

    dockerConnector.setAccountIdentifier(accountIdentifier);
    dockerConnector.setOrgIdentifier(orgIdentifier);
    dockerConnector.setProjectIdentifier(projectIdentifier);
    dockerConnector.setIdentifier(identifier);
    dockerConnector.setScope(scope);
    dockerConnector.setType(ConnectorType.DOCKER);

    return dockerConnector;
  }

  public ConnectorConfigDTO createDockerConnectorDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, Scope scope) {
    String dockerRegistryUrl = "url";
    String dockerUserName = "dockerUserName";
    DockerConnectorDTO dockerConnector = DockerConnectorDTO.builder()
                                             .dockerRegistryUrl(dockerRegistryUrl)
                                             .auth(DockerAuthenticationDTO.builder()
                                                       .authType(DockerAuthType.USER_PASSWORD)
                                                       .credentials(DockerUserNamePasswordDTO.builder()
                                                                        .username(dockerUserName)
                                                                        .passwordRef(SecretRefData.builder().build())
                                                                        .build())
                                                       .build())
                                             .build();
    return dockerConnector;
  }
}
