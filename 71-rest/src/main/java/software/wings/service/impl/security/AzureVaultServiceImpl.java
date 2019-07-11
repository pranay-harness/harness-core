package software.wings.service.impl.security;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.eraro.ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.AZURE_KEY_VAULT_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.inject.Inject;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Duration;
import java.util.regex.Pattern;

@Slf4j
public class AzureVaultServiceImpl extends AbstractSecretServiceImpl implements AzureVaultService {
  // limit mentioned at
  // https://docs.microsoft.com/en-us/azure/key-vault/about-keys-secrets-and-certificates#working-with-secrets
  private static final int AZURE_SECRET_CONTENT_SIZE_LIMIT = 24000;
  private static final Pattern AZURE_KEYVAULT_NAME_PATTERN = Pattern.compile("^[\\w-/_+=.@!]+$");
  @Inject AzureSecretsManagerService azureSecretsManagerService;

  private void validateSecretName(String name) {
    if (!AZURE_KEYVAULT_NAME_PATTERN.matcher(name).find()) {
      throw new WingsException(AWS_SECRETS_MANAGER_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY, "Secret name can only contain alphanumeric characters, or any of: -_");
    }
  }

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AzureVaultConfig secretsManagerConfig, EncryptedData encryptedData) {
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();

    validateSecretName(name);
    return (EncryptedData) delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .encrypt(name, value, accountId, settingType, secretsManagerConfig, encryptedData);
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, AzureVaultConfig secretsManagerConfig) {
    // HAR-7605: Shorter timeout for decryption tasks, and it should retry on timeout or failure.
    int failedAttempts = 0;
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(accountId)
                                              .timeout(Duration.ofSeconds(5).toMillis())
                                              .appId(GLOBAL_APP_ID)
                                              .correlationId(data.getName())
                                              .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .decrypt(data, secretsManagerConfig);
      } catch (WingsException e) {
        failedAttempts++;
        logger.info(
            "Azure vault decryption failed for encryptedData {}. trial num: {}", data.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public boolean delete(String accountId, String azureSecretName, AzureVaultConfig config) {
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .delete(config, azureSecretName);
  }

  @Override
  public EncryptedData encryptFile(String accountId, AzureVaultConfig secretsManagerConfig, String name,
      byte[] inputBytes, EncryptedData savedEncryptedData) {
    // taking reference from software.wings.service.impl.security.AwsSecretsManagerServiceImpl.encryptFile
    Preconditions.checkNotNull(secretsManagerConfig);
    byte[] bytes = encodeBase64ToByteArray(inputBytes);

    if (bytes.length > AZURE_SECRET_CONTENT_SIZE_LIMIT) {
      throw new WingsException(AZURE_KEY_VAULT_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY,
              "Azure Secrets Manager limits secret value to " + AZURE_SECRET_CONTENT_SIZE_LIMIT + " bytes.");
    }
    EncryptedData fileData = encrypt(name, new String(CHARSET.decode(ByteBuffer.wrap(bytes)).array()), accountId,
        SettingVariableTypes.CONFIG_FILE, secretsManagerConfig, savedEncryptedData);
    fileData.setAccountId(accountId);
    fileData.setName(name);
    fileData.setType(SettingVariableTypes.CONFIG_FILE);
    fileData.setBase64Encoded(true);
    fileData.setFileSize(inputBytes.length);
    return fileData;
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    try {
      AzureVaultConfig secretManagerConfig =
          azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId());
      Preconditions.checkNotNull(secretManagerConfig);
      Preconditions.checkNotNull(encryptedData);
      char[] decrypt = decrypt(encryptedData, accountId, secretManagerConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      Files.write(fileData, file);
      return file;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      AzureVaultConfig secretsManagerConfig =
          azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId());
      Preconditions.checkNotNull(secretsManagerConfig);
      Preconditions.checkNotNull(encryptedData);
      char[] decrypt = decrypt(encryptedData, accountId, secretsManagerConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }
}
