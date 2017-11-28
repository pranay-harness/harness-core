package software.wings.service.impl.security;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Base;
import software.wings.beans.BaseFile;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ErrorCode;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsServiceImpl extends AbstractSecretServiceImpl implements KmsService {
  public static final String SECRET_MASK = "**************";

  @Inject private FeatureFlagService featureFlagService;
  @Inject private FileService fileService;

  @Override
  public EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      return encryptLocal(value);
    }
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
          .encrypt(accountId, value, kmsConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", e.getMessage());
    }
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      return decryptLocal(data);
    }
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext).decrypt(data, kmsConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", e.getMessage());
    }
  }

  @Override
  public KmsConfig getSecretConfig(String accountId) {
    KmsConfig kmsConfig = null;
    Iterator<KmsConfig> query = wingsPersistence.createQuery(KmsConfig.class)
                                    .field("accountId")
                                    .equal(accountId)
                                    .field("isDefault")
                                    .equal(true)
                                    .fetch(new FindOptions().limit(1));
    if (query.hasNext()) {
      kmsConfig = query.next();
    } else {
      logger.info("No kms setup for account {}. Using harness's kms", accountId);
      query = wingsPersistence.createQuery(KmsConfig.class)
                  .field("accountId")
                  .equal(Base.GLOBAL_ACCOUNT_ID)
                  .fetch(new FindOptions().limit(1));

      if (query.hasNext()) {
        kmsConfig = query.next();
      }
    }

    if (kmsConfig != null) {
      kmsConfig.setAccessKey(new String(decryptKey(kmsConfig.getAccessKey().toCharArray())));
      kmsConfig.setSecretKey(new String(decryptKey(kmsConfig.getSecretKey().toCharArray())));
      kmsConfig.setKmsArn(new String(decryptKey(kmsConfig.getKmsArn().toCharArray())));
    }

    return kmsConfig;
  }

  private char[] decryptKey(char[] key) {
    final EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, new String(key));
    return decrypt(encryptedData, null, null);
  }

  @Override
  public String saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig) {
    validateKms(accountId, kmsConfig);
    return saveKmsConfigInternal(Base.GLOBAL_ACCOUNT_ID, kmsConfig);
  }

  @Override
  public String saveKmsConfig(String accountId, KmsConfig kmsConfig) {
    try {
      validateKms(accountId, kmsConfig);
    } catch (Exception e) {
      throw new WingsException(
          ErrorCode.KMS_OPERATION_ERROR, "reason", "Validation failed. Please check your credentials");
    }
    return saveKmsConfigInternal(accountId, kmsConfig);
  }

  private String saveKmsConfigInternal(String accountId, KmsConfig kmsConfig) {
    kmsConfig.setAccountId(accountId);
    Query<KmsConfig> query = wingsPersistence.createQuery(KmsConfig.class).field("accountId").equal(accountId);
    Collection<KmsConfig> savedConfigs = query.asList();
    Query<VaultConfig> vaultConfigQuery =
        wingsPersistence.createQuery(VaultConfig.class).field("accountId").equal(accountId);
    List<VaultConfig> vaultConfigs = vaultConfigQuery.asList();

    EncryptedData accessKeyData = encrypt(kmsConfig.getAccessKey().toCharArray(), accountId, null);
    accessKeyData.setAccountId(accountId);
    accessKeyData.setType(SettingVariableTypes.KMS);
    accessKeyData.setName(kmsConfig.getName() + "_accessKey");
    String accessKeyId = wingsPersistence.save(accessKeyData);
    kmsConfig.setAccessKey(accessKeyId);

    EncryptedData secretKeyData = encrypt(kmsConfig.getSecretKey().toCharArray(), accountId, null);
    secretKeyData.setAccountId(accountId);
    secretKeyData.setType(SettingVariableTypes.KMS);
    secretKeyData.setName(kmsConfig.getName() + "_secretKey");
    String secretKeyId = wingsPersistence.save(secretKeyData);
    kmsConfig.setSecretKey(secretKeyId);

    EncryptedData arnKeyData = encrypt(kmsConfig.getKmsArn().toCharArray(), accountId, null);
    arnKeyData.setAccountId(accountId);
    arnKeyData.setType(SettingVariableTypes.KMS);
    arnKeyData.setName(kmsConfig.getName() + "_arn");
    String arnKeyId = wingsPersistence.save(arnKeyData);
    kmsConfig.setKmsArn(arnKeyId);

    String parentId = wingsPersistence.save(kmsConfig);

    accessKeyData.addParent(parentId);
    wingsPersistence.save(accessKeyData);

    secretKeyData.addParent(parentId);
    wingsPersistence.save(secretKeyData);

    arnKeyData.addParent(parentId);
    wingsPersistence.save(arnKeyData);

    if (kmsConfig.isDefault() && (!savedConfigs.isEmpty() || !vaultConfigs.isEmpty())) {
      for (KmsConfig savedConfig : savedConfigs) {
        if (kmsConfig.getUuid().equals(savedConfig.getUuid())) {
          continue;
        }
        if (savedConfig.isDefault()) {
          savedConfig.setDefault(false);
          wingsPersistence.save(savedConfig);
        }
      }

      for (VaultConfig vaultConfig : vaultConfigs) {
        if (vaultConfig.isDefault()) {
          vaultConfig.setDefault(false);
          wingsPersistence.save(vaultConfig);
        }
      }
    }

    return parentId;
  }

  @Override
  public boolean deleteKmsConfig(String accountId, String kmsConfigId) {
    Iterator<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                        .field("accountId")
                                        .equal(accountId)
                                        .field("kmsId")
                                        .equal(kmsConfigId)
                                        .field("encryptionType")
                                        .equal(EncryptionType.KMS)
                                        .fetch(new FindOptions().limit(1));

    if (query.hasNext()) {
      String message = "Can not delete the kms configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to a new kms and then try again";
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", message);
    }

    wingsPersistence.delete(KmsConfig.class, kmsConfigId);
    Query<EncryptedData> deleteQuery =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(kmsConfigId);
    return wingsPersistence.delete(deleteQuery);
  }

  @Override
  public Collection<KmsConfig> listKmsConfigs(String accountId, boolean maskSecret) {
    List<KmsConfig> rv = new ArrayList<>();
    Iterator<KmsConfig> query = wingsPersistence.createQuery(KmsConfig.class)
                                    .field("accountId")
                                    .in(Lists.newArrayList(accountId, Base.GLOBAL_ACCOUNT_ID))
                                    .order("-createdAt")
                                    .fetch();

    KmsConfig globalConfig = null;
    boolean defaultSet = false;
    while (query.hasNext()) {
      KmsConfig kmsConfig = query.next();
      Query<EncryptedData> encryptedDataQuery =
          wingsPersistence.createQuery(EncryptedData.class).field("kmsId").equal(kmsConfig.getUuid());
      kmsConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
      if (kmsConfig.isDefault()) {
        defaultSet = true;
      }

      if (kmsConfig.getAccountId().equals(Base.GLOBAL_ACCOUNT_ID)) {
        globalConfig = kmsConfig;
      }
      EncryptedData accessKeyData = wingsPersistence.get(EncryptedData.class, kmsConfig.getAccessKey());
      Preconditions.checkNotNull(accessKeyData, "encrypted accessKey can't be null for " + kmsConfig);
      kmsConfig.setAccessKey(new String(decrypt(accessKeyData, null, null)));

      EncryptedData arnData = wingsPersistence.get(EncryptedData.class, kmsConfig.getKmsArn());
      Preconditions.checkNotNull(arnData, "encrypted arn can't be null for " + kmsConfig);
      kmsConfig.setKmsArn(new String(decrypt(arnData, null, null)));

      if (maskSecret) {
        kmsConfig.setSecretKey(SECRET_MASK);
      } else {
        EncryptedData secretData = wingsPersistence.get(EncryptedData.class, kmsConfig.getSecretKey());
        Preconditions.checkNotNull(secretData, "encrypted secret key can't be null for " + kmsConfig);
        kmsConfig.setSecretKey(new String(decrypt(secretData, null, null)));
      }
      kmsConfig.setEncryptionType(EncryptionType.KMS);
      rv.add(kmsConfig);
    }

    if (!defaultSet && globalConfig != null) {
      globalConfig.setDefault(true);
    }
    return rv;
  }

  @Override
  public boolean transitionKms(String accountId, String fromKmsId, String toKmsId) {
    return transitionSecretStore(accountId, fromKmsId, toKmsId, EncryptionType.KMS);
  }

  @Override
  public void changeKms(String accountId, String entityId, String fromKmsId, String toKmsId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    Preconditions.checkNotNull(encryptedData, "No encrypted data with id " + entityId);
    KmsConfig fromConfig = getKmsConfig(accountId, fromKmsId);
    Preconditions.checkNotNull(fromConfig, "No kms found for account " + accountId + " with id " + entityId);
    KmsConfig toConfig = getKmsConfig(accountId, toKmsId);
    Preconditions.checkNotNull(toConfig, "No kms found for account " + accountId + " with id " + entityId);

    char[] decrypted = decrypt(encryptedData, accountId, fromConfig);
    EncryptedData encrypted = encrypt(decrypted, accountId, toConfig);
    encryptedData.setKmsId(toKmsId);
    encryptedData.setEncryptionKey(encrypted.getEncryptionKey());
    encryptedData.setEncryptedValue(encrypted.getEncryptedValue());

    wingsPersistence.save(encryptedData);
  }

  @Override
  public EncryptedData encryptFile(String accountId, String name, BoundedInputStream inputStream) {
    try {
      KmsConfig kmsConfig = getSecretConfig(accountId);
      Preconditions.checkNotNull(kmsConfig);
      byte[] bytes = ByteStreams.toByteArray(inputStream);
      EncryptedData fileData = encrypt(CHARSET.decode(ByteBuffer.wrap(bytes)).array(), accountId, kmsConfig);
      fileData.setName(name);
      fileData.setAccountId(accountId);
      fileData.setType(SettingVariableTypes.CONFIG_FILE);
      char[] encryptedValue = fileData.getEncryptedValue();
      BaseFile baseFile = new BaseFile();
      baseFile.setFileName(name);
      String fileId = fileService.saveFile(
          baseFile, new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(encryptedValue)).array()), CONFIGS);
      fileData.setEncryptedValue(fileId.toCharArray());
      fileData.setFileSize(inputStream.getTotalBytesRead());
      return fileData;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    try {
      KmsConfig kmsConfig = getSecretConfig(accountId);
      Preconditions.checkNotNull(kmsConfig);
      Preconditions.checkNotNull(encryptedData);
      byte[] bytes = Files.toByteArray(file);
      encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(bytes)).array());
      char[] decrypt = decrypt(encryptedData, accountId, kmsConfig);
      Files.write(CHARSET.encode(CharBuffer.wrap(decrypt)).array(), file);
      return file;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  private void validateKms(String accountId, KmsConfig kmsConfig) {
    encrypt(UUID.randomUUID().toString().toCharArray(), accountId, kmsConfig);
  }

  @Override
  public KmsConfig getKmsConfig(String accountId, String entityId) {
    KmsConfig kmsConfig = null;
    Iterator<KmsConfig> query = wingsPersistence.createQuery(KmsConfig.class)
                                    .field("accountId")
                                    .in(Lists.newArrayList(accountId, Base.GLOBAL_ACCOUNT_ID))
                                    .field("_id")
                                    .equal(entityId)
                                    .fetch(new FindOptions().limit(1));
    if (query.hasNext()) {
      kmsConfig = query.next();
    }

    if (kmsConfig != null) {
      kmsConfig.setAccessKey(new String(decryptKey(kmsConfig.getAccessKey().toCharArray())));
      kmsConfig.setSecretKey(new String(decryptKey(kmsConfig.getSecretKey().toCharArray())));
      kmsConfig.setKmsArn(new String(decryptKey(kmsConfig.getKmsArn().toCharArray())));
    }

    return kmsConfig;
  }

  private EncryptedData encryptLocal(char[] value) {
    final String encryptionKey = UUID.randomUUID().toString();
    final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptionKey);
    char[] encryptChars = simpleEncryption.encryptChars(value);

    return EncryptedData.builder()
        .encryptionKey(encryptionKey)
        .encryptedValue(encryptChars)
        .encryptionType(EncryptionType.LOCAL)
        .build();
  }

  private char[] decryptLocal(EncryptedData data) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }
}
