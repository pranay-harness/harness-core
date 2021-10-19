package software.wings.sm.states.azure.artifact.container;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefHelper;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.sm.states.azure.artifact.ArtifactConnectorMapper;

import java.util.Optional;

public final class ArtifactoryArtifactConnectorMapper extends ArtifactConnectorMapper {
  public ArtifactoryArtifactConnectorMapper(Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    super(artifact, artifactStreamAttributes);
  }

  public ConnectorConfigDTO getConnectorDTO() {
    ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) artifactStreamAttributes.getServerSetting().getValue();
    String username = artifactoryConfig.getUsername();
    String registryUrl = artifactoryConfig.fetchRegistryUrl();
    String passwordSecretRef = artifactoryConfig.getEncryptedPassword();

    ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
        ArtifactoryUsernamePasswordAuthDTO.builder()
            .username(username)
            .passwordRef(SecretRefHelper.createSecretRef(passwordSecretRef, Scope.ACCOUNT, null))
            .build();
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder()
                                                                    .authType(ArtifactoryAuthType.USER_PASSWORD)
                                                                    .credentials(artifactoryUsernamePasswordAuthDTO)
                                                                    .build();
    return ArtifactoryConnectorDTO.builder()
        .artifactoryServerUrl(registryUrl)
        .auth(artifactoryAuthenticationDTO)
        .build();
  }

  @Override
  public AzureRegistryType getAzureRegistryType() {
    return AzureRegistryType.ARTIFACTORY_PRIVATE_REGISTRY;
  }

  @Override
  public boolean isDockerArtifactType() {
    return true;
  }

  @Override
  public Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO) {
    ArtifactoryConnectorDTO artifactoryConnectorDTO = (ArtifactoryConnectorDTO) connectorConfigDTO;
    return Optional.ofNullable(artifactoryConnectorDTO.getAuth().getCredentials());
  }

  @Override
  public Optional<EncryptableSetting> getEncryptableSetting() {
    return Optional.ofNullable((ArtifactoryConfig) artifactStreamAttributes.getServerSetting().getValue());
  }
}
