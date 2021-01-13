package software.wings.service.intfc.security;

import static io.harness.security.encryption.EncryptionType.VAULT;

import io.harness.beans.SecretManagerConfig;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;

import software.wings.beans.VaultConfig;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface NGSecretManagerService {
  static boolean isReadOnlySecretManager(SecretManagerConfig secretManagerConfig) {
    if (secretManagerConfig == null) {
      return false;
    }
    if (VAULT.equals(secretManagerConfig.getEncryptionType())) {
      return ((VaultConfig) secretManagerConfig).isReadOnly();
    }
    return false;
  }

  SecretManagerConfig create(SecretManagerConfig secretManagerConfig);

  ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  List<SecretManagerConfig> list(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Optional<SecretManagerConfig> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  SecretManagerConfig update(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, SecretManagerConfigUpdateDTO updateDTO);

  SecretManagerConfig getGlobalSecretManager(String accountIdentifier);

  boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, boolean softDelete);

  SecretManagerMetadataDTO getMetadata(String accountIdentifier, SecretManagerMetadataRequestDTO requestDTO);

  long getCountOfSecretsCreatedUsingSecretManager(
      String account, String org, String project, String secretManagerIdentifier);
}
