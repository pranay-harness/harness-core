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
  @DelegateTaskType(TaskType.KMS_ENCRYPT) EncryptedData encrypt(String accountId, char[] value, KmsConfig kmsConfig);

  @DelegateTaskType(TaskType.KMS_DECRYPT) char[] decrypt(EncryptedData data, KmsConfig kmsConfig);

  @DelegateTaskType(TaskType.VAULT_ENCRYPT)
  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData);

  @DelegateTaskType(TaskType.VAULT_DECRYPT) char[] decrypt(EncryptedData data, VaultConfig vaultConfig);

  void deleteVaultSecret(String path, VaultConfig vaultConfig);

  @DelegateTaskType(TaskType.VAULT_RENEW_TOKEN) boolean renewVaultToken(VaultConfig vaultConfig);
}
