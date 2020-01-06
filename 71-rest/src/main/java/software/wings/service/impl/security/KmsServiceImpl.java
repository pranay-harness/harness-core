package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.KMS_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.ENCRYPTION_TYPE_KEY;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.BaseFile;
import software.wings.beans.KmsConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by rsingh on 9/29/17.
 */
@Singleton
@Slf4j
public class KmsServiceImpl extends AbstractSecretServiceImpl implements KmsService {
  @Inject private FileService fileService;
  @Inject private GlobalEncryptDecryptClient globalEncryptDecryptClient;

  @Override
  public EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null || value == null) {
      return encryptLocal(value);
    }

    if (GLOBAL_ACCOUNT_ID.equals(kmsConfig.getAccountId())) {
      // PL-1836: Perform encrypt/decrypt at manager side for global shared KMS.
      logger.info("Encrypt secret with global KMS secret manager for account {}", accountId);
      return globalEncryptDecryptClient.encrypt(accountId, value, kmsConfig);
    } else {
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(accountId)
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                            .build();
      return (EncryptedData) delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .encrypt(accountId, value, kmsConfig);
    }
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null || data.getEncryptedValue() == null) {
      return decryptLocal(data);
    }

    if (GLOBAL_ACCOUNT_ID.equals(kmsConfig.getAccountId())) {
      // PL-1836: Perform encrypt/decrypt at manager side for global shared KMS.
      logger.info("Decrypt secret with global KMS secret manager for account {}", accountId);
      return globalEncryptDecryptClient.decrypt(data, accountId, kmsConfig);
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
              .decrypt(data, kmsConfig);
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
  public String saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig) {
    return saveKmsConfigInternal(GLOBAL_ACCOUNT_ID, kmsConfig);
  }

  @Override
  public KmsConfig getGlobalKmsConfig() {
    KmsConfig globalKmsConfig = wingsPersistence.createQuery(KmsConfig.class)
                                    .field(ACCOUNT_ID_KEY)
                                    .equal(GLOBAL_ACCOUNT_ID)
                                    .field(ENCRYPTION_TYPE_KEY)
                                    .equal(EncryptionType.KMS)
                                    .get();
    if (globalKmsConfig == null) {
      return null;
    }
    // Secrets field of raw KmsConfig are encrypted record IDs. It needs to be decrypted to be used.
    decryptKmsConfigSecrets(globalKmsConfig);
    return globalKmsConfig;
  }

  @Override
  public String saveKmsConfig(String accountId, KmsConfig kmsConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    return saveKmsConfigInternal(accountId, kmsConfig);
  }

  private String saveKmsConfigInternal(String accountId, KmsConfig kmsConfig) {
    kmsConfig.setAccountId(accountId);

    KmsConfig oldConfigForAudit = null;
    KmsConfig savedKmsConfig = null;
    boolean credentialChanged = true;
    if (isNotEmpty(kmsConfig.getUuid())) {
      savedKmsConfig = getKmsConfig(accountId, kmsConfig.getUuid());
      // Replaced masked secrets with the real secret value.
      if (SECRET_MASK.equals(kmsConfig.getSecretKey())) {
        kmsConfig.setSecretKey(savedKmsConfig.getSecretKey());
      }
      if (SECRET_MASK.equals(kmsConfig.getKmsArn())) {
        kmsConfig.setKmsArn(savedKmsConfig.getKmsArn());
      }
      credentialChanged = !Objects.equals(kmsConfig.getRegion(), savedKmsConfig.getRegion())
          || !Objects.equals(kmsConfig.getAccessKey(), savedKmsConfig.getAccessKey())
          || !Objects.equals(kmsConfig.getSecretKey(), savedKmsConfig.getSecretKey())
          || !Objects.equals(kmsConfig.getKmsArn(), savedKmsConfig.getKmsArn());

      // secret field un-decrypted version of saved KMS config
      savedKmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfig.getUuid());
      oldConfigForAudit = KryoUtils.clone(savedKmsConfig);
    }

    // Validate every time when secret manager config change submitted
    validateKms(accountId, kmsConfig);

    if (!credentialChanged) {
      savedKmsConfig.setName(kmsConfig.getName());
      savedKmsConfig.setDefault(kmsConfig.isDefault());

      // PL-3237: Audit secret manager config changes.
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedKmsConfig);

      return secretManagerConfigService.save(savedKmsConfig);
    }

    EncryptedData accessKeyData = encryptLocal(kmsConfig.getAccessKey().toCharArray());
    if (isNotBlank(kmsConfig.getUuid())) {
      EncryptedData savedAccessKey = wingsPersistence.get(EncryptedData.class, savedKmsConfig.getAccessKey());
      checkNotNull(savedAccessKey, "Access key reference is null for KMS secret manager " + kmsConfig.getUuid());
      savedAccessKey.setEncryptionKey(accessKeyData.getEncryptionKey());
      savedAccessKey.setEncryptedValue(accessKeyData.getEncryptedValue());
      accessKeyData = savedAccessKey;
    }
    accessKeyData.setAccountId(accountId);
    accessKeyData.setType(SettingVariableTypes.KMS);
    accessKeyData.setName(kmsConfig.getName() + "_accessKey");
    String accessKeyId = wingsPersistence.save(accessKeyData);
    kmsConfig.setAccessKey(accessKeyId);

    EncryptedData secretKeyData = encryptLocal(kmsConfig.getSecretKey().toCharArray());
    if (isNotBlank(kmsConfig.getUuid())) {
      EncryptedData savedSecretKey = wingsPersistence.get(EncryptedData.class, savedKmsConfig.getSecretKey());
      checkNotNull(savedSecretKey, "Secret Key reference is null for KMS secret manager " + kmsConfig.getUuid());
      savedSecretKey.setEncryptionKey(secretKeyData.getEncryptionKey());
      savedSecretKey.setEncryptedValue(secretKeyData.getEncryptedValue());
      secretKeyData = savedSecretKey;
    }
    secretKeyData.setAccountId(accountId);
    secretKeyData.setType(SettingVariableTypes.KMS);
    secretKeyData.setName(kmsConfig.getName() + "_secretKey");
    String secretKeyId = wingsPersistence.save(secretKeyData);
    kmsConfig.setSecretKey(secretKeyId);

    EncryptedData arnKeyData = encryptLocal(kmsConfig.getKmsArn().toCharArray());
    if (isNotBlank(kmsConfig.getUuid())) {
      EncryptedData savedArn = wingsPersistence.get(EncryptedData.class, savedKmsConfig.getKmsArn());
      checkNotNull(savedArn, "ARN reference is null for KMS secret manager " + kmsConfig.getUuid());
      savedArn.setEncryptionKey(arnKeyData.getEncryptionKey());
      savedArn.setEncryptedValue(arnKeyData.getEncryptedValue());
      arnKeyData = savedArn;
    }
    arnKeyData.setAccountId(accountId);
    arnKeyData.setType(SettingVariableTypes.KMS);
    arnKeyData.setName(kmsConfig.getName() + "_arn");
    String arnKeyId = wingsPersistence.save(arnKeyData);
    kmsConfig.setKmsArn(arnKeyId);

    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, oldConfigForAudit, kmsConfig);

    String parentId = secretManagerConfigService.save(kmsConfig);

    accessKeyData.addParent(parentId);
    wingsPersistence.save(accessKeyData);

    secretKeyData.addParent(parentId);
    wingsPersistence.save(secretKeyData);

    arnKeyData.addParent(parentId);
    wingsPersistence.save(arnKeyData);

    return parentId;
  }

  @Override
  public boolean deleteKmsConfig(String accountId, String kmsConfigId) {
    KmsConfig kmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfigId);
    checkNotNull(kmsConfig, "No KMS secret manager found with id " + kmsConfigId);

    if (GLOBAL_ACCOUNT_ID.equals(kmsConfig.getAccountId())) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Can not delete global KMS secret manager", USER);
    }

    final long count = wingsPersistence.createQuery(EncryptedData.class)
                           .filter(EncryptedDataKeys.accountId, accountId)
                           .filter(EncryptedDataKeys.kmsId, kmsConfigId)
                           .filter(EncryptedDataKeys.encryptionType, EncryptionType.KMS)
                           .count(new CountOptions().limit(1));

    if (count > 0) {
      String message = "Can not delete the kms configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER_SRE);
    }

    Query<EncryptedData> deleteQuery =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(kmsConfigId);
    wingsPersistence.delete(deleteQuery);

    return deleteSecretManagerAndGenerateAudit(accountId, kmsConfig);
  }

  @Override
  public void decryptKmsConfigSecrets(String accountId, KmsConfig kmsConfig, boolean maskSecret) {
    EncryptedData accessKeyData = wingsPersistence.get(EncryptedData.class, kmsConfig.getAccessKey());
    checkNotNull(accessKeyData, "Access key reference is null for KMS secret manager " + kmsConfig.getUuid());
    kmsConfig.setAccessKey(new String(decryptLocal(accessKeyData)));

    if (maskSecret) {
      kmsConfig.maskSecrets();
    } else {
      EncryptedData secretData = wingsPersistence.get(EncryptedData.class, kmsConfig.getSecretKey());
      checkNotNull(secretData, "Secret Key reference is null for KMS secret manager " + kmsConfig.getUuid());
      kmsConfig.setSecretKey(new String(decryptLocal(secretData)));

      EncryptedData arnData = wingsPersistence.get(EncryptedData.class, kmsConfig.getKmsArn());
      checkNotNull(arnData, "ARN reference is null for KMS secret manager " + kmsConfig.getUuid());
      kmsConfig.setKmsArn(new String(decryptLocal(arnData)));
    }
  }

  @Override
  public EncryptedData encryptFile(String accountId, KmsConfig kmsConfig, String name, byte[] inputBytes) {
    checkNotNull(kmsConfig, "KMS secret manager can't be null");
    byte[] bytes = encodeBase64ToByteArray(inputBytes);
    EncryptedData fileData = encrypt(CHARSET.decode(ByteBuffer.wrap(bytes)).array(), accountId, kmsConfig);
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
      KmsConfig kmsConfig = getKmsConfig(accountId, encryptedData.getKmsId());
      checkNotNull(kmsConfig, "KMS configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      byte[] bytes = Files.toByteArray(file);
      encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(bytes)).array());
      char[] decrypt = decrypt(encryptedData, accountId, kmsConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      Files.write(fileData, file);
      return file;
    } catch (IOException ioe) {
      throw new SecretManagementException(KMS_OPERATION_ERROR, "Failed to decrypt data into an output file", ioe, USER);
    }
  }

  @Override
  public void decryptToStream(File file, String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      KmsConfig kmsConfig = getKmsConfig(accountId, encryptedData.getKmsId());
      checkNotNull(kmsConfig, "KMS configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      byte[] bytes = Files.toByteArray(file);
      encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(bytes)).array());
      char[] decrypt = decrypt(encryptedData, accountId, kmsConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new SecretManagementException(
          KMS_OPERATION_ERROR, "Failed to decrypt data into an output stream", ioe, USER);
    }
  }

  private void validateKms(String accountId, KmsConfig kmsConfig) {
    try {
      encrypt(UUID.randomUUID().toString().toCharArray(), accountId, kmsConfig);
    } catch (WingsException e) {
      String message = "Was not able to encrypt using given credentials. Please check your credentials and try again";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }
  }

  @Override
  public KmsConfig getKmsConfig(String accountId, String entityId) {
    KmsConfig kmsConfig = wingsPersistence.createQuery(KmsConfig.class)
                              .field("accountId")
                              .in(Lists.newArrayList(accountId, GLOBAL_ACCOUNT_ID))
                              .filter("_id", entityId)
                              .get();

    // Secrets field of raw KmsConfig are encrypted record IDs. It needs to be decrypted to be used.
    decryptKmsConfigSecrets(kmsConfig);

    return kmsConfig;
  }

  private void decryptKmsConfigSecrets(KmsConfig kmsConfig) {
    if (kmsConfig != null) {
      kmsConfig.setAccessKey(new String(decryptKey(kmsConfig.getAccessKey().toCharArray())));
      kmsConfig.setSecretKey(new String(decryptKey(kmsConfig.getSecretKey().toCharArray())));
      kmsConfig.setKmsArn(new String(decryptKey(kmsConfig.getKmsArn().toCharArray())));
    }
  }
}
