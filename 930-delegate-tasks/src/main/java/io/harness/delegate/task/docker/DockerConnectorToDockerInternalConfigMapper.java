package io.harness.delegate.task.docker;

import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.artifacts.docker.beans.DockerInternalConfig.DockerInternalConfigBuilder;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.SecretRefData;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DockerConnectorToDockerInternalConfigMapper {
  public DockerInternalConfig toDockerInternalConfig(DockerConnectorDTO dockerConnectorDTO) {
    DockerInternalConfigBuilder dockerInternalConfigBuilder =
        DockerInternalConfig.builder().dockerRegistryUrl(dockerConnectorDTO.getDockerRegistryUrl());
    if (dockerConnectorDTO.getAuth() != null
        && dockerConnectorDTO.getAuth().getAuthType() == DockerAuthType.USER_PASSWORD) {
      DockerUserNamePasswordDTO dockerAuthCredentialsDTO =
          (DockerUserNamePasswordDTO) dockerConnectorDTO.getAuth().getCredentials();
      dockerInternalConfigBuilder.username(dockerAuthCredentialsDTO.getUsername())
          .password(getDecryptedValueWithNullCheck(dockerAuthCredentialsDTO.getPasswordRef()));
    }
    return dockerInternalConfigBuilder.build();
  }

  private String getDecryptedValueWithNullCheck(SecretRefData passwordRef) {
    if (passwordRef != null) {
      return String.valueOf(passwordRef.getDecryptedValue());
    }
    return null;
  }
}
