package software.wings.service.intfc.security;

import software.wings.beans.VaultConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.impl.security.vault.SecretEngineSummary;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by rsingh on 11/2/17.
 */
public interface VaultService {
  String VAULT_VAILDATION_URL = "harness_vault_validation";
  String DEFAULT_BASE_PATH = "/harness";
  String DEFAULT_SECRET_ENGINE_NAME = "secret";
  String KEY_VALUE_SECRET_ENGINE_TYPE = "kv";
  String DEFAULT_KEY_NAME = "value";
  String PATH_SEPARATOR = "/";
  String KEY_SPEARATOR = "#";

  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData);

  char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig);

  String saveOrUpdateVaultConfig(String accountId, VaultConfig vaultConfig);

  boolean deleteVaultConfig(String accountId, String vaultConfigId);

  boolean isReadOnly(String vaultConfigId);

  List<SecretEngineSummary> listSecretEngines(VaultConfig vaultConfig);

  void decryptVaultConfigSecrets(String accountId, VaultConfig vaultConfig, boolean maskSecret);

  VaultConfig getVaultConfig(String accountId, String entityId);

  VaultConfig getVaultConfigByName(String accountId, String name);

  void renewTokens(VaultConfig vaultConfig);

  void renewAppRoleClientToken(VaultConfig vaultConfig);

  EncryptedData encryptFile(
      String accountId, VaultConfig vaultConfig, String name, byte[] inputBytes, EncryptedData savedEncryptedData);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output);

  void deleteSecret(String accountId, String path, VaultConfig vaultConfig);

  List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedData encryptedData, VaultConfig vaultConfig);
}
