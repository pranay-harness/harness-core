package software.wings.service.impl.security;

import static software.wings.security.encryption.SimpleEncryption.CHARSET;
import static software.wings.service.impl.security.KmsServiceImpl.SECRET_MASK;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.utils.WingsReflectionUtils.getEncryptedFields;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;

import com.google.common.base.Preconditions;

import com.mongodb.DuplicateKeyException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.BaseFile;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.UuidAware;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/30/17.
 */
public class SecretManagerImpl implements SecretManager {
  public static final String HARNESS_DEFAULT_SECRET_MANAGER = "Harness Manager";
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;
  @Inject private AccountService accountService;
  @Inject private AlertService alertService;
  @Inject private FileService fileService;

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
    Collection<VaultConfig> vaultConfigs = vaultService.listVaultConfigs(accountId, true);
    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, true);

    boolean defaultVaultSet = false;
    for (VaultConfig vaultConfig : vaultConfigs) {
      if (vaultConfig.isDefault()) {
        defaultVaultSet = true;
      }
      rv.add(vaultConfig);
    }

    for (KmsConfig kmsConfig : kmsConfigs) {
      if (defaultVaultSet && kmsConfig.isDefault()) {
        Preconditions.checkState(
            kmsConfig.getAccountId().equals(Base.GLOBAL_ACCOUNT_ID), "found both kms and vault configs to be default");
        kmsConfig.setDefault(false);
      }
      rv.add(kmsConfig);
    }

    return rv;
  }

  @Override
  public EncryptedData encrypt(EncryptionType encryptionType, String accountId, SettingVariableTypes settingType,
      char[] secret, EncryptedData encryptedData, String secretName) {
    EncryptedData rv;
    switch (encryptionType) {
      case LOCAL:
        char[] encryptedChars = secret == null ? null : new SimpleEncryption(accountId).encryptChars(secret);
        rv = EncryptedData.builder()
                 .encryptionKey(accountId)
                 .encryptedValue(encryptedChars)
                 .encryptionType(EncryptionType.LOCAL)
                 .accountId(accountId)
                 .type(settingType)
                 .enabled(true)
                 .parentIds(new HashSet<>())
                 .build();
        break;

      case KMS:
        final KmsConfig kmsConfig = kmsService.getSecretConfig(accountId);
        rv = kmsService.encrypt(secret, accountId, kmsConfig);
        rv.setType(settingType);
        break;

      case VAULT:
        final VaultConfig vaultConfig = vaultService.getSecretConfig(accountId);
        String toEncrypt = secret == null ? null : String.valueOf(secret);
        rv = vaultService.encrypt(secretName, toEncrypt, accountId, settingType, vaultConfig, encryptedData);
        rv.setType(settingType);
        break;

      default:
        throw new IllegalStateException("Invalid type:  " + encryptionType);
    }
    rv.setName(secretName);
    return rv;
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(Encryptable object, String appId, String workflowExecutionId) {
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }

    List<Field> encryptedFields = object.getEncryptedFields();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        Field encryptedRefField = getEncryptedRefField(f, object);
        encryptedRefField.setAccessible(true);
        if (f.get(object) != null) {
          Preconditions.checkState(
              encryptedRefField.get(object) == null, "both encrypted and non encrypted field set for " + object);
          encryptedDataDetails.add(EncryptedDataDetail.builder()
                                       .encryptionType(EncryptionType.LOCAL)
                                       .encryptedData(EncryptedData.builder()
                                                          .encryptionKey(object.getAccountId())
                                                          .encryptedValue((char[]) f.get(object))
                                                          .build())
                                       .fieldName(f.getName())
                                       .build());
        } else {
          EncryptedData encryptedData =
              wingsPersistence.get(EncryptedData.class, (String) encryptedRefField.get(object));
          if (encryptedData == null) {
            logger.info("No encrypted record set for field {} for object {}", f.getName(), object);
            continue;
          }
          EncryptionConfig encryptionConfig =
              getEncryptionConfig(object.getAccountId(), encryptedData.getKmsId(), encryptedData.getEncryptionType());

          EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder()
                                                        .encryptionType(encryptedData.getEncryptionType())
                                                        .encryptedData(encryptedData)
                                                        .encryptionConfig(encryptionConfig)
                                                        .fieldName(f.getName())
                                                        .build();

          encryptedDataDetails.add(encryptedDataDetail);

          if (!StringUtils.isBlank(workflowExecutionId)) {
            WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
            if (workflowExecution == null) {
              logger.warn("No workflow execution with id {} found.", workflowExecutionId);
            } else {
              SecretUsageLog usageLog = SecretUsageLog.builder()
                                            .encryptedDataId(encryptedData.getUuid())
                                            .workflowExecutionId(workflowExecutionId)
                                            .accountId(encryptedData.getAccountId())
                                            .envId(workflowExecution.getEnvId())
                                            .build();
              usageLog.setAppId(appId);
              wingsPersistence.save(usageLog);
            }
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }

    return encryptedDataDetails;
  }

  @Override
  public PageResponse<SecretUsageLog> getUsageLogs(String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);

    final PageRequest<SecretUsageLog> request =
        PageRequest.Builder.aPageRequest().addFilter("encryptedDataId", Operator.IN, secretIds.toArray()).build();
    PageResponse<SecretUsageLog> response = wingsPersistence.query(SecretUsageLog.class, request);
    response.getResponse().forEach(secretUsageLog -> {
      if (!StringUtils.isBlank(secretUsageLog.getWorkflowExecutionId())) {
        WorkflowExecution workflowExecution =
            wingsPersistence.get(WorkflowExecution.class, secretUsageLog.getWorkflowExecutionId());
        secretUsageLog.setWorkflowExecutionName(workflowExecution.getName());
      }
    });
    return response;
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
    Iterator<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                        .field("accountId")
                                        .equal(accountId)
                                        .field("type")
                                        .notEqual(SettingVariableTypes.SECRET_TEXT)
                                        .fetch();
    while (query.hasNext()) {
      EncryptedData data = query.next();
      if (data.getType() != SettingVariableTypes.KMS) {
        for (String parentId : data.getParentIds()) {
          UuidAware parent =
              fetchParent(data.getType(), accountId, parentId, data.getKmsId(), data.getEncryptionType());
          if (parent == null) {
            logger.warn("No parent found for {}", data);
            continue;
          }
          rv.put(parentId, parent);
        }
      }
    }
    return rv.values();
  }

  @Override
  public String getEncryptedYamlRef(Encryptable object, String... fieldNames) throws IllegalAccessException {
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
      throw new IllegalAccessException("trying to get a yaml reference which wasn't encrypted using secret management");
    }

    Field encryptedFieldRef = getEncryptedRefField(encryptedField, object);
    encryptedFieldRef.setAccessible(true);
    String encryptedFieldRefId = (String) encryptedFieldRef.get(object);
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
    Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + object);

    return encryptedData.getEncryptionType().getYamlName() + ":" + encryptedFieldRefId;
  }

  @Override
  public EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef) throws IllegalAccessException {
    Preconditions.checkState(!StringUtils.isBlank(encryptedYamlRef));
    logger.info("Decrypting: {}", encryptedYamlRef);
    String[] tags = encryptedYamlRef.split(":");
    String fieldRefId = tags[1];
    return wingsPersistence.get(EncryptedData.class, fieldRefId);
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

  @Override
  public void checkAndAlertForInvalidManagers() {
    PageRequest<Account> pageRequest = Builder.aPageRequest().build();
    List<Account> accounts = accountService.list(pageRequest);
    for (Account account : accounts) {
      vaildateKmsConfigs(account.getUuid());
      validateVaultConfigs(account.getUuid());
    }
  }

  @Override
  public String saveSecret(String accountId, String name, String value) {
    EncryptedData encryptedData = encrypt(
        getEncryptionType(accountId), accountId, SettingVariableTypes.SECRET_TEXT, value.toCharArray(), null, name);
    String encryptedDataId;
    try {
      encryptedDataId = wingsPersistence.save(encryptedData);
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", "Variable " + name + " already exists");
    }

    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(encryptedDataId)
                                .description("Created")
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }

    return encryptedDataId;
  }

  @Override
  public boolean updateSecret(String accountId, String uuId, String name, String value) {
    EncryptedData savedData = wingsPersistence.get(EncryptedData.class, uuId);
    if (savedData == null) {
      return false;
    }

    String description = savedData.getName().equals(name) ? "Changed value" : "Changed name & value";
    EncryptedData encryptedData = encrypt(getEncryptionType(accountId), accountId, SettingVariableTypes.SECRET_TEXT,
        value.toCharArray(), savedData, name);
    savedData.setEncryptionKey(encryptedData.getEncryptionKey());
    savedData.setEncryptedValue(encryptedData.getEncryptedValue());
    savedData.setName(name);
    try {
      wingsPersistence.save(savedData);
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", "Variable " + name + " already exists");
    }

    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(uuId)
                                .description(description)
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }
    return true;
  }

  @Override
  public boolean deleteSecret(String accountId, String uuId) {
    List<ServiceVariable> serviceVariables = wingsPersistence.createQuery(ServiceVariable.class)
                                                 .field("accountId")
                                                 .equal(accountId)
                                                 .field("encryptedValue")
                                                 .equal(uuId)
                                                 .asList();
    if (!serviceVariables.isEmpty()) {
      String errorMessage = "Being used by ";
      for (ServiceVariable serviceVariable : serviceVariables) {
        errorMessage += serviceVariable.getName() + ", ";
      }

      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", errorMessage);
    }

    return wingsPersistence.delete(EncryptedData.class, uuId);
  }

  @Override
  public String saveFile(String accountId, String name, BoundedInputStream inputStream) {
    EncryptionType encryptionType = getEncryptionType(accountId);
    String recordId;
    switch (encryptionType) {
      case LOCAL:
        char[] encryptedFileData = EncryptionUtils.encrypt(inputStream, accountId);
        BaseFile baseFile = new BaseFile();
        baseFile.setFileName(name);
        String fileId = fileService.saveFile(
            baseFile, new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(encryptedFileData)).array()), CONFIGS);
        EncryptedData encryptedData = EncryptedData.builder()
                                          .accountId(accountId)
                                          .name(name)
                                          .encryptionKey(accountId)
                                          .encryptedValue(fileId.toCharArray())
                                          .encryptionType(EncryptionType.LOCAL)
                                          .kmsId(null)
                                          .type(SettingVariableTypes.CONFIG_FILE)
                                          .enabled(true)
                                          .build();
        recordId = wingsPersistence.save(encryptedData);
        break;

      case KMS:
        recordId = wingsPersistence.save(kmsService.encryptFile(accountId, name, inputStream));
        break;

      case VAULT:
        recordId = wingsPersistence.save(vaultService.encryptFile(accountId, name, inputStream, null));
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }

    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(recordId)
                                .description("File uploaded")
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }
    return recordId;
  }

  @Override
  public File getFile(String accountId, String uuid, File readInto) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    Preconditions.checkNotNull(encryptedData, "could not find file with id " + uuid);
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    switch (encryptionType) {
      case LOCAL:
        fileService.download(String.valueOf(encryptedData.getEncryptedValue()), readInto, CONFIGS);
        return EncryptionUtils.decrypt(readInto, encryptedData.getEncryptionKey());

      case KMS:
        fileService.download(String.valueOf(encryptedData.getEncryptedValue()), readInto, CONFIGS);
        return kmsService.decryptFile(readInto, accountId, encryptedData);

      case VAULT:
        return vaultService.decryptFile(readInto, accountId, encryptedData);

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }
  }

  @Override
  public boolean updateFile(String accountId, String name, String uuid, BoundedInputStream inputStream) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    String oldName = encryptedData.getName();
    Preconditions.checkNotNull(encryptedData, "could not find file with id " + uuid);

    String savedFileId = String.valueOf(encryptedData.getEncryptedValue());
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    EncryptedData encryptedFileData = null;
    switch (encryptionType) {
      case LOCAL:
        char[] encryptedFileDataVal = EncryptionUtils.encrypt(inputStream, accountId);
        BaseFile baseFile = new BaseFile();
        baseFile.setFileName(name);
        String fileId = fileService.saveFile(
            baseFile, new ByteArrayInputStream(CHARSET.encode(CharBuffer.wrap(encryptedFileDataVal)).array()), CONFIGS);
        encryptedFileData =
            EncryptedData.builder().encryptionKey(accountId).encryptedValue(fileId.toCharArray()).build();
        fileService.deleteFile(savedFileId, CONFIGS);
        break;

      case KMS:
        encryptedFileData = kmsService.encryptFile(accountId, name, inputStream);
        fileService.deleteFile(savedFileId, CONFIGS);
        break;

      case VAULT:
        encryptedFileData = vaultService.encryptFile(accountId, name, inputStream, encryptedData);
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }

    encryptedData.setEncryptionKey(encryptedFileData.getEncryptionKey());
    encryptedData.setEncryptedValue(encryptedFileData.getEncryptedValue());
    wingsPersistence.save(encryptedData);

    if (UserThreadLocal.get() != null) {
      String description = oldName.equals(name) ? "Changed File" : "Changed Name and File";
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(uuid)
                                .description(description)
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }

    return true;
  }

  @Override
  public boolean deleteFile(String accountId, String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull("No encrypted record found with id " + uuId);
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .field("accountId")
                                       .equal(accountId)
                                       .field("encryptedFileId")
                                       .equal(uuId)
                                       .asList();
    if (!configFiles.isEmpty()) {
      String errorMessage = "Being used by ";
      for (ConfigFile configFile : configFiles) {
        errorMessage += configFile.getName() + ", ";
      }

      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR, "reason", errorMessage);
    }

    switch (encryptedData.getEncryptionType()) {
      case LOCAL:
      case KMS:
        fileService.deleteFile(String.valueOf(encryptedData.getEncryptedValue()), CONFIGS);
        return true;
      case VAULT:
        vaultService.deleteSecret(accountId, encryptedData.getEncryptionKey(),
            vaultService.getVaultConfig(accountId, encryptedData.getKmsId()));
        return true;
    }
    return wingsPersistence.delete(EncryptedData.class, uuId);
  }

  @Override
  public List<EncryptedData> listSecrets(String accountId, SettingVariableTypes type) {
    List<EncryptedData> rv = new ArrayList<>();
    Iterator<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                        .field("accountId")
                                        .equal(accountId)
                                        .field("type")
                                        .equal(type)
                                        .fetch(new FindOptions());
    while (query.hasNext()) {
      EncryptedData encryptedData = query.next();
      encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
      encryptedData.setEncryptionKey(SECRET_MASK);
      encryptedData.setEncryptedBy(getSecretManagerName(
          type, encryptedData.getUuid(), encryptedData.getKmsId(), encryptedData.getEncryptionType()));
      rv.add(encryptedData);
    }

    return rv;
  }

  @Override
  public List<UuidAware> getSecretUsage(String accountId, String secretTextId) {
    EncryptedData secretText = wingsPersistence.get(EncryptedData.class, secretTextId);
    Preconditions.checkNotNull(secretText, "could not find secret with id " + secretTextId);
    List<UuidAware> rv = new ArrayList<>();
    if (secretText.getParentIds() == null) {
      return rv;
    }

    SettingVariableTypes type = secretText.getType() == SettingVariableTypes.SECRET_TEXT
        ? SettingVariableTypes.SERVICE_VARIABLE
        : secretText.getType();
    for (String parentId : secretText.getParentIds()) {
      rv.add(fetchParent(type, accountId, parentId, secretText.getKmsId(), secretText.getEncryptionType()));
    }

    return rv;
  }

  private EncryptionConfig getEncryptionConfig(String accountId, String entityId, EncryptionType encryptionType) {
    switch (encryptionType) {
      case LOCAL:
        return null;
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
      case SECRET_TEXT:
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
      SettingVariableTypes type, String accountId, String parentId, String kmsId, EncryptionType encryptionType) {
    String encryptedBy = getSecretManagerName(type, parentId, kmsId, encryptionType);
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
          serviceVariable.setValue(SECRET_MASK.toCharArray());
          if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
            ServiceTemplate serviceTemplate =
                wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
            Preconditions.checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
            serviceVariable.setServiceId(serviceTemplate.getServiceId());
          }
          serviceVariable.setEncryptionType(encryptionType);
          serviceVariable.setEncryptedBy(encryptedBy);
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
          configFile.setEncryptedBy(encryptedBy);
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
          vaultConfig.setEncryptedBy(encryptedBy);
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
          settingAttribute.setEncryptedBy(encryptedBy);
          return settingAttribute;
        }
        return null;
    }
  }

  private String getSecretManagerName(
      SettingVariableTypes type, String parentId, String kmsId, EncryptionType encryptionType) {
    switch (encryptionType) {
      case LOCAL:
        Preconditions.checkState(StringUtils.isBlank(kmsId),
            "kms id should be null for local type, "
                + "kmsId: " + kmsId + " for " + type + " id: " + parentId);
        return HARNESS_DEFAULT_SECRET_MANAGER;
      case KMS:
        KmsConfig kmsConfig = wingsPersistence.get(KmsConfig.class, kmsId);
        Preconditions.checkNotNull(kmsConfig,
            "could not find kmsId " + kmsId + " for " + type + " id: " + parentId + " encryptionType" + encryptionType);
        return kmsConfig.getName();
      case VAULT:
        VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, kmsId);
        Preconditions.checkNotNull(vaultConfig,
            "could not find kmsId " + kmsId + " for " + type + " id: " + parentId + " encryptionType" + encryptionType);
        return vaultConfig.getName();
      default:
        throw new IllegalArgumentException("Invalid type: " + encryptionType);
    }
  }

  private void vaildateKmsConfigs(String accountId) {
    Collection<KmsConfig> kmsConfigs = kmsService.listKmsConfigs(accountId, false);
    for (KmsConfig kmsConfig : kmsConfigs) {
      KmsSetupAlert kmsSetupAlert =
          KmsSetupAlert.builder()
              .kmsId(kmsConfig.getUuid())
              .message(kmsConfig.getName() + "(Amazon KMS) is not able to encrypt/decrypt. Please check your setup")
              .build();
      try {
        kmsService.encrypt(UUID.randomUUID().toString().toCharArray(), accountId, kmsConfig);
        alertService.closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      } catch (Exception e) {
        logger.error("Could not validate kms for account {} and kmsId {}", accountId, kmsConfig.getUuid(), e);
        alertService.openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      }
    }
  }

  private void validateVaultConfigs(String accountId) {
    Collection<VaultConfig> vaultConfigs = vaultService.listVaultConfigs(accountId, false);
    for (VaultConfig vaultConfig : vaultConfigs) {
      KmsSetupAlert kmsSetupAlert =
          KmsSetupAlert.builder()
              .kmsId(vaultConfig.getUuid())
              .message(vaultConfig.getName()
                  + "(Hashicorp Vault) is not able to encrypt/decrypt. Please check your setup and ensure that token is not expired")
              .build();
      try {
        vaultService.encrypt(
            VAULT_VAILDATION_URL, VAULT_VAILDATION_URL, accountId, SettingVariableTypes.VAULT, vaultConfig, null);
        alertService.closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      } catch (Exception e) {
        logger.error("Could not validate vault for account {} and kmsId {}", accountId, vaultConfig.getUuid(), e);
        alertService.openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      }
    }
  }
}
