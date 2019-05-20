package software.wings.service.intfc.security;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;

/**
 * @author marklu on 2019-05-07
 */
public interface AwsSecretsManagerService {
  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AwsSecretsManagerConfig secretsManagerConfig, EncryptedData encryptedData);

  char[] decrypt(EncryptedData data, String accountId, AwsSecretsManagerConfig secretsManagerConfig);

  void deleteSecret(String accountId, String path, AwsSecretsManagerConfig secretsManagerConfig);

  AwsSecretsManagerConfig getSecretConfig(String accountId);

  AwsSecretsManagerConfig getAwsSecretsManagerConfig(String accountId, String configId);

  String saveAwsSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig);

  boolean deleteAwsSecretsManagerConfig(String accountId, String configId);

  Collection<AwsSecretsManagerConfig> listAwsSecretsManagerConfigs(String accountId, boolean maskSecret);

  EncryptedData encryptFile(String accountId, AwsSecretsManagerConfig secretsManagerConfig, String name,
      byte[] inputBytes, EncryptedData savedEncryptedData);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output);
}
