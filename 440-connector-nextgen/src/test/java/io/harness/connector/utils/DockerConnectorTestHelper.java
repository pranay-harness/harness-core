package io.harness.connector.utils;

import static io.harness.encryption.Scope.ACCOUNT;

import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.docker.DockerConnector;
import io.harness.connector.entities.embedded.docker.DockerUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.encryption.Scope;

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
}
