package software.wings.service.impl.security;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.common.base.Preconditions;

import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.UuidAware;
import software.wings.core.queue.Queue;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.security.KmsDelegateService;
import software.wings.service.intfc.security.KmsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsServiceImpl implements KmsService {
  private static final Logger logger = LoggerFactory.getLogger(KmsServiceImpl.class);

  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;

  @Override
  public EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      return encryptLocal(value);
    }
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(KmsDelegateService.class, syncTaskContext).encrypt(value, kmsConfig);
    } catch (Exception e) {
      throw new WingsException("Encryption with security failed", e);
    }
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      return decryptLocal(data);
    }
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(KmsDelegateService.class, syncTaskContext).decrypt(data, kmsConfig);
    } catch (Exception e) {
      throw new WingsException("Decryption with security failed", e);
    }
  }

  @Override
  public KmsConfig getKmsConfig(String accountId) {
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
  public boolean saveKmsConfig(String accountId, KmsConfig kmsConfig) {
    kmsConfig.setAccountId(accountId);

    EncryptedData accessKeyData = encrypt(kmsConfig.getAccessKey().toCharArray(), accountId, null);
    accessKeyData.setAccountId(accountId);
    accessKeyData.setType(SettingVariableTypes.KMS);
    String accessKeyId = wingsPersistence.save(accessKeyData);
    kmsConfig.setAccessKey(accessKeyId);

    EncryptedData secretKeyData = encrypt(kmsConfig.getSecretKey().toCharArray(), accountId, null);
    secretKeyData.setAccountId(accountId);
    secretKeyData.setType(SettingVariableTypes.KMS);
    String secretKeyId = wingsPersistence.save(secretKeyData);
    kmsConfig.setSecretKey(secretKeyId);

    EncryptedData arnKeyData = encrypt(kmsConfig.getKmsArn().toCharArray(), accountId, null);
    arnKeyData.setAccountId(accountId);
    arnKeyData.setType(SettingVariableTypes.KMS);
    String arnKeyId = wingsPersistence.save(arnKeyData);
    kmsConfig.setKmsArn(arnKeyId);

    String parentId = wingsPersistence.save(kmsConfig);

    accessKeyData.setParentId(parentId);
    wingsPersistence.save(accessKeyData);

    secretKeyData.setParentId(parentId);
    wingsPersistence.save(secretKeyData);

    arnKeyData.setParentId(parentId);
    wingsPersistence.save(arnKeyData);

    return true;
  }

  @Override
  public boolean deleteKmsConfig(String accountId, String kmsConfigId) {
    Iterator<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                        .field("accountId")
                                        .equal(accountId)
                                        .field("kmsId")
                                        .equal(kmsConfigId)
                                        .fetch(new FindOptions().limit(1));

    if (query.hasNext()) {
      throw new WingsException("Can not delete the kms configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to a new kms and then try again");
    }

    wingsPersistence.delete(KmsConfig.class, kmsConfigId);
    Query<EncryptedData> deleteQuery =
        wingsPersistence.createQuery(EncryptedData.class).field("parentId").equal(kmsConfigId);
    return wingsPersistence.delete(deleteQuery);
  }

  @Override
  public Collection<UuidAware> listEncryptedValues(String accountId) {
    Map<String, UuidAware> rv = new HashMap<>();
    Iterator<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("accountId").equal(accountId).fetch();
    while (query.hasNext()) {
      EncryptedData data = query.next();
      rv.put(data.getParentId(), fetchParent(data));
    }
    return rv.values();
  }

  @Override
  public boolean transitionKms(String accountId, String fromKmsId, String toKmsId) {
    Iterator<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                        .field("accountId")
                                        .equal(accountId)
                                        .field("kmsId")
                                        .equal(fromKmsId)
                                        .fetch();
    while (query.hasNext()) {
      EncryptedData dataToTransition = query.next();
      transitionKmsQueue.send(KmsTransitionEvent.builder()
                                  .accountId(accountId)
                                  .entityId(dataToTransition.getUuid())
                                  .fromKmsId(fromKmsId)
                                  .toKmsId(toKmsId)
                                  .build());
    }
    return true;
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

  private KmsConfig getKmsConfig(String accountId, String entityId) {
    KmsConfig kmsConfig = null;
    Iterator<KmsConfig> query = wingsPersistence.createQuery(KmsConfig.class)
                                    .field("accountId")
                                    .equal(accountId)
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
        .updates(new HashMap<>())
        .build();
  }

  private char[] decryptLocal(EncryptedData data) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(data.getEncryptionKey());
    return simpleEncryption.decryptChars(data.getEncryptedValue());
  }

  private UuidAware fetchParent(EncryptedData data) {
    switch (data.getType()) {
      case KMS:
        return getKmsConfig(data.getAccountId());

      case SERVICE_VARIABLE:
        return wingsPersistence.get(ServiceVariable.class, data.getParentId());

      default:
        return wingsPersistence.get(SettingAttribute.class, data.getParentId());
    }
  }
}
