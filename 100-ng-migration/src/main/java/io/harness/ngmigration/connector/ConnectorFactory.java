package io.harness.ngmigration.connector;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthCredentialsDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerRegistryProviderType;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class ConnectorFactory {
  public static ConnectorType getConnectorType(SettingAttribute settingAttribute) {
    if (settingAttribute.getValue() instanceof DockerConfig) {
      return ConnectorType.DOCKER;
    }
    throw new UnsupportedOperationException("Connector Not Supported");
  }

  public static ConnectorConfigDTO getConfigDTO(SettingAttribute settingAttribute) {
    if (settingAttribute.getValue() instanceof DockerConfig) {
      return fromDocker(settingAttribute);
    }
    throw new UnsupportedOperationException("Connector Not Supported");
  }

  private static Set<String> toSet(List<String> list) {
    if (EmptyPredicate.isEmpty(list)) {
      return new HashSet<>();
    }
    return new HashSet<>(list);
  }

  private static ConnectorConfigDTO fromDocker(SettingAttribute settingAttribute) {
    DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();
    DockerAuthType dockerAuthType =
        StringUtils.isBlank(dockerConfig.getUsername()) ? DockerAuthType.ANONYMOUS : DockerAuthType.USER_PASSWORD;
    DockerAuthCredentialsDTO credentialsDTO = null;
    if (dockerAuthType.equals(DockerAuthType.USER_PASSWORD)) {
      credentialsDTO =
          DockerUserNamePasswordDTO.builder()
              .username(dockerConfig.getUsername())
              .passwordRef(
                  SecretRefData.builder().identifier(dockerConfig.getEncryptedPassword()).scope(Scope.PROJECT).build())
              .build();
    }
    return DockerConnectorDTO.builder()
        .dockerRegistryUrl(dockerConfig.getDockerRegistryUrl())
        .delegateSelectors(toSet(dockerConfig.getDelegateSelectors()))
        .providerType(DockerRegistryProviderType.OTHER)
        .auth(DockerAuthenticationDTO.builder().authType(dockerAuthType).credentials(credentialsDTO).build())
        .build();
  }
}
