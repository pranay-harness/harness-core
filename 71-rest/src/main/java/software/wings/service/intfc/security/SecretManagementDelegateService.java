package software.wings.service.intfc.security;

import software.wings.beans.KmsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

/**
 * Created by rsingh on 10/2/17.
 */
public interface SecretManagementDelegateService {
  /**
   * Encrypt the value of the specified account against using KMS. The {@link EncryptedData} will be returned.
   */
  @DelegateTaskType(TaskType.KMS_ENCRYPT) EncryptedData encrypt(String accountId, char[] value, KmsConfig kmsConfig);

  /**
   * Decrypt the previously encrypted data using KMS. The decrypted value will be returned.
   */
  @DelegateTaskType(TaskType.KMS_DECRYPT) char[] decrypt(EncryptedData data, KmsConfig kmsConfig);

  /**
   * Encrypt the name-value setting of the specified account against using KMS. The {@link EncryptedData} will be
   * returned.
   */
  @DelegateTaskType(TaskType.VAULT_ENCRYPT)
  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData);

  /**
   * Decrypt the previously encrypted data using Hashicorp Vault. The decrypted value will be returned.
   */
  @DelegateTaskType(TaskType.VAULT_DECRYPT) char[] decrypt(EncryptedData data, VaultConfig vaultConfig);

  /**
   * Delete the previously saved secret from Hashicorp Vault from the specified vault path. Return true if deletion is
   * successful; Return false if the deletion failed (e.g. the path specified doesn't have any value bound to).
   */
  @DelegateTaskType(TaskType.VAULT_DELETE_SECRET) boolean deleteVaultSecret(String path, VaultConfig vaultConfig);

  /**
   * Renew the Hashicorp Vault authentication token.
   */
  @DelegateTaskType(TaskType.VAULT_RENEW_TOKEN) boolean renewVaultToken(VaultConfig vaultConfig);
}
