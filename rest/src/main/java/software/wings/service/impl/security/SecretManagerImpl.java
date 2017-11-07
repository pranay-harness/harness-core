package software.wings.service.impl.security;

import static software.wings.service.impl.security.KmsServiceImpl.SECRET_MASK;
import static software.wings.utils.WingsReflectionUtils.getEncryptedFields;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.UuidAware;
import software.wings.beans.VaultConfig;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/30/17.
 */
public class SecretManagerImpl implements SecretManager {
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;

  @Override
  public EncryptionType getEncryptionType(String accountId) {
    if (!featureFlagService.isEnabled(FeatureName.KMS, accountId)) {
      return EncryptionType.LOCAL;
    }

    if (vaultService.getSecretConfig(accountId) != null) {
      return EncryptionType.VAULT;
    }

    if (kmsService.getSecretConfig(accountId) != null) {
      return EncryptionType.KMS;
    }

    return EncryptionType.LOCAL;
  }

  @Override
  public List<EncryptionConfig> listEncryptionConfig(String accountId) {
    List<EncryptionConfig> rv = new ArrayList<>();
    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId);
    Collection<VaultConfig> vaultConfigs = vaultService.listVaultConfigs(accountId);

    for (KmsConfig kmsConfig : kmsConfigs) {
      rv.add(kmsConfig);
    }

    for (VaultConfig vaultConfig : vaultConfigs) {
      rv.add(vaultConfig);
    }
    return rv;
  }

  @Override
  public EncryptedData encrypt(EncryptionType encryptionType, String accountId, SettingVariableTypes settingType,
      char[] secret, String decryptedFieldName, EncryptedData encryptedData) {
    switch (encryptionType) {
      case KMS:
        final KmsConfig kmsConfig = kmsService.getSecretConfig(accountId);
        return kmsService.encrypt(secret, accountId, kmsConfig);

      case VAULT:
        final VaultConfig vaultConfig = vaultService.getSecretConfig(accountId);
        String toEncrypt = secret == null ? null : String.valueOf(secret);
        return vaultService.encrypt(decryptedFieldName, toEncrypt, accountId, settingType, vaultConfig, encryptedData);

      default:
        throw new IllegalStateException("Invalid type:  " + encryptionType);
    }
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(Encryptable object, String workflowId, String appId) {
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }

    List<Field> encryptedFields = object.getEncryptedFields();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        EncryptionType encryptionType = getEncryptionType(object.getAccountId());
        if (encryptionType != EncryptionType.LOCAL && f.get(object) == null) {
          Field encryptedRefField = getEncryptedRefField(f, object);
          encryptedRefField.setAccessible(true);
          EncryptedData encryptedData =
              wingsPersistence.get(EncryptedData.class, (String) encryptedRefField.get(object));
          Preconditions.checkNotNull(encryptedData,
              "field " + f.getName() + " has no reference for " + f.get(object) + "  for encryptable " + object);

          EncryptionConfig encryptionConfig =
              getEncryptionConfig(object.getAccountId(), encryptedData.getKmsId(), encryptionType);

          EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder()
                                                        .encryptionType(encryptionType)
                                                        .encryptedData(encryptedData)
                                                        .encryptionConfig(encryptionConfig)
                                                        .fieldName(f.getName())
                                                        .build();

          encryptedDataDetails.add(encryptedDataDetail);
          if (!StringUtils.isBlank(workflowId)) {
            SecretUsageLog usageLog = SecretUsageLog.builder()
                                          .encryptedDataId(encryptedData.getUuid())
                                          .workflowId(workflowId)
                                          .accountId(encryptedData.getAccountId())
                                          .build();
            usageLog.setAppId(appId);
            wingsPersistence.save(usageLog);
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
  public List<SecretUsageLog> getUsageLogs(String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    final List<SecretUsageLog> secretUsageLogs = new ArrayList<>();
    final List<String> secretIds = getSecretIds(entityId, variableType);

    Iterator<SecretUsageLog> usageLogQuery =
        wingsPersistence.createQuery(SecretUsageLog.class).field("encryptedDataId").hasAnyOf(secretIds).fetch();
    while (usageLogQuery.hasNext()) {
      SecretUsageLog usageLog = usageLogQuery.next();
      Workflow workflow = wingsPersistence.get(Workflow.class, usageLog.getWorkflowId());
      usageLog.setWorkflowName(workflow.getName());

      secretUsageLogs.add(usageLog);
    }
    return secretUsageLogs;
  }

  @Override
  public List<SecretChangeLog> getChangeLogs(String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);
    List<SecretChangeLog> rv = new ArrayList<>();
    Iterator<SecretChangeLog> secretChangeLogsQuery = wingsPersistence.createQuery(SecretChangeLog.class)
                                                          .field("encryptedDataId")
                                                          .hasAnyOf(secretIds)
                                                          .order("-createdAt")
                                                          .fetch();
    while (secretChangeLogsQuery.hasNext()) {
      rv.add(secretChangeLogsQuery.next());
    }

    return rv;
  }

  @Override
  public Collection<UuidAware> listEncryptedValues(String accountId) {
    Map<String, UuidAware> rv = new HashMap<>();
    Iterator<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).field("accountId").equal(accountId).fetch();
    while (query.hasNext()) {
      EncryptedData data = query.next();
      if (data.getType() != SettingVariableTypes.KMS) {
        UuidAware parent = fetchParent(data.getType(), accountId, data.getParentId(), true, data.getEncryptionType());
        if (parent == null) {
          logger.error("No parent found for {}", data);
          continue;
        }
        rv.put(data.getParentId(), parent);
      }
    }
    return rv.values();
  }

  @Override
  public EncryptedData encryptFile(BoundedInputStream inputStream, String accountId, String uuid) {
    EncryptionType encryptionType = getEncryptionType(accountId);
    switch (encryptionType) {
      case KMS:
        return kmsService.encryptFile(inputStream, accountId, uuid);

      case VAULT:
        return vaultService.encryptFile(inputStream, accountId, uuid);

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    EncryptionType encryptionType = getEncryptionType(accountId);
    switch (encryptionType) {
      case KMS:
        return kmsService.decryptFile(file, accountId, encryptedData);

      case VAULT:
        return vaultService.decryptFile(file, accountId, encryptedData);

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }
  }

  @Override
  public String getEncryptedYamlRef(Encryptable object, String uuid, SettingVariableTypes type, String... fieldNames)
      throws IllegalAccessException {
    Preconditions.checkState(fieldNames.length <= 1, "can't give more than one field in the call");
    Field encryptedField = null;
    if (fieldNames.length == 0) {
      encryptedField = object.getEncryptedFields().get(0);
    } else {
      String fieldName = fieldNames[0];
      List<Field> encryptedFields = object.getEncryptedFields();
      for (Field f : encryptedFields) {
        if (f.getName().equals(fieldName)) {
          encryptedField = f;
          break;
        }
      }
    }
    Preconditions.checkNotNull(encryptedField, "encrypted field not found " + object + ", args:" + fieldNames);

    encryptedField.setAccessible(true);

    // locally encrypted
    if (encryptedField.get(object) != null) {
      Preconditions.checkState(!StringUtils.isBlank(uuid), "uuid can't be empty");
      return EncryptionType.LOCAL + ":" + object.getAccountId() + ":" + type + ":" + uuid;
    }

    Field encryptedFieldRef = getEncryptedRefField(encryptedField, object);
    encryptedFieldRef.setAccessible(true);
    String encryptedFieldRefId = (String) encryptedFieldRef.get(object);
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
    Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + object);

    return encryptedData.getEncryptionType() + ":" + object.getAccountId() + ":" + type + ":" + encryptedFieldRefId;
  }

  @Override
  public char[] decryptYamlRef(String encryptedYamlRef) throws NoSuchFieldException, IllegalAccessException {
    Preconditions.checkState(!StringUtils.isBlank(encryptedYamlRef));
    logger.info("Decrypting: {}", encryptedYamlRef);
    String[] tags = encryptedYamlRef.split(":");
    EncryptionType encryptionType = EncryptionType.valueOf(tags[0]);
    String accountId = tags[1];
    SettingVariableTypes type = SettingVariableTypes.valueOf(tags[2]);
    String fieldRefId = tags[3];

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, fieldRefId);
    switch (encryptionType) {
      case LOCAL:
        UuidAware uuidAware = fetchParent(type, accountId, fieldRefId, false, encryptionType);
        Preconditions.checkState(
            SettingAttribute.class.isInstance(uuidAware) || Encryptable.class.isInstance(uuidAware),
            "Only setting attributes and encryptable classes can be decrypted");
        Object toDecrypt = uuidAware;
        if (SettingAttribute.class.isInstance(uuidAware)) {
          toDecrypt = ((SettingAttribute) uuidAware).getValue();
        }

        if (Encryptable.class.isInstance(toDecrypt)) {
          Encryptable encrypted = (Encryptable) toDecrypt;
          Field encryptedField = encrypted.getEncryptedFields().get(0);
          encryptedField.setAccessible(true);
          return kmsService.decrypt(EncryptedData.builder()
                                        .encryptionKey(accountId)
                                        .encryptedValue((char[]) encryptedField.get(encrypted))
                                        .build(),
              accountId, null);
        }

        throw new IllegalArgumentException("Can't find encryptable field for " + encryptedYamlRef);
      case KMS:
        return kmsService.decrypt(
            encryptedData, accountId, kmsService.getKmsConfig(accountId, encryptedData.getKmsId()));

      case VAULT:
        return vaultService.decrypt(
            encryptedData, accountId, vaultService.getVaultConfig(accountId, encryptedData.getKmsId()));

      default:
        throw new IllegalStateException("Invalid encryptionType: " + encryptionType);
    }
  }

  @Override
  public boolean transitionSecrets(
      String accountId, String fromVaultId, String toVaultId, EncryptionType encryptionType) {
    switch (encryptionType) {
      case KMS:
        return kmsService.transitionKms(accountId, fromVaultId, toVaultId);

      case VAULT:
        return vaultService.transitionVault(accountId, fromVaultId, toVaultId);

      default:
        throw new IllegalArgumentException("Invalid type: " + encryptionType);
    }
  }

  private EncryptionConfig getEncryptionConfig(String accountId, String entityId, EncryptionType encryptionType) {
    switch (encryptionType) {
      case KMS:
        return kmsService.getKmsConfig(accountId, entityId);
      case VAULT:
        return vaultService.getVaultConfig(accountId, entityId);
      default:
        throw new IllegalStateException("invalid type: " + encryptionType);
    }
  }

  private List<String> getSecretIds(String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = new ArrayList<>();
    switch (variableType) {
      case SERVICE_VARIABLE:
        Iterator<ServiceVariable> serviceVaribaleQuery = wingsPersistence.createQuery(ServiceVariable.class)
                                                             .field("_id")
                                                             .equal(entityId)
                                                             .fetch(new FindOptions().limit(1));
        if (serviceVaribaleQuery.hasNext()) {
          ServiceVariable serviceVariable = serviceVaribaleQuery.next();
          List<Field> encryptedFields = getEncryptedFields(serviceVariable.getClass());

          for (Field field : encryptedFields) {
            Field encryptedRefField = getEncryptedRefField(field, serviceVariable);
            encryptedRefField.setAccessible(true);
            secretIds.add((String) encryptedRefField.get(serviceVariable));
          }
        }
        break;

      case CONFIG_FILE:
        secretIds.add(entityId);
        break;

      default:
        Iterator<SettingAttribute> settingAttributeQuery = wingsPersistence.createQuery(SettingAttribute.class)
                                                               .field("_id")
                                                               .equal(entityId)
                                                               .fetch(new FindOptions().limit(1));
        if (settingAttributeQuery.hasNext()) {
          SettingAttribute settingAttribute = settingAttributeQuery.next();

          List<Field> encryptedFields = getEncryptedFields(settingAttribute.getValue().getClass());

          for (Field field : encryptedFields) {
            Field encryptedRefField = getEncryptedRefField(field, (Encryptable) settingAttribute.getValue());
            encryptedRefField.setAccessible(true);
            secretIds.add((String) encryptedRefField.get(settingAttribute.getValue()));
          }
        }
    }
    return secretIds;
  }

  private UuidAware fetchParent(
      SettingVariableTypes type, String accountId, String parentId, boolean maskSecret, EncryptionType encryptionType) {
    switch (type) {
      case KMS:
        return kmsService.getSecretConfig(accountId);

      case SERVICE_VARIABLE:
        Iterator<ServiceVariable> serviceVaribaleQuery = wingsPersistence.createQuery(ServiceVariable.class)
                                                             .field("_id")
                                                             .equal(parentId)
                                                             .fetch(new FindOptions().limit(1));
        if (serviceVaribaleQuery.hasNext()) {
          ServiceVariable serviceVariable = serviceVaribaleQuery.next();
          if (maskSecret) {
            serviceVariable.setValue(SECRET_MASK.toCharArray());
          }
          if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
            ServiceTemplate serviceTemplate =
                wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
            Preconditions.checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
            serviceVariable.setServiceId(serviceTemplate.getServiceId());
          }
          serviceVariable.setEncryptionType(encryptionType);
          return serviceVariable;
        }
        return null;

      case CONFIG_FILE:
        Iterator<ConfigFile> configFileQuery = wingsPersistence.createQuery(ConfigFile.class)
                                                   .field("_id")
                                                   .equal(parentId)
                                                   .fetch(new FindOptions().limit(1));
        if (configFileQuery.hasNext()) {
          ConfigFile configFile = configFileQuery.next();
          if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
            ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, configFile.getEntityId());
            Preconditions.checkNotNull(serviceTemplate, "can't find service template " + configFile);
            configFile.setServiceId(serviceTemplate.getServiceId());
          }
          configFile.setEncryptionType(encryptionType);
          return configFile;
        }
        return null;

      case VAULT:
        Iterator<VaultConfig> vaultConfigIterator = wingsPersistence.createQuery(VaultConfig.class)
                                                        .field("_id")
                                                        .equal(parentId)
                                                        .fetch(new FindOptions().limit(1));
        if (vaultConfigIterator.hasNext()) {
          VaultConfig vaultConfig = vaultConfigIterator.next();
          vaultConfig.setEncryptionType(encryptionType);
          return vaultConfig;
        }
        return null;

      default:
        Iterator<SettingAttribute> settingAttributeQuery = wingsPersistence.createQuery(SettingAttribute.class)
                                                               .field("_id")
                                                               .equal(parentId)
                                                               .fetch(new FindOptions().limit(1));
        if (settingAttributeQuery.hasNext()) {
          SettingAttribute settingAttribute = settingAttributeQuery.next();
          settingAttribute.setEncryptionType(encryptionType);
          return settingAttribute;
        }
        return null;
    }
  }
}
