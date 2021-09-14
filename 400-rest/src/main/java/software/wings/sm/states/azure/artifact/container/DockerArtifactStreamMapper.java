/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.azure.artifact.container;

import static io.harness.delegate.beans.connector.docker.DockerAuthType.USER_PASSWORD;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.sm.states.azure.artifact.ArtifactStreamMapper;

import java.util.Optional;

public final class DockerArtifactStreamMapper extends ArtifactStreamMapper {
  private static final String PUBLIC_DOCKER_REGISTER_URL = "https://index.docker.io";

  public DockerArtifactStreamMapper(Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    super(artifact, artifactStreamAttributes);
  }

  @Override
  public ConnectorConfigDTO getConnectorDTO() {
    DockerConfig dockerConfig = (DockerConfig) artifactStreamAttributes.getServerSetting().getValue();
    String dockerUserName = dockerConfig.getUsername();
    String passwordSecretRef = dockerConfig.getEncryptedPassword();
    SecretRefData secretRefData = new SecretRefData(passwordSecretRef, Scope.ACCOUNT, null);

    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        DockerUserNamePasswordDTO.builder().username(dockerUserName).passwordRef(secretRefData).build();
    DockerAuthenticationDTO dockerAuthenticationDTO =
        DockerAuthenticationDTO.builder().authType(USER_PASSWORD).credentials(dockerUserNamePasswordDTO).build();
    return DockerConnectorDTO.builder()
        .dockerRegistryUrl(PUBLIC_DOCKER_REGISTER_URL)
        .auth(dockerAuthenticationDTO)
        .build();
  }

  @Override
  public AzureRegistryType getAzureRegistryType() {
    DockerConfig dockerConfig = (DockerConfig) artifactStreamAttributes.getServerSetting().getValue();
    String dockerUserName = dockerConfig.getUsername();
    String passwordSecretRef = dockerConfig.getEncryptedPassword();
    if (isNotBlank(dockerUserName) && isNotBlank(passwordSecretRef)) {
      return AzureRegistryType.DOCKER_HUB_PRIVATE;
    } else {
      return AzureRegistryType.DOCKER_HUB_PUBLIC;
    }
  }

  @Override
  public boolean isDockerArtifactType() {
    return true;
  }

  @Override
  public Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO) {
    DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) connectorConfigDTO;
    return Optional.ofNullable(dockerConnectorDTO.getAuth().getCredentials());
  }

  @Override
  public Optional<EncryptableSetting> getEncryptableSetting() {
    return Optional.ofNullable((DockerConfig) artifactStreamAttributes.getServerSetting().getValue());
  }

  @Override
  public String getFullImageName() {
    return artifact.getArtifactSourceName();
  }
}
