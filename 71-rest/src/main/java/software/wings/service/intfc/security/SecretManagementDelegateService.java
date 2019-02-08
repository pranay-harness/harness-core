package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptedRecord;
import software.wings.beans.KmsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;

/**
 * Created by rsingh on 10/2/17.
 */
public interface SecretManagementDelegateService {
  int NUM_OF_RETRIES = 3;

  /**
   * Encrypt the value of the specified account against using KMS. The {@link EncryptedRecord} will be returned.
   */
  @DelegateTaskType(TaskType.KMS_ENCRYPT) EncryptedRecord encrypt(String accountId, char[] value, KmsConfig kmsConfig);

  /**
   * Decrypt the previously encrypted data using KMS. The decrypted value will be returned.
   */
  @DelegateTaskType(TaskType.KMS_DECRYPT) char[] decrypt(EncryptedRecord data, KmsConfig kmsConfig);

  /**
   * Encrypt the name-value setting of the specified account against using KMS. The {@link EncryptedRecord} will be
   * returned.
   */
  @DelegateTaskType(TaskType.VAULT_ENCRYPT)
  EncryptedRecord encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedRecord savedEncryptedData);

  /**
   * Decrypt the previously encrypted data using Hashicorp Vault. The decrypted value will be returned.
   */
  @DelegateTaskType(TaskType.VAULT_DECRYPT) char[] decrypt(EncryptedRecord data, VaultConfig vaultConfig);

  /**
   * Delete the previously saved secret from Hashicorp Vault from the specified vault path. Return true if deletion is
   * successful; Return false if the deletion failed (e.g. the path specified doesn't have any value bound to).
   */
  @DelegateTaskType(TaskType.VAULT_DELETE_SECRET) boolean deleteVaultSecret(String path, VaultConfig vaultConfig);

  /**
   * Retrieve the versions metadata for Vault managed secrets from Hashicorp Vault, and construct the version history as
   * {@see SecretChangeLog} to be displayed in Harness UI.
   */
  @DelegateTaskType(TaskType.VAULT_GET_CHANGELOG)
  List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedRecord encryptedData, VaultConfig vaultConfig);

  /**
   * Renew the Hashicorp Vault authentication token.
   */
  @DelegateTaskType(TaskType.VAULT_RENEW_TOKEN) boolean renewVaultToken(VaultConfig vaultConfig);
}
