package software.wings.service.intfc.security;

import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.util.Collection;

/**
 * Created by rsingh on 11/2/17.
 */
public interface VaultService {
  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData);

  char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig);

  VaultConfig getSecretConfig(String accountId);

  String saveVaultConfig(String accountId, VaultConfig vaultConfig);

  boolean deleteVaultConfig(String accountId, String vaultConfigId);

  Collection<VaultConfig> listVaultConfigs(String accountId, boolean maskSecret);

  VaultConfig getVaultConfig(String accountId, String entityId);

  boolean transitionVault(String accountId, String fromVaultId, String toVaultId);

  void changeVault(String accountId, String entityId, String fromVaultId, String toVaultId);

  EncryptedData encryptFile(
      String accountId, String name, BoundedInputStream inputStream, EncryptedData savedEncryptedData);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void deleteSecret(String accountId, String path, VaultConfig vaultConfig);
}
