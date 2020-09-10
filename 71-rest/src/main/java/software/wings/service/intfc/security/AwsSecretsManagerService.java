package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingVariableTypes;

import java.io.File;
import java.io.OutputStream;

/**
 * @author marklu on 2019-05-07
 */
@OwnedBy(PL)
public interface AwsSecretsManagerService {
  EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AwsSecretsManagerConfig secretsManagerConfig, EncryptedData encryptedData);

  char[] decrypt(EncryptedData data, String accountId, AwsSecretsManagerConfig secretsManagerConfig);

  void deleteSecret(String accountId, String path, AwsSecretsManagerConfig secretsManagerConfig);

  AwsSecretsManagerConfig getAwsSecretsManagerConfig(String accountId, String configId);

  String saveAwsSecretsManagerConfig(String accountId, AwsSecretsManagerConfig secretsManagerConfig);

  boolean deleteAwsSecretsManagerConfig(String accountId, String configId);

  void validateSecretsManagerConfig(AwsSecretsManagerConfig secretsManagerConfig);

  void decryptAsmConfigSecrets(String accountId, AwsSecretsManagerConfig secretsManagerConfig, boolean maskSecret);

  EncryptedData encryptFile(String accountId, AwsSecretsManagerConfig secretsManagerConfig, String name,
      byte[] inputBytes, EncryptedData savedEncryptedData);

  File decryptFile(File file, String accountId, EncryptedData encryptedData);

  void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output);
}
