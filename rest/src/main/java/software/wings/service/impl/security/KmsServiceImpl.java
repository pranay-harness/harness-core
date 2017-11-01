package software.wings.service.impl.security;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.ErrorCode.DEFAULT_ERROR_CODE;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.UuidAware;
import software.wings.core.queue.Queue;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.KmsDelegateService;
import software.wings.service.intfc.security.KmsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 9/29/17.
 */
public class KmsServiceImpl implements KmsService {
  private static final Logger logger = LoggerFactory.getLogger(KmsServiceImpl.class);
  public static final String SECRET_MASK = "**************";

  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public boolean shouldUseKms(String accountId) {
    return featureFlagService.isEnabled(FeatureName.KMS, accountId) && getKmsConfig(accountId) != null;
  }

  @Override
  public EncryptedData encrypt(char[] value, String accountId, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      return encryptLocal(value);
    }
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    try {
      return delegateProxyFactory.get(KmsDelegateService.class, syncTaskContext).encrypt(value, kmsConfig);
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
      return delegateProxyFactory.get(KmsDelegateService.class, syncTaskContext).decrypt(data, kmsConfig);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", e.getMessage());
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
  public boolean saveGlobalKmsConfig(String accountId, KmsConfig kmsConfig) {
    validateKms(accountId, kmsConfig);
    return saveKmsConfigInternal(Base.GLOBAL_ACCOUNT_ID, kmsConfig);
  }

  @Override
  public boolean saveKmsConfig(String accountId, KmsConfig kmsConfig) {
    try {
      validateKms(accountId, kmsConfig);
    } catch (Exception e) {
      throw new WingsException(
          ErrorCode.KMS_OPERATION_ERROR, "reason", "Validation failed. Please check your credentials");
    }
    return saveKmsConfigInternal(accountId, kmsConfig);
  }

  private boolean saveKmsConfigInternal(String accountId, KmsConfig kmsConfig) {
    kmsConfig.setAccountId(accountId);
    Query<KmsConfig> query = wingsPersistence.createQuery(KmsConfig.class).field("accountId").equal(accountId);
    Collection<KmsConfig> savedConfigs = query.asList();

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

    if (kmsConfig.isDefault() && !savedConfigs.isEmpty()) {
      for (KmsConfig savedConfig : savedConfigs) {
        if (kmsConfig.getUuid().equals(savedConfig.getUuid())) {
          continue;
        }
        savedConfig.setDefault(false);
        wingsPersistence.save(savedConfig);
      }
    }

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
      String message = "Can not delete the kms configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to a new kms and then try again";
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", message);
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
      if (data.getType() != SettingVariableTypes.KMS) {
        UuidAware parent = fetchParent(data);
        if (parent == null) {
          logger.error("No parent found for {}", data);
          continue;
        }
        rv.put(data.getParentId(), fetchParent(data));
      }
    }
    return rv.values();
  }

  @Override
  public Collection<KmsConfig> listKmsConfigs(String accountId) {
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

      kmsConfig.setSecretKey(SECRET_MASK);
      rv.add(kmsConfig);
    }

    if (!defaultSet && globalConfig != null) {
      globalConfig.setDefault(true);
    }
    return rv;
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

  @Override
  public EncryptedData encryptFile(BoundedInputStream inputStream, String accountId) {
    try {
      KmsConfig kmsConfig = getKmsConfig(accountId);
      Preconditions.checkNotNull(kmsConfig);
      byte[] bytes = ByteStreams.toByteArray(inputStream);
      EncryptedData fileData = encrypt(new String(bytes).toCharArray(), accountId, kmsConfig);
      fileData.setAccountId(accountId);
      fileData.setType(SettingVariableTypes.CONFIG_FILE);
      char[] encryptedValue = fileData.getEncryptedValue();
      fileData.setEncryptedValue(null);
      wingsPersistence.save(fileData);

      fileData.setEncryptedValue(encryptedValue);
      return fileData;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    try {
      KmsConfig kmsConfig = getKmsConfig(accountId);
      Preconditions.checkNotNull(kmsConfig);
      Preconditions.checkNotNull(encryptedData);
      byte[] bytes = Files.toByteArray(file);
      encryptedData.setEncryptedValue(new String(bytes).toCharArray());
      char[] decrypt = decrypt(encryptedData, accountId, kmsConfig);
      Files.write(new String(decrypt).getBytes(), file);
      return file;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(Encryptable object, String workflowId) {
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }

    List<Field> encryptedFields = object.getEncryptedFields();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        if (shouldUseKms(object.getAccountId()) && f.get(object) == null) {
          Field encryptedRefField = getEncryptedRefField(f, object);
          encryptedRefField.setAccessible(true);
          EncryptedData encryptedData =
              wingsPersistence.get(EncryptedData.class, (String) encryptedRefField.get(object));
          Preconditions.checkNotNull(encryptedData,
              "field " + f.getName() + " has no reference for " + f.get(object) + "  for encryptable " + object);

          KmsConfig kmsConfig = getKmsConfig(object.getAccountId(), encryptedData.getKmsId());
          encryptedDataDetails.add(EncryptedDataDetail.builder()
                                       .encryptionType(EncryptionType.KMS)
                                       .encryptedData(encryptedData)
                                       .kmsConfig(kmsConfig)
                                       .fieldName(f.getName())
                                       .build());
          if (!StringUtils.isBlank(workflowId)) {
            wingsPersistence.save(SecretUsageLog.builder()
                                      .encryptedDataId(encryptedData.getUuid())
                                      .workflowId(workflowId)
                                      .accountId(encryptedData.getAccountId())
                                      .build());
          }
        } else if (f.get(object) != null) {
          encryptedDataDetails.add(
              EncryptedDataDetail.builder()
                  .encryptionType(EncryptionType.LOCAL)
                  .encryptedData(EncryptedData.builder().encryptedValue((char[]) f.get(object)).build())
                  .fieldName(f.getName())
                  .build());
        }
      }
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }

    return encryptedDataDetails;
  }

  @Override
  public String getEncryptedYamlRef(Encryptable object, String appId, String fieldName, String entityName)
      throws IllegalAccessException {
    List<Field> encryptedFields = object.getEncryptedFields();
    Field encryptedField = null;
    for (Field f : encryptedFields) {
      if (f.getName().equals(fieldName)) {
        encryptedField = f;
        break;
      }
    }
    Preconditions.checkNotNull(encryptedField, fieldName + " is not found in encrypted fields " + encryptedFields);

    encryptedField.setAccessible(true);

    // locally encrypted
    if (encryptedField.get(object) != null) {
      return EncryptionType.LOCAL + ":" + object.getAccountId() + ":" + appId + ":" + object.getSettingType() + ":"
          + entityName + ":" + fieldName;
    }

    if (shouldUseKms(object.getAccountId())) {
      Field encryptedFieldRef = getEncryptedRefField(encryptedField, object);
      Preconditions.checkNotNull(encryptedField, "ref null for " + object);
      encryptedFieldRef.setAccessible(true);
      return EncryptionType.KMS + ":" + object.getAccountId() + ":" + appId + ":" + object.getSettingType() + ":"
          + entityName + ":" + encryptedFieldRef.getName();
    }

    throw new IllegalStateException("Illegal encryption state for " + object);
  }

  @Override
  public char[] decryptYamlRef(String encryptedYamlRef) throws NoSuchFieldException, IllegalAccessException {
    Preconditions.checkState(!StringUtils.isBlank(encryptedYamlRef));
    logger.info("Decrypting: {}", encryptedYamlRef);
    String[] tags = encryptedYamlRef.split(":");
    EncryptionType encryptionType = EncryptionType.valueOf(tags[0]);
    String accountId = tags[1];
    String appId = tags[2];
    SettingVariableTypes variableType = SettingVariableTypes.valueOf(tags[3]);
    String entityName = tags[4];
    String fieldName = tags[5];

    Encryptable entity = getEntityByName(accountId, appId, variableType, entityName);
    Field field = entity.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    switch (encryptionType) {
      case LOCAL:
        return decrypt(
            EncryptedData.builder().encryptionKey(accountId).encryptedValue((char[]) field.get(entity)).build(), null,
            null);

      case KMS:
        EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, (String) field.get(entity));
        Preconditions.checkNotNull("No encrypted ref found for " + encryptedYamlRef);
        Preconditions.checkState(!StringUtils.isBlank(encryptedData.getKmsId()), "KmsId null for " + encryptedYamlRef);
        KmsConfig kmsConfig = getKmsConfig(accountId, encryptedData.getKmsId());
        return decrypt(encryptedData, accountId, kmsConfig);

      default:
        throw new IllegalStateException("Invalid encryptionType: " + encryptionType);
    }
  }

  private void validateKms(String accountId, KmsConfig kmsConfig) {
    encrypt(UUID.randomUUID().toString().toCharArray(), accountId, kmsConfig);
  }

  private KmsConfig getKmsConfig(String accountId, String entityId) {
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
        Iterator<ServiceVariable> serviceVaribaleQuery = wingsPersistence.createQuery(ServiceVariable.class)
                                                             .field("_id")
                                                             .equal(data.getParentId())
                                                             .fetch(new FindOptions().limit(1));
        if (serviceVaribaleQuery.hasNext()) {
          ServiceVariable serviceVariable = serviceVaribaleQuery.next();
          serviceVariable.setValue(SECRET_MASK.toCharArray());
          if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
            ServiceTemplate serviceTemplate =
                wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
            Preconditions.checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
            serviceVariable.setServiceId(serviceTemplate.getServiceId());
          }
          return serviceVariable;
        }
        return null;

      case CONFIG_FILE:
        Iterator<ConfigFile> configFileQuery = wingsPersistence.createQuery(ConfigFile.class)
                                                   .field("_id")
                                                   .equal(data.getParentId())
                                                   .fetch(new FindOptions().limit(1));
        if (configFileQuery.hasNext()) {
          ConfigFile configFile = configFileQuery.next();
          if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
            ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, configFile.getEntityId());
            Preconditions.checkNotNull(serviceTemplate, "can't find service template " + configFile);
            configFile.setServiceId(serviceTemplate.getServiceId());
          }
          return configFile;
        }
        return null;

      default:
        Iterator<SettingAttribute> settingAttributeQuery = wingsPersistence.createQuery(SettingAttribute.class)
                                                               .field("_id")
                                                               .equal(data.getParentId())
                                                               .fetch(new FindOptions().limit(1));
        if (settingAttributeQuery.hasNext()) {
          return settingAttributeQuery.next();
        }
        return null;
    }
  }

  private Encryptable getEntityByName(
      String accountId, String appId, SettingVariableTypes variableType, String entityName) {
    Encryptable rv = null;
    switch (variableType) {
      case SERVICE_VARIABLE:
        Iterator<ServiceVariable> serviceVaribaleQuery = wingsPersistence.createQuery(ServiceVariable.class)
                                                             .field("accountId")
                                                             .equal(accountId)
                                                             .field("appId")
                                                             .equal(appId)
                                                             .field("name")
                                                             .equal(entityName)
                                                             .fetch(new FindOptions().limit(1));
        if (serviceVaribaleQuery.hasNext()) {
          ServiceVariable serviceVariable = serviceVaribaleQuery.next();
          rv = serviceVariable;
        }
        break;

      case CONFIG_FILE:
        Iterator<ConfigFile> configFileQuery = wingsPersistence.createQuery(ConfigFile.class)
                                                   .field("accountId")
                                                   .equal(accountId)
                                                   .field("appId")
                                                   .equal(appId)
                                                   .field("name")
                                                   .equal(entityName)
                                                   .fetch(new FindOptions().limit(1));
        if (configFileQuery.hasNext()) {
          rv = configFileQuery.next();
        }
        break;

      default:
        Iterator<SettingAttribute> settingAttributeQuery = wingsPersistence.createQuery(SettingAttribute.class)
                                                               .field("accountId")
                                                               .equal(accountId)
                                                               .field("name")
                                                               .equal(entityName)
                                                               .field("value.type")
                                                               .equal(variableType)
                                                               .fetch(new FindOptions().limit(1));
        if (settingAttributeQuery.hasNext()) {
          rv = (Encryptable) settingAttributeQuery.next().getValue();
        }
        break;
    }

    Preconditions.checkNotNull(
        rv, "Could not find entity accountId: " + accountId + " type: " + variableType + " name: " + entityName);
    return rv;
  }
}
