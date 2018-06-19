package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.joining;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.dl.HQuery.excludeCount;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.USER;
import static software.wings.security.EncryptionType.LOCAL;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.utils.WingsReflectionUtils.getEncryptedFields;
import static software.wings.utils.WingsReflectionUtils.getEncryptedRefField;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.BaseFile;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityType;
import software.wings.beans.ErrorCode;
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
import software.wings.core.queue.Queue;
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
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
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 10/30/17.
 */
public class SecretManagerImpl implements SecretManager {
  public static final String HARNESS_DEFAULT_SECRET_MANAGER = "Harness Manager";
  @SuppressFBWarnings("MS_MUTABLE_ARRAY") public static final char[] ENCRYPTED_FIELD_MASK = "*******".toCharArray();
  protected static final Logger logger = LoggerFactory.getLogger(SecretManagerImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;
  @Inject private EncryptionService encryptionService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private AlertService alertService;
  @Inject private FileService fileService;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;

  @Override
  public EncryptionType getEncryptionType(String accountId) {
    if (vaultService.getSecretConfig(accountId) != null) {
      return EncryptionType.VAULT;
    }

    if (kmsService.getSecretConfig(accountId) != null) {
      return EncryptionType.KMS;
    }

    return LOCAL;
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
                 .encryptionType(LOCAL)
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

  public String encrypt(String accountId, String secret) {
    EncryptedData encryptedData = encrypt(getEncryptionType(accountId), accountId,
        SettingVariableTypes.APM_VERIFICATION, secret.toCharArray(), null, UUID.randomUUID().toString());
    return wingsPersistence.save(encryptedData);
  }

  public Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, refId);
    if (encryptedData == null) {
      logger.info("No encrypted record set for field {} for id: {}", fieldName, refId);
      return Optional.empty();
    }
    EncryptionConfig encryptionConfig =
        getEncryptionConfig(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());

    return Optional.of(EncryptedDataDetail.builder()
                           .encryptionType(encryptedData.getEncryptionType())
                           .encryptedData(encryptedData)
                           .encryptionConfig(encryptionConfig)
                           .fieldName(fieldName)
                           .build());
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
                                       .encryptionType(LOCAL)
                                       .encryptedData(EncryptedData.builder()
                                                          .encryptionKey(object.getAccountId())
                                                          .encryptedValue((char[]) f.get(object))
                                                          .build())
                                       .fieldName(f.getName())
                                       .build());
        } else {
          String id = (String) encryptedRefField.get(object);
          EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, id);
          if (encryptedData == null) {
            logger.info("No encrypted record set for field {} for id: {}", f.getName(), id);
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

          if (isNotEmpty(workflowExecutionId)) {
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
  public void maskEncryptedFields(Encryptable object) {
    List<Field> encryptedFields = object.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        f.set(object, ENCRYPTED_FIELD_MASK);
      }
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }
  }

  @Override
  public void resetUnchangedEncryptedFields(Encryptable sourceObject, Encryptable destinationObject) {
    Validator.equalCheck(sourceObject.getClass().getName(), destinationObject.getClass().getName());

    List<Field> encryptedFields = sourceObject.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        if (java.util.Arrays.equals((char[]) f.get(destinationObject), ENCRYPTED_FIELD_MASK)) {
          f.set(destinationObject, f.get(sourceObject));
        }
      }
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }
  }

  @Override
  public PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId,
      String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);

    pageRequest.addFilter("encryptedDataId", Operator.IN, secretIds.toArray());
    pageRequest.addFilter("accountId", Operator.EQ, accountId);
    PageResponse<SecretUsageLog> response = wingsPersistence.query(SecretUsageLog.class, pageRequest);
    response.getResponse().forEach(secretUsageLog -> {
      if (isNotEmpty(secretUsageLog.getWorkflowExecutionId())) {
        WorkflowExecution workflowExecution =
            wingsPersistence.get(WorkflowExecution.class, secretUsageLog.getWorkflowExecutionId());
        if (workflowExecution != null) {
          secretUsageLog.setWorkflowExecutionName(workflowExecution.getName());
        }
      }
    });
    return response;
  }

  @Override
  public long getUsageLogsSize(String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);
    final PageRequest<SecretUsageLog> request =
        aPageRequest().addFilter("encryptedDataId", Operator.IN, secretIds.toArray()).build();
    return wingsPersistence.getCount(SecretUsageLog.class, request);
  }

  @Override
  public List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);
    return wingsPersistence.createQuery(SecretChangeLog.class, excludeCount)
        .filter("accountId", accountId)
        .field("encryptedDataId")
        .hasAnyOf(secretIds)
        .order("-createdAt")
        .asList();
  }

  @Override
  public Collection<UuidAware> listEncryptedValues(String accountId) {
    Map<String, UuidAware> rv = new HashMap<>();
    try (HIterator<EncryptedData> query = new HIterator<>(
             wingsPersistence.createQuery(EncryptedData.class)
                 .filter("accountId", accountId)
                 .field("type")
                 .hasNoneOf(Lists.newArrayList(SettingVariableTypes.SECRET_TEXT, SettingVariableTypes.CONFIG_FILE))
                 .fetch())) {
      while (query.hasNext()) {
        EncryptedData data = query.next();
        if (data.getParentIds() != null && data.getType() != SettingVariableTypes.KMS) {
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
    }
    return rv.values();
  }

  @SuppressFBWarnings("DMI_INVOKING_TOSTRING_ON_ARRAY")
  @Override
  public String getEncryptedYamlRef(Encryptable object, String... fieldNames) throws IllegalAccessException {
    if (object.getSettingType() == SettingVariableTypes.CONFIG_FILE) {
      String encryptedFieldRefId = ((ConfigFile) object).getEncryptedFileId();
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + object);
      return encryptedData.getEncryptionType().getYamlName() + ":" + encryptedFieldRefId;
    }
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

    Field encryptedFieldRef = getEncryptedRefField(encryptedField, object);
    encryptedFieldRef.setAccessible(true);
    String encryptedFieldRefId = (String) encryptedFieldRef.get(object);
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
    Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + object);

    return encryptedData.getEncryptionType().getYamlName() + ":" + encryptedFieldRefId;
  }

  @Override
  public EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef) throws IllegalAccessException {
    Preconditions.checkState(isNotEmpty(encryptedYamlRef));
    String[] tags = encryptedYamlRef.split(":");
    String fieldRefId = tags[1];
    return wingsPersistence.get(EncryptedData.class, fieldRefId);
  }

  @Override
  public boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretId,
      EncryptionType toEncryptionType, String toSecretId) {
    Preconditions.checkState(isNotEmpty(accountId), "accountId can't be blank");
    Preconditions.checkNotNull(fromEncryptionType, "fromEncryptionType can't be blank");
    Preconditions.checkState(isNotEmpty(fromSecretId), "fromVaultId can't be blank");
    Preconditions.checkNotNull(toEncryptionType, "toEncryptionType can't be blank");
    Preconditions.checkState(isNotEmpty(toSecretId), "toVaultId can't be blank");

    Query<EncryptedData> query =
        wingsPersistence.createQuery(EncryptedData.class).filter("accountId", accountId).filter("kmsId", fromSecretId);

    if (toEncryptionType == EncryptionType.VAULT) {
      query = query.field("type").notEqual(SettingVariableTypes.VAULT);
    }

    try (HIterator<EncryptedData> iterator = new HIterator<>(query.fetch())) {
      while (iterator.hasNext()) {
        EncryptedData dataToTransition = iterator.next();
        transitionKmsQueue.send(KmsTransitionEvent.builder()
                                    .accountId(accountId)
                                    .entityId(dataToTransition.getUuid())
                                    .fromEncryptionType(fromEncryptionType)
                                    .fromKmsId(fromSecretId)
                                    .toEncryptionType(toEncryptionType)
                                    .toKmsId(toSecretId)
                                    .build());
      }
    }
    return true;
  }

  public char[] decryptYamlRef(String encryptedYamlRef) throws IllegalAccessException, IOException {
    EncryptedData encryptedData = getEncryptedDataFromYamlRef(encryptedYamlRef);
    if (encryptedData == null) {
      throw new WingsException("encryptedData is null", USER);
    }

    EncryptionConfig encryptionConfig =
        getEncryptionConfig(encryptedData.getAccountId(), encryptedData.getKmsId(), encryptedData.getEncryptionType());
    return encryptionService.getDecryptedValue(EncryptedDataDetail.builder()
                                                   .encryptedData(encryptedData)
                                                   .encryptionConfig(encryptionConfig)
                                                   .encryptionType(encryptedData.getEncryptionType())
                                                   .build());
  }

  @Override
  public void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType,
      String fromKmsId, EncryptionType toEncryptionType, String toKmsId) throws IOException {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    Preconditions.checkNotNull(encryptedData, "No encrypted data with id " + entityId);
    Preconditions.checkState(encryptedData.getEncryptionType() == fromEncryptionType,
        "mismatch between saved encrypted type and from encryption type");
    EncryptionConfig fromConfig = getEncryptionConfig(accountId, fromKmsId, fromEncryptionType);
    Preconditions.checkNotNull(
        fromConfig, "No kms found for account " + accountId + " with id " + entityId + " type: " + fromEncryptionType);
    EncryptionConfig toConfig = getEncryptionConfig(accountId, toKmsId, toEncryptionType);
    Preconditions.checkNotNull(
        toConfig, "No kms found for account " + accountId + " with id " + entityId + " type: " + fromEncryptionType);

    if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
      changeFileSecretManager(accountId, encryptedData, toEncryptionType, toConfig);
      return;
    }

    char[] decrypted = null;
    switch (fromEncryptionType) {
      case KMS:
        decrypted = kmsService.decrypt(encryptedData, accountId, (KmsConfig) fromConfig);
        break;
      case VAULT:
        decrypted = vaultService.decrypt(encryptedData, accountId, (VaultConfig) fromConfig);
        break;

      default:
        throw new IllegalStateException("Invalid type : " + fromEncryptionType);
    }

    EncryptedData encrypted;
    switch (toEncryptionType) {
      case KMS:
        encrypted = kmsService.encrypt(decrypted, accountId, (KmsConfig) toConfig);
        break;
      case VAULT:
        String encryptionKey = encryptedData.getEncryptionKey();

        SettingVariableTypes settingVariableType = encryptedData.getType();
        String keyName = settingVariableType + "/" + encryptedData.getName();
        encrypted = vaultService.encrypt(keyName, String.valueOf(decrypted), accountId, settingVariableType,
            (VaultConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      default:
        throw new IllegalStateException("Invalid type : " + toEncryptionType);
    }

    encryptedData.setKmsId(toKmsId);
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setEncryptionKey(encrypted.getEncryptionKey());
    encryptedData.setEncryptedValue(encrypted.getEncryptedValue());

    wingsPersistence.save(encryptedData);
  }

  private void changeFileSecretManager(String accountId, EncryptedData encryptedData, EncryptionType toEncryptionType,
      EncryptionConfig toConfig) throws IOException {
    String decryptedFileContent = getFileContents(accountId, encryptedData.getUuid());
    String savedFileId = String.valueOf(encryptedData.getEncryptedValue());
    EncryptedData encryptedFileData = null;
    switch (toEncryptionType) {
      case KMS:
        encryptedFileData = kmsService.encryptFile(accountId, (KmsConfig) toConfig, encryptedData.getName(),
            new BoundedInputStream(IOUtils.toInputStream(decryptedFileContent, "UTF-8")));
        fileService.deleteFile(savedFileId, CONFIGS);
        break;

      case VAULT:
        encryptedFileData =
            vaultService.encryptFile(accountId, vaultService.getSecretConfig(accountId), encryptedData.getName(),
                new BoundedInputStream(IOUtils.toInputStream(decryptedFileContent, "UTF-8")), encryptedData);
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + toEncryptionType);
    }

    encryptedData.setEncryptionKey(encryptedFileData.getEncryptionKey());
    encryptedData.setEncryptedValue(encryptedFileData.getEncryptedValue());
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setKmsId(toConfig.getUuid());
    wingsPersistence.save(encryptedData);
  }

  @Override
  public void checkAndAlertForInvalidManagers() {
    wingsPersistence.createQuery(Account.class, excludeAuthority)
        .asKeyList()
        .stream()
        .map(key -> key.getId().toString())
        .forEach(accountId -> {
          vaildateKmsConfigs(accountId);
          validateVaultConfigs(accountId);
        });
  }

  @Override
  public String saveSecret(String accountId, String name, String value) {
    EncryptionType encryptionType = getEncryptionType(accountId);
    return processEncryption(accountId, name, value, encryptionType);
  }

  private String processEncryption(String accountId, String name, String value, EncryptionType encryptionType) {
    EncryptedData encryptedData =
        encrypt(encryptionType, accountId, SettingVariableTypes.SECRET_TEXT, value.toCharArray(), null, name);
    String encryptedDataId;
    try {
      encryptedDataId = wingsPersistence.save(encryptedData);
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR)
          .addParam("reason", "Variable " + name + " already exists");
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
  public String saveSecretUsingLocalMode(String accountId, String name, String value) {
    return processEncryption(accountId, name, value, EncryptionType.LOCAL);
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
    savedData.setEncryptionType(encryptedData.getEncryptionType());
    savedData.setKmsId(encryptedData.getKmsId());
    try {
      wingsPersistence.save(savedData);
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR)
          .addParam("reason", "Variable " + name + " already exists");
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
                                                 .filter("accountId", accountId)
                                                 .filter("encryptedValue", uuId)
                                                 .asList();
    if (!serviceVariables.isEmpty()) {
      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR)
          .addParam("reason",
              "Being used by " + serviceVariables.stream().map(ServiceVariable::getName).collect(joining(", ")));
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
                                          .encryptionType(LOCAL)
                                          .kmsId(null)
                                          .type(SettingVariableTypes.CONFIG_FILE)
                                          .fileSize(inputStream.getTotalBytesRead())
                                          .enabled(true)
                                          .build();
        recordId = wingsPersistence.save(encryptedData);
        break;

      case KMS:
        recordId = wingsPersistence.save(
            kmsService.encryptFile(accountId, kmsService.getSecretConfig(accountId), name, inputStream));
        break;

      case VAULT:
        recordId = wingsPersistence.save(
            vaultService.encryptFile(accountId, vaultService.getSecretConfig(accountId), name, inputStream, null));
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

  @SuppressFBWarnings("DM_DEFAULT_ENCODING")
  @Override
  public String getFileContents(String accountId, String uuid) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    Preconditions.checkNotNull(encryptedData, "could not find file with id " + uuid);
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    try {
      OutputStream output = new ByteArrayOutputStream();
      File file;
      switch (encryptionType) {
        case LOCAL:
          file = new File(Files.createTempDir(), generateUuid());
          logger.info("Temp file path [{}]", file.getAbsolutePath());
          fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
          EncryptionUtils.decryptToStream(file, encryptedData.getEncryptionKey(), output);
          break;

        case KMS:
          file = new File(Files.createTempDir(), generateUuid());
          logger.info("Temp file path [{}]", file.getAbsolutePath());
          fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
          kmsService.decryptToStream(file, accountId, encryptedData, output);
          break;

        case VAULT:
          vaultService.decryptToStream(accountId, encryptedData, output);
          break;

        default:
          throw new IllegalArgumentException("Invalid type " + encryptionType);
      }
      output.flush();
      return output.toString();
    } catch (IOException e) {
      throw new WingsException(INVALID_ARGUMENT, e).addParam("args", "Failed to get content");
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
        encryptedFileData = kmsService.encryptFile(accountId, kmsService.getSecretConfig(accountId), name, inputStream);
        fileService.deleteFile(savedFileId, CONFIGS);
        break;

      case VAULT:
        encryptedFileData = vaultService.encryptFile(
            accountId, vaultService.getSecretConfig(accountId), name, inputStream, encryptedData);
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }

    encryptedData.setEncryptionKey(encryptedFileData.getEncryptionKey());
    encryptedData.setEncryptedValue(encryptedFileData.getEncryptedValue());
    encryptedData.setName(name);
    encryptedData.setFileSize(inputStream.getTotalBytesRead());
    wingsPersistence.save(encryptedData);

    // update parent's file size
    for (String parentId : encryptedData.getParentIds()) {
      ConfigFile configFile = (ConfigFile) fetchParent(SettingVariableTypes.CONFIG_FILE, encryptedData.getAccountId(),
          parentId, encryptedData.getKmsId(), encryptionType);
      if (configFile != null) {
        configFile.setSize(inputStream.getTotalBytesRead());
      }
      wingsPersistence.save(configFile);
    }

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

  @SuppressFBWarnings("SBSC_USE_STRINGBUFFER_CONCATENATION")
  @Override
  public boolean deleteFile(String accountId, String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull("No encrypted record found with id " + uuId);
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .filter("accountId", accountId)
                                       .filter("encryptedFileId", uuId)
                                       .asList();
    if (!configFiles.isEmpty()) {
      String errorMessage = "Being used by ";
      for (ConfigFile configFile : configFiles) {
        errorMessage += configFile.getFileName() + ", ";
      }

      throw new WingsException(ErrorCode.KMS_OPERATION_ERROR).addParam("reason", errorMessage);
    }

    switch (encryptedData.getEncryptionType()) {
      case LOCAL:
      case KMS:
        fileService.deleteFile(String.valueOf(encryptedData.getEncryptedValue()), CONFIGS);
        break;
      case VAULT:
        vaultService.deleteSecret(accountId, encryptedData.getEncryptionKey(),
            vaultService.getVaultConfig(accountId, encryptedData.getKmsId()));
        break;
      default:
        throw new IllegalStateException("Invalid type " + encryptedData.getEncryptionType());
    }
    return wingsPersistence.delete(EncryptedData.class, uuId);
  }

  @Override
  public List<EncryptedData> listSecrets(String accountId, SettingVariableTypes type) throws IllegalAccessException {
    List<EncryptedData> rv = new ArrayList<>();
    try (HIterator<EncryptedData> iterator = new HIterator<>(wingsPersistence.createQuery(EncryptedData.class)
                                                                 .filter("accountId", accountId)
                                                                 .filter("type", type)
                                                                 .fetch())) {
      while (iterator.hasNext()) {
        EncryptedData encryptedData = iterator.next();
        encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
        encryptedData.setEncryptionKey(SECRET_MASK);
        encryptedData.setEncryptedBy(getSecretManagerName(
            type, encryptedData.getUuid(), encryptedData.getKmsId(), encryptedData.getEncryptionType()));

        encryptedData.setSetupUsage(getSecretUsage(accountId, encryptedData.getUuid()).size());
        encryptedData.setRunTimeUsage(getUsageLogsSize(encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT));
        encryptedData.setChangeLog(
            getChangeLogs(accountId, encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT).size());
        rv.add(encryptedData);
      }
    }

    return rv;
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(PageRequest<EncryptedData> pageRequest) throws IllegalAccessException {
    PageResponse<EncryptedData> pageResponse = wingsPersistence.query(EncryptedData.class, pageRequest);

    for (EncryptedData encryptedData : pageResponse.getResponse()) {
      encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
      encryptedData.setEncryptionKey(SECRET_MASK);
      encryptedData.setEncryptedBy(getSecretManagerName(encryptedData.getType(), encryptedData.getUuid(),
          encryptedData.getKmsId(), encryptedData.getEncryptionType()));

      encryptedData.setSetupUsage(getSecretUsage(encryptedData.getAccountId(), encryptedData.getUuid()).size());
      encryptedData.setRunTimeUsage(getUsageLogsSize(encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT));
      encryptedData.setChangeLog(
          getChangeLogs(encryptedData.getAccountId(), encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT)
              .size());
    }
    return pageResponse;
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

  @Override
  public EncryptionConfig getEncryptionConfig(String accountId, String entityId, EncryptionType encryptionType) {
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
        ServiceVariable serviceVariable =
            wingsPersistence.createQuery(ServiceVariable.class).filter("_id", entityId).get();

        if (serviceVariable != null) {
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
        SettingAttribute settingAttribute =
            wingsPersistence.createQuery(SettingAttribute.class).filter("_id", entityId).get();

        if (settingAttribute != null) {
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
        ServiceVariable serviceVariable =
            wingsPersistence.createQuery(ServiceVariable.class).filter("_id", parentId).get();

        if (serviceVariable == null) {
          return null;
        }
        serviceVariable.setValue(SECRET_MASK.toCharArray());
        if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
          ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
          Preconditions.checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
          serviceVariable.setServiceId(serviceTemplate.getServiceId());
        }
        serviceVariable.setEncryptionType(encryptionType);
        serviceVariable.setEncryptedBy(encryptedBy);
        return serviceVariable;

      case CONFIG_FILE:
        ConfigFile configFile = wingsPersistence.createQuery(ConfigFile.class).filter("_id", parentId).get();
        if (configFile == null) {
          return null;
        }

        if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
          ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, configFile.getEntityId());
          Preconditions.checkNotNull(serviceTemplate, "can't find service template " + configFile);
          configFile.setServiceId(serviceTemplate.getServiceId());
        }
        configFile.setEncryptionType(encryptionType);
        configFile.setEncryptedBy(encryptedBy);
        return configFile;

      case VAULT:
        VaultConfig vaultConfig = wingsPersistence.createQuery(VaultConfig.class).filter("_id", parentId).get();
        if (vaultConfig == null) {
          return null;
        }

        vaultConfig.setEncryptionType(encryptionType);
        vaultConfig.setEncryptedBy(encryptedBy);
        return vaultConfig;

      default:
        SettingAttribute settingAttribute =
            wingsPersistence.createQuery(SettingAttribute.class).filter("_id", parentId).get();
        if (settingAttribute == null) {
          return null;
        }

        settingAttribute.setEncryptionType(encryptionType);
        settingAttribute.setEncryptedBy(encryptedBy);
        return settingAttribute;
    }
  }

  private String getSecretManagerName(
      SettingVariableTypes type, String parentId, String kmsId, EncryptionType encryptionType) {
    switch (encryptionType) {
      case LOCAL:
        Preconditions.checkState(isEmpty(kmsId),
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
        timeLimiter.callWithTimeout(
            ()
                -> kmsService.encrypt(UUID.randomUUID().toString().toCharArray(), accountId, kmsConfig),
            15L, TimeUnit.SECONDS, true);
        alertService.closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      } catch (UncheckedTimeoutException ex) {
        logger.warn("Timed out validating kms for account {} and kmsId {}", accountId, kmsConfig.getUuid());
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
        timeLimiter.callWithTimeout(()
                                        -> vaultService.encrypt(VAULT_VAILDATION_URL, VAULT_VAILDATION_URL, accountId,
                                            SettingVariableTypes.VAULT, vaultConfig, null),
            15L, TimeUnit.SECONDS, true);
        alertService.closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      } catch (UncheckedTimeoutException ex) {
        logger.warn("Timed out validating vault for account {} and kmsId {}", accountId, vaultConfig.getUuid());
      } catch (Exception e) {
        logger.error("Could not validate vault for account {} and kmsId {}", accountId, vaultConfig.getUuid(), e);
        alertService.openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      }
    }
  }
}
