package software.wings.service.intfc.security;

import io.harness.security.encryption.EncryptedRecord;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.impl.security.vault.SecretEngineSummary;
import software.wings.service.impl.security.vault.VaultAppRoleLoginResult;
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
   * Encrypt the name-value setting in the specified account using Hashicorp Vault. The {@link EncryptedRecord} will be
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

  /**
   * List vault secret engines
   */
  @DelegateTaskType(TaskType.VAULT_LIST_ENGINES) List<SecretEngineSummary> listSecretEngines(VaultConfig vaultConfig);

  /**
   * Login Vault using AppRole auth.
   */
  @DelegateTaskType(TaskType.VAULT_APPROLE_LOGIN) VaultAppRoleLoginResult appRoleLogin(VaultConfig vaultConfig);

  /**
   * Encrypt the name-value setting in the specified account using AWS Secrets Manager. A {@link EncryptedRecord}
   * instance will be returned.
   */
  @DelegateTaskType(TaskType.ASM_ENCRYPT)
  EncryptedRecord encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AwsSecretsManagerConfig secretsManagerConfig, EncryptedRecord savedEncryptedData);

  /**
   * Decrypt the previously encrypted data using AWS Secrets Manager. The decrypted value will be returned.
   */
  @DelegateTaskType(TaskType.ASM_DECRYPT)
  char[] decrypt(EncryptedRecord data, AwsSecretsManagerConfig secretsManagerConfig);

  /**
   * Delete the previously saved secret from AWS Secrets Manager. Return true if the deltion is successful; Return false
   * if the deletion failed (e.g. The specified path is non-existent)
   */
  @DelegateTaskType(TaskType.ASM_DELETE_SECRET)
  boolean deleteSecret(String secretName, AwsSecretsManagerConfig secretsManagerConfig);

  /**
   * Encrypt the name-value setting in the specified account using Azure vault Secrets Manager. A {@link
   * EncryptedRecord} instance will be returned.
   */
  @DelegateTaskType(TaskType.AZURE_VAULT_ENCRYPT)
  EncryptedRecord encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AzureVaultConfig azureConfig, EncryptedRecord savedEncryptedData);

  /**
   * Decrypt the previously encrypted data using Azure Vault Secrets Manager. The decrypted value will be returned.
   */
  @DelegateTaskType(TaskType.AZURE_VAULT_DECRYPT) char[] decrypt(EncryptedRecord data, AzureVaultConfig azureConfig);

  /**
   * Delete the previously saved secret from Azure Vault. Return true if the deletion is successful; Return false
   * if the deletion failed (e.g. The specified path is non-existent)
   */
  @DelegateTaskType(TaskType.AZURE_VAULT_DELETE) boolean delete(AzureVaultConfig config, String key);

  /**
   * Decrypt the previously encrypted data using CyberArk Secrets Manager. The decrypted value will be returned.
   * We only allow reference of existing secrets in CyberArk for now. Therefore only the decrypt task is present.
   */
  @DelegateTaskType(TaskType.CYBERARK_DECRYPT) char[] decrypt(EncryptedRecord data, CyberArkConfig cyberArkConfig);

  /**
   * Validate the CyberArk configuration, including the connectivity to CyberArk service, client certificate etc.
   */
  @DelegateTaskType(TaskType.CYBERARK_VALIDATE_CONFIG) boolean validateCyberArkConfig(CyberArkConfig cyberArkConfig);

  /**
   *
   * @param value The value to be encrypted
   * @param accountId The accountId of the user requesting this encryption
   * @param gcpKmsConfig The decrypted gcpKmsConfig required to encrypt the value
   * @param savedEncryptedData Previously saved encrypted data that is being overridden.
   * @return EncryptedRecord with the encrypted value.
   */
  @DelegateTaskType(TaskType.GCP_KMS_ENCRYPT)
  EncryptedRecord encrypt(
      String value, String accountId, GcpKmsConfig gcpKmsConfig, EncryptedRecord savedEncryptedData);

  @DelegateTaskType(TaskType.GCP_KMS_DECRYPT) char[] decrypt(EncryptedRecord data, GcpKmsConfig gcpKmsConfig);

  char[] decrypt(EncryptedRecord data, CustomSecretsManagerConfig customSecretsManagerConfig);
}
