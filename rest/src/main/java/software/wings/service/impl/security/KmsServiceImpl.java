package software.wings.service.impl.security;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.kms.KmsDelegateService;
import software.wings.service.intfc.kms.KmsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Iterator;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsServiceImpl implements KmsService {
  private static final Logger logger = LoggerFactory.getLogger(KmsServiceImpl.class);

  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public EncryptedData encrypt(char[] value, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      logger.warn("Kms service not configured, encrypting locally");
      return encryptLocal(value);
    }
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(kmsConfig.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(KmsDelegateService.class, syncTaskContext).encrypt(value, kmsConfig);
    } catch (Exception e) {
      throw new WingsException("Encryption with kms failed", e);
    }
  }

  @Override
  public char[] decrypt(EncryptedData data, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      logger.warn("Kms service not configured, decrypting locally");
      return decryptLocal(data);
    }
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(kmsConfig.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(KmsDelegateService.class, syncTaskContext).decrypt(data, kmsConfig);
    } catch (Exception e) {
      throw new WingsException("Decryption with kms failed", e);
    }
  }

  @Override
  public KmsConfig getKmsConfig(String accountId) {
    KmsConfig kmsConfig = null;
    Iterator<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                           .field("accountId")
                                           .equal(accountId)
                                           .field("value.type")
                                           .equal(SettingVariableTypes.KMS)
                                           .fetch(new FindOptions().limit(1));
    if (query.hasNext()) {
      kmsConfig = (KmsConfig) query.next().getValue();
    } else {
      query = wingsPersistence.createQuery(SettingAttribute.class)
                  .field("accountId")
                  .equal(Base.GLOBAL_ACCOUNT_ID)
                  .field("value.type")
                  .equal(SettingVariableTypes.KMS)
                  .fetch(new FindOptions().limit(1));

      if (query.hasNext()) {
        kmsConfig = (KmsConfig) query.next().getValue();
      }
    }

    if (kmsConfig != null) {
      kmsConfig.setAccessKey(decryptKey(kmsConfig.getAccessKey()));
      kmsConfig.setSecretKey(decryptKey(kmsConfig.getSecretKey()));
      kmsConfig.setKmsArn(decryptKey(kmsConfig.getKmsArn()));
    }

    return kmsConfig;
  }

  private char[] decryptKey(char[] key) {
    final EncryptedData encryptedData = wingsPersistence.getDatastore().get(EncryptedData.class, new String(key));
    return decrypt(encryptedData, null);
  }

  @Override
  public boolean saveKmsConfig(String accountId, String name, KmsConfig kmsConfig) {
    kmsConfig.setType(SettingVariableTypes.KMS.name());
    kmsConfig.setAccountId(accountId);

    EncryptedData accessKeyData = encrypt(kmsConfig.getAccessKey(), null);
    accessKeyData.setAccountId(accountId);
    accessKeyData.setType(SettingVariableTypes.KMS);
    String accessKeyId = (String) wingsPersistence.getDatastore().save(accessKeyData).getId();
    kmsConfig.setAccessKey(accessKeyId.toCharArray());

    EncryptedData secretKeyData = encrypt(kmsConfig.getSecretKey(), null);
    accessKeyData.setAccountId(accountId);
    accessKeyData.setType(SettingVariableTypes.KMS);
    String secretKeyId = (String) wingsPersistence.getDatastore().save(secretKeyData).getId();

    EncryptedData arnKeyData = encrypt(kmsConfig.getKmsArn(), null);
    accessKeyData.setAccountId(accountId);
    accessKeyData.setType(SettingVariableTypes.KMS);
    String arnKeyId = (String) wingsPersistence.getDatastore().save(arnKeyData).getId();

    kmsConfig.setSecretKey(secretKeyId.toCharArray());
    kmsConfig.setKmsArn(arnKeyId.toCharArray());

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(accountId)
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withName(name)
                                            .withValue(kmsConfig)
                                            .build();
    String parentId = (String) wingsPersistence.getDatastore().save(settingAttribute).getId();

    accessKeyData.setParentId(parentId);
    wingsPersistence.getDatastore().save(accessKeyData);

    secretKeyData.setParentId(parentId);
    wingsPersistence.getDatastore().save(secretKeyData);

    arnKeyData.setParentId(parentId);
    wingsPersistence.getDatastore().save(arnKeyData);

    return true;
  }

  private EncryptedData encryptLocal(char[] value) {
    final String encryptionKey = UUID.randomUUID().toString();
    final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptionKey);
    char[] encryptChars = simpleEncryption.encryptChars(value);

    return EncryptedData.builder().encryptionKey(encryptionKey).encryptedValue(encryptChars).build();
  }

  private char[] decryptLocal(EncryptedData data) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }
}
