package software.wings.service.impl.security;

import static io.harness.persistence.UpdatedAtAware.LAST_UPDATED_AT_KEY;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.service.intfc.security.SecretManager.ID_KEY;

import com.google.inject.Inject;

import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.persistence.HPersistence;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.BaseFile;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.impl.security.gcpkms.GcpKmsEncryptDecryptClient;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.ByteArrayInputStream;
import java.nio.CharBuffer;
import java.util.Arrays;

@Slf4j
public class GlobalEncryptDecryptClient {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject private KmsService kmsService;
  @Inject private FileService fileService;
  @Inject private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient;
  @Inject private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  @Inject private SecretManager secretManager;
  @Inject private FeatureFlagService featureFlagService;

  public EncryptedData encrypt(String accountId, char[] value, KmsConfig kmsConfig) {
    if (!kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
      throw new SecretManagementException(
          String.format("The kms config %s is not the global kms", kmsConfig.getUuid()));
    }
    return (EncryptedData) kmsEncryptDecryptClient.encrypt(accountId, value, kmsConfig);
  }

  public EncryptedData encrypt(String accountId, char[] value, GcpKmsConfig gcpKmsConfig) {
    if (!gcpKmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
      throw new SecretManagementException(
          String.format("The gcp kms config %s is not the global kms", gcpKmsConfig.getUuid()));
    }
    EncryptedData encryptedData =
        (EncryptedData) gcpKmsEncryptDecryptClient.encrypt(String.valueOf(value), accountId, gcpKmsConfig, null);
    KmsConfig backupKmsConfig = getAWSGlobalSecretManager();
    if (backupKmsConfig != null) {
      EncryptedData backupEncryptedData =
          (EncryptedData) kmsEncryptDecryptClient.encrypt(accountId, value, backupKmsConfig);
      encryptedData.setBackupEncryptionKey(backupEncryptedData.getEncryptionKey());
      encryptedData.setBackupEncryptedValue(backupEncryptedData.getEncryptedValue());
      encryptedData.setBackupKmsId(backupEncryptedData.getKmsId());
      encryptedData.setBackupEncryptionType(backupEncryptedData.getEncryptionType());
    }
    return encryptedData;
  }

  public char[] decrypt(EncryptedData encryptedData, String accountId, KmsConfig kmsConfig) {
    if (!kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
      throw new SecretManagementException(
          String.format("The kms config %s is not the global kms", kmsConfig.getUuid()));
    }
    char[] plainTextSecret = kmsEncryptDecryptClient.decrypt(encryptedData, kmsConfig);
    if (featureFlagService.isEnabled(FeatureName.SWITCH_GLOBAL_TO_GCP_KMS, accountId)) {
      try {
        GcpKmsConfig gcpKmsConfig = getGcpGlobalSecretManager();
        if (gcpKmsConfig != null) {
          EncryptedData newEncryptedData = (EncryptedData) gcpKmsEncryptDecryptClient.encrypt(
              String.valueOf(plainTextSecret), accountId, gcpKmsConfig, null);

          encryptedData.setBackupEncryptionType(encryptedData.getEncryptionType());
          encryptedData.setBackupKmsId(encryptedData.getKmsId());
          encryptedData.setBackupEncryptedValue(encryptedData.getEncryptedValue());
          encryptedData.setBackupEncryptionKey(encryptedData.getEncryptionKey());

          encryptedData.setEncryptionType(newEncryptedData.getEncryptionType());
          encryptedData.setKmsId(newEncryptedData.getKmsId());
          encryptedData.setEncryptionKey(newEncryptedData.getEncryptionKey());
          encryptedData.setEncryptedValue(newEncryptedData.getEncryptedValue());

          char[] newPlainTextSecret = gcpKmsEncryptDecryptClient.decrypt(encryptedData, gcpKmsConfig);
          if (Arrays.equals(plainTextSecret, newPlainTextSecret)) {
            if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
              String fileId = createFile(encryptedData.getEncryptedValue(), encryptedData.getName(), accountId);
              encryptedData.setEncryptedValue(fileId.toCharArray());
              String backupFileId =
                  createFile(encryptedData.getBackupEncryptedValue(), encryptedData.getName(), accountId);
              encryptedData.setBackupEncryptedValue(backupFileId.toCharArray());
            }

            Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                             .field(ID_KEY)
                                             .equal(encryptedData.getUuid())
                                             .field(LAST_UPDATED_AT_KEY)
                                             .equal(encryptedData.getLastUpdatedAt());

            UpdateOperations<EncryptedData> updateOperations =
                wingsPersistence.createUpdateOperations(EncryptedData.class)
                    .set(EncryptedDataKeys.encryptionType, encryptedData.getEncryptionType())
                    .set(EncryptedDataKeys.encryptedValue, encryptedData.getEncryptedValue())
                    .set(EncryptedDataKeys.kmsId, encryptedData.getKmsId())
                    .set(EncryptedDataKeys.encryptionKey, encryptedData.getEncryptionKey())
                    .set(EncryptedDataKeys.backupEncryptionType, encryptedData.getBackupEncryptionType())
                    .set(EncryptedDataKeys.backupEncryptedValue, encryptedData.getBackupEncryptedValue())
                    .set(EncryptedDataKeys.backupKmsId, encryptedData.getBackupKmsId())
                    .set(EncryptedDataKeys.backupEncryptionKey, encryptedData.getBackupEncryptionKey());

            EncryptedData savedEncryptedData =
                wingsPersistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);
            if (savedEncryptedData == null) {
              throw new SecretManagementException("Migration failed. The new encrypted data could not be saved");
            }
          } else {
            throw new SecretManagementException(
                "Migration failed. The plain text values did not match for encryptedData");
          }
        }
      } catch (Exception e) {
        logger.error("Migration failed for encrypted data {} due to error", encryptedData.getUuid(), e);
      }
    }
    return plainTextSecret;
  }

  private String createFile(char[] encryptedValue, String name, String accountId) {
    BaseFile baseFile = new BaseFile();
    baseFile.setFileName(name);
    baseFile.setAccountId(accountId);
    baseFile.setFileUuid(UUIDGenerator.generateUuid());
    return fileService.saveFile(
        baseFile, new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(encryptedValue)).array()), CONFIGS);
  }

  public char[] decrypt(EncryptedData encryptedData, GcpKmsConfig gcpKmsConfig) {
    if (!gcpKmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
      throw new SecretManagementException(
          String.format("The gcp kms config %s is not the global kms", gcpKmsConfig.getUuid()));
    }
    try {
      return gcpKmsEncryptDecryptClient.decrypt(encryptedData, gcpKmsConfig);
    } catch (Exception e) {
      logger.error("Decryption failed due to the following error", e);
    }

    KmsConfig kmsConfig = getAWSGlobalSecretManager();
    if (kmsConfig != null) {
      EncryptedData newEncryptedData = EncryptedData.builder()
                                           .name(encryptedData.getName())
                                           .accountId(encryptedData.getAccountId())
                                           .encryptionKey(encryptedData.getBackupEncryptionKey())
                                           .encryptionType(encryptedData.getBackupEncryptionType())
                                           .encryptedValue(encryptedData.getBackupEncryptedValue())
                                           .kmsId(encryptedData.getBackupKmsId())
                                           .type(encryptedData.getType())
                                           .enabled(encryptedData.isEnabled())
                                           .parentIds(encryptedData.getParentIds())
                                           .base64Encoded(encryptedData.isBase64Encoded())
                                           .path(encryptedData.getPath())
                                           .fileSize(encryptedData.getFileSize())
                                           .build();
      newEncryptedData.setUuid(encryptedData.getUuid());
      return kmsEncryptDecryptClient.decrypt(newEncryptedData, kmsConfig);
    }
    throw new SecretManagementException(
        "Could not decrypt encrypted data using both the primary and backup global secret managers");
  }

  EncryptedRecordData convertEncryptedRecordToLocallyEncrypted(
      EncryptedData encryptedRecord, String accountId, EncryptionConfig encryptionConfig) {
    try {
      char[] decryptedValue;
      if (encryptionConfig.getEncryptionType() == GCP_KMS) {
        decryptedValue = decrypt(encryptedRecord, (GcpKmsConfig) encryptionConfig);
      } else {
        decryptedValue = decrypt(encryptedRecord, accountId, (KmsConfig) encryptionConfig);
      }
      String randomEncryptionKey = UUIDGenerator.generateUuid();
      char[] reEncryptedValue = new SimpleEncryption(randomEncryptionKey).encryptChars(decryptedValue);

      return EncryptedRecordData.builder()
          .uuid(encryptedRecord.getUuid())
          .name(encryptedRecord.getName())
          .encryptionType(LOCAL)
          .encryptionKey(randomEncryptionKey)
          .encryptedValue(reEncryptedValue)
          .build();
    } catch (DelegateRetryableException | SecretManagementDelegateException e) {
      logger.warn(
          "Failed to decrypt secret {} with secret manager {}. Falling back to decrypt this secret using delegate",
          encryptedRecord.getUuid(), encryptionConfig.getUuid(), e);
      // This means we are falling back to use delegate to decrypt.
      return SecretManager.buildRecordData(encryptedRecord);
    }
  }

  private KmsConfig getAWSGlobalSecretManager() {
    return kmsService.getGlobalKmsConfig();
  }

  private GcpKmsConfig getGcpGlobalSecretManager() {
    return gcpSecretsManagerService.getGlobalKmsConfig();
  }
}
