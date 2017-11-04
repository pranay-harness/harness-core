package software.wings.service.intfc.security;

import software.wings.beans.KmsConfig;
import software.wings.beans.TaskType;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by rsingh on 10/2/17.
 */
public interface SecretManagementDelegateService {
  @DelegateTaskType(TaskType.KMS_ENCRYPT) EncryptedData encrypt(char[] value, KmsConfig kmsConfig) throws IOException;

  @DelegateTaskType(TaskType.KMS_DECRYPT) char[] decrypt(EncryptedData data, KmsConfig kmsConfig) throws IOException;

  @DelegateTaskType(TaskType.VAULT_ENCRYPT)
  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData) throws IOException;

  @DelegateTaskType(TaskType.VAULT_DECRYPT)
  char[] decrypt(EncryptedData data, VaultConfig vaultConfig) throws IOException;
}
