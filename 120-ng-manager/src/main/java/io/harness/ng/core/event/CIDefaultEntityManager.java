package io.harness.ng.core.event;

import static io.harness.ng.NextGenModule.CONNECTOR_DECORATOR_SERVICE;
import static io.harness.secretmanagerclient.SecretType.SecretText;
import static io.harness.secretmanagerclient.ValueType.Inline;

import io.harness.NGConstants;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerRegistryProviderType;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.CiDefaultEntityConfiguration;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CIDefaultEntityManager {
  public static final String HARNESS_IMAGE_PASSWORD = "HARNESS_IMAGE_PASSWORD";
  public static final String HARNESS_IMAGE = "harnessImage";
  @Inject private SecretCrudService secretCrudService;
  @Inject private CiDefaultEntityConfiguration configuration;
  @Inject @Named(CONNECTOR_DECORATOR_SERVICE) ConnectorService connectorService;

  public void createCIDefaultEntities(String accountIdentifier, String organizer, String project) {
    createDefaultSecret(accountIdentifier, organizer, project);
    createDockerConnector(accountIdentifier, organizer, project);
  }

  private void createDockerConnector(String accountId, String org, String project) {
    Optional<ConnectorResponseDTO> harnessImage = connectorService.get(accountId, org, project, "harnessImage");
    if (harnessImage.isPresent()) {
      log.info(String.format("skipping creating docker connector as its already present in account id: %s", accountId));
      return;
    }

    try {
      SecretRefData harnessImagePassword =
          SecretRefData.builder().identifier(HARNESS_IMAGE_PASSWORD).scope(Scope.PROJECT).build();
      DockerUserNamePasswordDTO harnessImageCredentials = DockerUserNamePasswordDTO.builder()
                                                              .username(configuration.getHarnessImageUseName())
                                                              .passwordRef(harnessImagePassword)
                                                              .build();
      DockerAuthenticationDTO authenticationDTO = DockerAuthenticationDTO.builder()
                                                      .authType(DockerAuthType.USER_PASSWORD)
                                                      .credentials(harnessImageCredentials)
                                                      .build();
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

  private void createDefaultSecret(String accountIdentifier, String org, String project) {
    SecretDTOV2 secretTextDTO = getSecretTextDTO(org, project);

    try {
      Optional<SecretResponseWrapper> encryptedData =
          secretCrudService.get(accountIdentifier, org, project, secretTextDTO.getIdentifier());
      if (!encryptedData.isPresent()) {
        log.info(String.format("Creating default secret %s for account %s org: %s project: %s", secretTextDTO.getName(),
            accountIdentifier, org, project));
        secretCrudService.create(accountIdentifier, secretTextDTO);
      }
    } catch (Exception ex) {
      log.error(String.format(
                    "Failed in creating default secret %s for account %s", secretTextDTO.getName(), accountIdentifier),
          ex);
    }
  }

  private SecretDTOV2 getSecretTextDTO(String org, String project) {
    return SecretDTOV2.builder()
        .type(SecretText)
        .orgIdentifier(org)
        .projectIdentifier(project)
        .spec(SecretTextSpecDTO.builder()
                  .value(configuration.getHarnessImagePassword())
                  .secretManagerIdentifier(NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER)
                  .valueType(Inline)
                  .build())
        .identifier(HARNESS_IMAGE_PASSWORD)
        .name(HARNESS_IMAGE_PASSWORD)
        .build();
  }
}
