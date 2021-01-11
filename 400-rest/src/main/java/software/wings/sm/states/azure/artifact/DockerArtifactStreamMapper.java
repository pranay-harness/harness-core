package software.wings.sm.states.azure.artifact;

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

import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.util.Optional;

public class DockerArtifactStreamMapper extends ArtifactStreamMapper {
  public DockerArtifactStreamMapper(Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    super(artifact, artifactStreamAttributes);
  }

  public ConnectorConfigDTO getConnectorDTO() {
    DockerConfig dockerConfig = (DockerConfig) artifactStreamAttributes.getServerSetting().getValue();
    String dockerUserName = dockerConfig.getUsername();
    String dockerRegistryUrl = dockerConfig.fetchRegistryUrl();
    String passwordSecretRef = dockerConfig.getEncryptedPassword();
    SecretRefData secretRefData = new SecretRefData(passwordSecretRef, Scope.ACCOUNT, null);

    DockerUserNamePasswordDTO dockerUserNamePasswordDTO =
        DockerUserNamePasswordDTO.builder().username(dockerUserName).passwordRef(secretRefData).build();
    DockerAuthenticationDTO dockerAuthenticationDTO =
        DockerAuthenticationDTO.builder().authType(USER_PASSWORD).credentials(dockerUserNamePasswordDTO).build();
    return DockerConnectorDTO.builder().dockerRegistryUrl(dockerRegistryUrl).auth(dockerAuthenticationDTO).build();
  }

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
  public Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO) {
    DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) connectorConfigDTO;
    return Optional.ofNullable(dockerConnectorDTO.getAuth().getCredentials());
  }
}
