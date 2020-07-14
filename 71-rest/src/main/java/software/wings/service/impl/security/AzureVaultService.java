package software.wings.service.impl.security;

import software.wings.beans.AzureVaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingVariableTypes;

import java.io.File;
import java.io.OutputStream;

public interface AzureVaultService {
  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AzureVaultConfig secretsManagerConfig, EncryptedData encryptedData);

  char[] decrypt(EncryptedData data, String accountId, AzureVaultConfig secretsManagerConfig);

  boolean delete(String accountId, String azureSecretName, AzureVaultConfig fromConfig);

  EncryptedData encryptFile(String accountId, AzureVaultConfig secretsManagerConfig, String name, byte[] inputBytes,
      EncryptedData savedEncryptedData);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output);
}
