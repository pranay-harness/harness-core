package io.harness.secretmanagers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretManagerConfig;
import io.harness.security.encryption.EncryptionType;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by mark.lu on 5/31/2019.
 */
@OwnedBy(PL)
public interface SecretManagerConfigService {
  String save(SecretManagerConfig secretManagerConfig);

  String getSecretManagerName(@NotEmpty String kmsId, @NotEmpty String accountId);

  EncryptionType getEncryptionType(@NotEmpty String accountId);

  EncryptionType getEncryptionBySecretManagerId(@NotEmpty String kmsId, @NotEmpty String accountId);

  List<SecretManagerConfig> listSecretManagers(String accountId, boolean maskSecret);

  List<SecretManagerConfig> listSecretManagers(
      String accountId, boolean maskSecret, boolean includeGlobalSecretManager);

  List<SecretManagerConfig> listSecretManagersByType(
      String accountId, EncryptionType encryptionType, boolean maskSecret);

  SecretManagerConfig getDefaultSecretManager(String accountId);

  SecretManagerConfig getGlobalSecretManager(String accountId);

  List<SecretManagerConfig> getAllGlobalSecretManagers();

  SecretManagerConfig getSecretManager(String accountId, String entityId);

  SecretManagerConfig getSecretManager(String accountId, String entityId, boolean maskSecrets);

  List<Integer> getCountOfSecretManagersForAccounts(List<String> accountIds, boolean includeGlobalSecretManager);

  void decryptEncryptionConfigSecrets(String accountId, SecretManagerConfig secretManagerConfig, boolean maskSecrets);
}
