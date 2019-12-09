package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.GCP_KMS_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.KMS_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;

import com.google.common.io.Files;
import com.google.inject.Inject;

import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.BaseFile;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.gcpkms.GcpKmsEncryptDecryptClient;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Duration;

@Slf4j
public class GcpKmsServiceImpl extends AbstractSecretServiceImpl implements GcpKmsService {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient;
  @Inject private FileService fileService;

  @Override
  public EncryptedData encrypt(String value, String accountId, GcpKmsConfig gcpKmsConfig, EncryptedData encryptedData) {
    if (value == null) {
      return encryptLocal(null);
    }
    if (gcpKmsConfig == null) {
      return encryptLocal(value.toCharArray());
    }

    if (GLOBAL_ACCOUNT_ID.equals(gcpKmsConfig.getAccountId())
        && featureFlagService.isEnabled(FeatureName.GLOBAL_KMS_PRE_PROCESSING, accountId)) {
      logger.info("Encrypt secret with global KMS secret manager for account {}", accountId);
      return (EncryptedData) gcpKmsEncryptDecryptClient.encrypt(value, accountId, gcpKmsConfig, encryptedData);
    } else {
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(accountId)
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      return (EncryptedData) delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .encrypt(value, accountId, gcpKmsConfig, encryptedData);
    }
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, GcpKmsConfig gcpKmsConfig) {
    if (gcpKmsConfig == null || data.getEncryptedValue() == null) {
      return decryptLocal(data);
    }

    if (GLOBAL_ACCOUNT_ID.equals(gcpKmsConfig.getAccountId())
        && featureFlagService.isEnabled(FeatureName.GLOBAL_KMS_PRE_PROCESSING, accountId)) {
      // PL-1836: Perform encrypt/decrypt at manager side for global shared KMS.
      logger.info("Decrypt secret with global KMS secret manager for account {}", accountId);
      return gcpKmsEncryptDecryptClient.decrypt(data, gcpKmsConfig);
    } else {
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
              .decrypt(data, gcpKmsConfig);
        } catch (WingsException e) {
          failedAttempts++;
          logger.info("KMS Decryption failed for encryptedData {}. trial num: {}", data.getName(), failedAttempts, e);
          if (failedAttempts == NUM_OF_RETRIES) {
            throw e;
          }
          sleep(ofMillis(1000));
        }
      }
    }
  }

  @Override
  public EncryptedData encryptFile(
      String accountId, GcpKmsConfig gcpKmsConfig, String name, byte[] inputBytes, EncryptedData savedEncryptedData) {
    checkNotNull(gcpKmsConfig, "GCP KMS secret manager can't be null");
    byte[] bytes = encodeBase64ToByteArray(inputBytes);
    EncryptedData fileData =
        encrypt(CHARSET.decode(ByteBuffer.wrap(bytes)).toString(), accountId, gcpKmsConfig, savedEncryptedData);
    fileData.setName(name);
    fileData.setAccountId(accountId);
    fileData.setType(SettingVariableTypes.CONFIG_FILE);
    fileData.setBase64Encoded(true);
    char[] encryptedValue = fileData.getEncryptedValue();
    BaseFile baseFile = new BaseFile();
    baseFile.setFileName(name);
    baseFile.setAccountId(accountId);
    baseFile.setFileUuid(UUIDGenerator.generateUuid());
    String fileId = fileService.saveFile(
        baseFile, new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(encryptedValue)).array()), CONFIGS);
    fileData.setEncryptedValue(fileId.toCharArray());
    fileData.setFileSize(inputBytes.length);
    return fileData;
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    try {
      GcpKmsConfig gcpKmsConfig = gcpSecretsManagerService.getGcpKmsConfig(accountId, encryptedData.getKmsId());
      checkNotNull(gcpKmsConfig, "GCP KMS configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      byte[] bytes = Files.toByteArray(file);
      encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(bytes)).array());
      char[] decrypt = decrypt(encryptedData, accountId, gcpKmsConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      Files.write(fileData, file);
      return file;
    } catch (IOException ioe) {
      throw new SecretManagementException(
          GCP_KMS_OPERATION_ERROR, "Failed to decrypt data into an output file", ioe, USER);
    }
  }

  @Override
  public void decryptToStream(File file, String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      GcpKmsConfig gcpKmsConfig = gcpSecretsManagerService.getGcpKmsConfig(accountId, encryptedData.getKmsId());
      checkNotNull(gcpKmsConfig, "KMS configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      byte[] bytes = Files.toByteArray(file);
      encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(bytes)).array());
      char[] decrypt = decrypt(encryptedData, accountId, gcpKmsConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new SecretManagementException(
          KMS_OPERATION_ERROR, "Failed to decrypt data into an output stream", ioe, USER);
    }
  }
}
