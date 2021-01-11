package software.wings.sm.states.azure.artifact;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.azure.registry.AzureRegistryType;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.config.ArtifactoryConfig;

import java.util.Optional;

public class ArtifactoryArtifactStreamMapper extends ArtifactStreamMapper {
  protected ArtifactoryArtifactStreamMapper(Artifact artifact, ArtifactStreamAttributes artifactStreamAttributes) {
    super(artifact, artifactStreamAttributes);
  }

  public ConnectorConfigDTO getConnectorDTO() {
    ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) artifactStreamAttributes.getServerSetting().getValue();
    String username = artifactoryConfig.getUsername();
    String registryUrl = artifactoryConfig.fetchRegistryUrl();
    String passwordSecretRef = artifactoryConfig.getEncryptedPassword();
    SecretRefData secretRefData = new SecretRefData(passwordSecretRef, Scope.ACCOUNT, null);

    ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
        ArtifactoryUsernamePasswordAuthDTO.builder().username(username).passwordRef(secretRefData).build();
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder()
                                                                    .authType(ArtifactoryAuthType.USER_PASSWORD)
                                                                    .credentials(artifactoryUsernamePasswordAuthDTO)
                                                                    .build();
    return ArtifactoryConnectorDTO.builder()
        .artifactoryServerUrl(registryUrl)
        .auth(artifactoryAuthenticationDTO)
        .build();
  }

  public AzureRegistryType getAzureRegistryType() {
    return AzureRegistryType.ARTIFACTORY_PRIVATE_REGISTRY;
  }

  @Override
  public Optional<DecryptableEntity> getConnectorDTOAuthCredentials(ConnectorConfigDTO connectorConfigDTO) {
    ArtifactoryConnectorDTO artifactoryConnectorDTO = (ArtifactoryConnectorDTO) connectorConfigDTO;
    return Optional.ofNullable(artifactoryConnectorDTO.getAuth().getCredentials());
  }
}
