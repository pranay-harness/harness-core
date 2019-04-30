package software.wings.service.intfc.security;

import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

/**
 * Created by rsingh on 11/2/17.
 */
public interface VaultService {
  String VAULT_VAILDATION_URL = "harness_vault_validation";
  String DEFAULT_BASE_PATH = "/harness";
  String DEFAULT_KEY_NAME = "value";
  String PATH_SEPARATOR = "/";
  String KEY_SPEARATOR = "#";

  String IS_DEFAULT_KEY = "isDefault";
  String ID_KEY = "_id";

  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData);

  char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig);

  VaultConfig getSecretConfig(String accountId);

  String saveVaultConfig(String accountId, VaultConfig vaultConfig);

  boolean deleteVaultConfig(String accountId, String vaultConfigId);

  Collection<VaultConfig> listVaultConfigs(String accountId, boolean maskSecret);

  VaultConfig getVaultConfig(String accountId, String entityId);

  VaultConfig getVaultConfigByName(String accountId, String name);

  void renewTokens(String accountId);

  void appRoleLogin(String accountId);

  EncryptedData encryptFile(
      String accountId, VaultConfig vaultConfig, String name, byte[] inputBytes, EncryptedData savedEncryptedData);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output);

  void deleteSecret(String accountId, String path, VaultConfig vaultConfig);

  List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedData encryptedData, VaultConfig vaultConfig);
}
