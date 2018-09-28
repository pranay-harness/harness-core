package software.wings.service.impl.security;

import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;

import io.harness.exception.KmsOperationException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Base;
import software.wings.beans.BaseFile;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.common.Constants;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsServiceImpl extends AbstractSecretServiceImpl implements KmsService {
  @Inject private FileService fileService;

  @Override
  public EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null || value == null) {
      return encryptLocal(value);
    }
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .encrypt(accountId, value, kmsConfig);
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null || data.getEncryptedValue() == null) {
      return decryptLocal(data);
    }
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext).decrypt(data, kmsConfig);
  }

  @Override
  public KmsConfig getSecretConfig(String accountId) {
    KmsConfig kmsConfig =
        wingsPersistence.createQuery(KmsConfig.class).filter("accountId", accountId).filter("isDefault", true).get();

    if (kmsConfig == null) {
      kmsConfig = wingsPersistence.createQuery(KmsConfig.class).filter("accountId", Base.GLOBAL_ACCOUNT_ID).get();
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
    validateKms(accountId, kmsConfig);
    return saveKmsConfigInternal(accountId, kmsConfig);
  }

  private String saveKmsConfigInternal(String accountId, KmsConfig kmsConfig) {
    kmsConfig.setAccountId(accountId);

    Query<VaultConfig> vaultConfigQuery =
        wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId);
    List<VaultConfig> vaultConfigs = vaultConfigQuery.asList();

    EncryptedData accessKeyData = encrypt(kmsConfig.getAccessKey().toCharArray(), accountId, null);
    if (isNotBlank(kmsConfig.getUuid())) {
      EncryptedData savedAccessKey = wingsPersistence.get(
          EncryptedData.class, wingsPersistence.get(KmsConfig.class, kmsConfig.getUuid()).getAccessKey());
      Preconditions.checkNotNull(savedAccessKey, "reference is null for " + kmsConfig.getUuid());
      savedAccessKey.setEncryptionKey(accessKeyData.getEncryptionKey());
      savedAccessKey.setEncryptedValue(accessKeyData.getEncryptedValue());
      accessKeyData = savedAccessKey;
    }
    accessKeyData.setAccountId(accountId);
    accessKeyData.setType(SettingVariableTypes.KMS);
    accessKeyData.setName(kmsConfig.getName() + "_accessKey");
    String accessKeyId = wingsPersistence.save(accessKeyData);
    kmsConfig.setAccessKey(accessKeyId);

    EncryptedData secretKeyData = encrypt(kmsConfig.getSecretKey().toCharArray(), accountId, null);
    if (isNotBlank(kmsConfig.getUuid())) {
      EncryptedData savedSecretKey = wingsPersistence.get(
          EncryptedData.class, wingsPersistence.get(KmsConfig.class, kmsConfig.getUuid()).getSecretKey());
      Preconditions.checkNotNull(savedSecretKey, "reference is null for " + kmsConfig.getUuid());
      savedSecretKey.setEncryptionKey(secretKeyData.getEncryptionKey());
      savedSecretKey.setEncryptedValue(secretKeyData.getEncryptedValue());
      secretKeyData = savedSecretKey;
    }
    secretKeyData.setAccountId(accountId);
    secretKeyData.setType(SettingVariableTypes.KMS);
    secretKeyData.setName(kmsConfig.getName() + "_secretKey");
    String secretKeyId = wingsPersistence.save(secretKeyData);
    kmsConfig.setSecretKey(secretKeyId);

    EncryptedData arnKeyData = encrypt(kmsConfig.getKmsArn().toCharArray(), accountId, null);
    if (isNotBlank(kmsConfig.getUuid())) {
      EncryptedData savedArn = wingsPersistence.get(
          EncryptedData.class, wingsPersistence.get(KmsConfig.class, kmsConfig.getUuid()).getKmsArn());
      Preconditions.checkNotNull(savedArn, "reference is null for " + kmsConfig.getUuid());
      savedArn.setEncryptionKey(arnKeyData.getEncryptionKey());
      savedArn.setEncryptedValue(arnKeyData.getEncryptedValue());
      arnKeyData = savedArn;
    }
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

    Query<KmsConfig> query = wingsPersistence.createQuery(KmsConfig.class).filter("accountId", accountId);
    Collection<KmsConfig> savedConfigs = query.asList();
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
    final long count = wingsPersistence.createQuery(EncryptedData.class)
                           .filter("accountId", accountId)
                           .filter("kmsId", kmsConfigId)
                           .filter("encryptionType", EncryptionType.KMS)
                           .count(new CountOptions().limit(1));

    if (count > 0) {
      String message = "Can not delete the kms configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to a new kms and then try again";
      throw new KmsOperationException(message, USER_SRE);
    }

    wingsPersistence.delete(KmsConfig.class, kmsConfigId);
    Query<EncryptedData> deleteQuery =
        wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(kmsConfigId);
    return wingsPersistence.delete(deleteQuery);
  }

  @Override
  public Collection<KmsConfig> listKmsConfigs(String accountId, boolean maskSecret) {
    List<KmsConfig> rv = new ArrayList<>();

    try (HIterator<KmsConfig> iterator = new HIterator(wingsPersistence.createQuery(KmsConfig.class)
                                                           .field("accountId")
                                                           .in(Lists.newArrayList(accountId, Base.GLOBAL_ACCOUNT_ID))
                                                           .order("-createdAt")
                                                           .fetch())) {
      KmsConfig globalConfig = null;
      boolean defaultSet = false;
      while (iterator.hasNext()) {
        KmsConfig kmsConfig = iterator.next();
        Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                      .filter("accountId", accountId)
                                                      .filter("kmsId", kmsConfig.getUuid());
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
          kmsConfig.setSecretKey(Constants.SECRET_MASK);
          kmsConfig.setKmsArn(Constants.SECRET_MASK);
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
  }

  @Override
  public EncryptedData encryptFile(String accountId, KmsConfig kmsConfig, String name, BoundedInputStream inputStream) {
    try {
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

  @Override
  public void decryptToStream(File file, String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      KmsConfig kmsConfig = getSecretConfig(accountId);
      Preconditions.checkNotNull(kmsConfig);
      Preconditions.checkNotNull(encryptedData);
      byte[] bytes = Files.toByteArray(file);
      encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(bytes)).array());
      char[] decrypt = decrypt(encryptedData, accountId, kmsConfig);
      output.write(CHARSET.encode(CharBuffer.wrap(decrypt)).array(), 0, decrypt.length);
      output.flush();
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  private void validateKms(String accountId, KmsConfig kmsConfig) {
    try {
      encrypt(UUID.randomUUID().toString().toCharArray(), accountId, kmsConfig);
    } catch (WingsException e) {
      String message = "Was not able to encrypt using given credentials. Please check your credentials and try again";
      throw new KmsOperationException(message, USER);
    }
  }

  @Override
  public KmsConfig getKmsConfig(String accountId, String entityId) {
    KmsConfig kmsConfig = wingsPersistence.createQuery(KmsConfig.class)
                              .field("accountId")
                              .in(Lists.newArrayList(accountId, Base.GLOBAL_ACCOUNT_ID))
                              .filter("_id", entityId)
                              .get();

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
