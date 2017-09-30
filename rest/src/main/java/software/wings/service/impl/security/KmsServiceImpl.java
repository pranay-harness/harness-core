package software.wings.service.impl.security;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import org.mongodb.morphia.query.FindOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
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
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public EncryptedData encrypt(char[] value, KmsConfig kmsConfig) {
    if (kmsConfig == null || !kmsConfig.isInitialized()) {
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
    if (kmsConfig == null || !kmsConfig.isInitialized()) {
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
    Iterator<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                           .field("accountId")
                                           .equal(accountId)
                                           .field("value.type")
                                           .equal(SettingVariableTypes.KMS)
                                           .fetch(new FindOptions().limit(1));
    if (query.hasNext()) {
      return (KmsConfig) query.next().getValue();
    }

    KmsConfig kmsConfig = mainConfiguration.getPortal().getKmsConfig();
    if (kmsConfig != null) {
      kmsConfig.setAccountId(accountId);
      return kmsConfig;
    }

    return null;
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
