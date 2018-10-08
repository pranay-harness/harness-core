package software.wings.service.impl.security;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeCount;
import static io.harness.reflection.ReflectUtils.getEncryptedFields;
import static io.harness.reflection.ReflectUtils.getEncryptedRefField;
import static java.util.stream.Collectors.joining;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.security.EncryptionType.LOCAL;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.KmsOperationException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.UuidAware;
import io.harness.queue.Queue;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.Base;
import software.wings.beans.BaseFile;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityType;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.dl.WingsPersistence;
import software.wings.security.EncryptionType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.Validator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private AlertService alertService;
  @Inject private FileService fileService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private SettingsService settingsService;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ConfigService configService;

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
      char[] secret, EncryptedData encryptedData, String secretName, UsageRestrictions usageRestrictions) {
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
    rv.setUsageRestrictions(usageRestrictions);
    return rv;
  }

  public String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions) {
    EncryptedData encryptedData =
        encrypt(getEncryptionType(accountId), accountId, SettingVariableTypes.APM_VERIFICATION, secret.toCharArray(),
            null, UUID.randomUUID().toString(), usageRestrictions);
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
  public List<EncryptedDataDetail> getEncryptionDetails(
      EncryptableSetting object, String appId, String workflowExecutionId) {
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
  public void maskEncryptedFields(EncryptableSetting object) {
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
  public void resetUnchangedEncryptedFields(EncryptableSetting sourceObject, EncryptableSetting destinationObject) {
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
    Set<Parent> parents = new HashSet<>();
    try (HIterator<EncryptedData> query = new HIterator<>(
             wingsPersistence.createQuery(EncryptedData.class)
                 .filter("accountId", accountId)
                 .field("type")
                 .hasNoneOf(Lists.newArrayList(SettingVariableTypes.SECRET_TEXT, SettingVariableTypes.CONFIG_FILE))
                 .fetch())) {
      while (query.hasNext()) {
        EncryptedData data = query.next();
        if (!isEmpty(data.getParentIds()) && data.getType() != SettingVariableTypes.KMS) {
          data.getParentIds().forEach(parentId
              -> parents.add(Parent.builder()
                                 .id(parentId)
                                 .variableType(data.getType())
                                 .encryptionDetail(EncryptionDetail.builder()
                                                       .encryptionType(data.getEncryptionType())
                                                       .secretManagerName(getSecretManagerName(data.getType(), parentId,
                                                           data.getKmsId(), data.getEncryptionType()))
                                                       .build())
                                 .build()));
        }
      }
    }
    return fetchParents(accountId, parents);
  }

  @Override
  public PageResponse<UuidAware> listEncryptedValues(String accountId, PageRequest<EncryptedData> pageRequest) {
    Set<Parent> parents = new HashSet<>();
    PageResponse<EncryptedData> pageResponse = wingsPersistence.query(EncryptedData.class, pageRequest);
    pageResponse.getResponse().forEach(data -> {
      if (data.getParentIds() != null && data.getType() != SettingVariableTypes.KMS) {
        for (String parentId : data.getParentIds()) {
          parents.add(Parent.builder()
                          .id(parentId)
                          .variableType(data.getType())
                          .encryptionDetail(EncryptionDetail.builder()
                                                .encryptionType(data.getEncryptionType())
                                                .secretManagerName(getSecretManagerName(data.getType(), parentId,
                                                    data.getKmsId(), data.getEncryptionType()))
                                                .build())
                          .build());
        }
      }
    });
    List<UuidAware> rv = fetchParents(accountId, parents);
    return aPageResponse()
        .withResponse(rv)
        .withTotal(rv.size())
        .withOffset(pageResponse.getOffset())
        .withLimit(pageResponse.getLimit())
        .build();
  }

  @SuppressFBWarnings("DMI_INVOKING_TOSTRING_ON_ARRAY")
  @Override
  public String getEncryptedYamlRef(EncryptableSetting object, String... fieldNames) throws IllegalAccessException {
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

  @Override
  public void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType,
      String fromKmsId, EncryptionType toEncryptionType, String toKmsId) {
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

    char[] decrypted;
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
        encrypted = vaultService.encrypt(keyName, decrypted == null ? null : String.valueOf(decrypted), accountId,
            settingVariableType, (VaultConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
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

  private void changeFileSecretManager(
      String accountId, EncryptedData encryptedData, EncryptionType toEncryptionType, EncryptionConfig toConfig) {
    byte[] decryptedFileContent = getFileContents(accountId, encryptedData.getUuid());
    String savedFileId = String.valueOf(encryptedData.getEncryptedValue());
    EncryptedData encryptedFileData;
    switch (toEncryptionType) {
      case KMS:
        encryptedFileData = kmsService.encryptFile(accountId, (KmsConfig) toConfig, encryptedData.getName(),
            new BoundedInputStream(new ByteArrayInputStream(decryptedFileContent)));
        fileService.deleteFile(savedFileId, CONFIGS);
        break;

      case VAULT:
        encryptedFileData =
            vaultService.encryptFile(accountId, vaultService.getSecretConfig(accountId), encryptedData.getName(),
                new BoundedInputStream(new ByteArrayInputStream(decryptedFileContent)), encryptedData);
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + toEncryptionType);
    }

    encryptedData.setEncryptionKey(encryptedFileData.getEncryptionKey());
    encryptedData.setEncryptedValue(encryptedFileData.getEncryptedValue());
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setKmsId(toConfig.getUuid());
    encryptedData.setBase64Encoded(true);
    wingsPersistence.save(encryptedData);
  }

  @Override
  public void checkAndAlertForInvalidManagers() {
    Query<Account> query = wingsPersistence.createQuery(Account.class);
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        Account account = records.next();
        try {
          vaildateKmsConfigs(account.getUuid());
          validateVaultConfigs(account.getUuid());
          vaultService.renewTokens(account.getUuid());
        } catch (Exception e) {
          logger.info(
              "Failed to validate secret manager for {} account id {}", account.getAccountName(), account.getUuid(), e);
        }
      }
    }
  }

  @Override
  public EncryptedData getEncryptedDataByName(String accountId, String name) {
    return wingsPersistence.createQuery(EncryptedData.class)
        .filter(EncryptedData.ACCOUNT_ID_KEY, accountId)
        .filter(EncryptedData.NAME_KEY, name)
        .get();
  }

  @Override
  public String saveSecret(String accountId, String name, String value, UsageRestrictions usageRestrictions) {
    EncryptionType encryptionType = getEncryptionType(accountId);
    return processEncryption(accountId, name, value, encryptionType, usageRestrictions);
  }

  private String processEncryption(
      String accountId, String name, String value, EncryptionType encryptionType, UsageRestrictions usageRestrictions) {
    usageRestrictionsService.validateUsageRestrictionsOnEntitySave(accountId, usageRestrictions);
    EncryptedData encryptedData = encrypt(encryptionType, accountId, SettingVariableTypes.SECRET_TEXT,
        value.toCharArray(), null, name, usageRestrictions);
    encryptedData.addSearchTag(name);
    String encryptedDataId;
    try {
      encryptedDataId = wingsPersistence.save(encryptedData);
    } catch (DuplicateKeyException e) {
      String reason = "Variable " + name + " already exists";
      throw new KmsOperationException(reason);
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
  public String saveSecretUsingLocalMode(
      String accountId, String name, String value, UsageRestrictions usageRestrictions) {
    return processEncryption(accountId, name, value, EncryptionType.LOCAL, usageRestrictions);
  }

  @Override
  public boolean updateSecret(
      String accountId, String uuId, String name, String value, UsageRestrictions usageRestrictions) {
    EncryptedData savedData = wingsPersistence.get(EncryptedData.class, uuId);
    if (savedData == null) {
      return false;
    }

    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
        accountId, savedData.getUsageRestrictions(), usageRestrictions);

    String description = value.equals(SECRET_MASK) ? "Changed name" : "Changed name & value";
    if (usageRestrictions != null) {
      description += " & usage restrictions";
    }
    savedData.removeSearchTag(null, savedData.getName(), null);
    savedData.setName(name);
    savedData.addSearchTag(name);
    if (!value.equals(SECRET_MASK)) {
      EncryptedData encryptedData = encrypt(getEncryptionType(accountId), accountId, SettingVariableTypes.SECRET_TEXT,
          value.toCharArray(), savedData, name, usageRestrictions);
      savedData.setEncryptionKey(encryptedData.getEncryptionKey());
      savedData.setEncryptedValue(encryptedData.getEncryptedValue());
      savedData.setEncryptionType(encryptedData.getEncryptionType());
      savedData.setKmsId(encryptedData.getKmsId());
    }
    savedData.setUsageRestrictions(usageRestrictions);
    wingsPersistence.save(savedData);

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
  public boolean updateUsageRestrictionsForSecretOrFile(
      String accountId, String uuId, UsageRestrictions usageRestrictions) {
    EncryptedData savedData = wingsPersistence.get(EncryptedData.class, uuId);
    if (savedData == null) {
      return false;
    }
    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
        accountId, savedData.getUsageRestrictions(), usageRestrictions);
    savedData.setUsageRestrictions(usageRestrictions);

    try {
      wingsPersistence.save(savedData);
    } catch (DuplicateKeyException e) {
      throw new KmsOperationException("Unable to save Restrictions");
    }

    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(uuId)
                                .description("Changed restrictions")
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
      String reason = "Being used by " + serviceVariables.stream().map(ServiceVariable::getName).collect(joining(", "));
      throw new KmsOperationException(reason, USER);
    }

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, encryptedData.getUsageRestrictions())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    return wingsPersistence.delete(EncryptedData.class, uuId);
  }

  @Override
  public boolean deleteSecretUsingUuid(String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    return wingsPersistence.delete(EncryptedData.class, uuId);
  }

  @Override
  public String saveFile(
      String accountId, String name, UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    usageRestrictionsService.validateUsageRestrictionsOnEntitySave(accountId, usageRestrictions);
    EncryptionType encryptionType = getEncryptionType(accountId);
    EncryptedData encryptedData;
    switch (encryptionType) {
      case LOCAL:
        try {
          byte[] inputBytes = ByteStreams.toByteArray(inputStream);
          byte[] base64Encoded = encodeBase64ToByteArray(inputBytes);
          byte[] encryptedFileContent = EncryptionUtils.encrypt(base64Encoded, accountId);
          try (InputStream encryptedInputStream = new ByteArrayInputStream(encryptedFileContent)) {
            BaseFile baseFile = new BaseFile();
            baseFile.setFileName(name);
            String fileId = fileService.saveFile(baseFile, encryptedInputStream, CONFIGS);
            encryptedData = EncryptedData.builder()
                                .accountId(accountId)
                                .name(name)
                                .encryptionKey(accountId)
                                .encryptedValue(fileId.toCharArray())
                                .encryptionType(LOCAL)
                                .kmsId(null)
                                .type(SettingVariableTypes.CONFIG_FILE)
                                .fileSize(inputStream.getTotalBytesRead())
                                .enabled(true)
                                .base64Encoded(true)
                                .build();
          }
        } catch (IOException e) {
          throw new WingsException(DEFAULT_ERROR_CODE, e);
        }
        break;

      case KMS:
        encryptedData = kmsService.encryptFile(accountId, kmsService.getSecretConfig(accountId), name, inputStream);
        break;

      case VAULT:
        encryptedData =
            vaultService.encryptFile(accountId, vaultService.getSecretConfig(accountId), name, inputStream, null);
        break;

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }

    encryptedData.setUsageRestrictions(usageRestrictions);
    String recordId;
    try {
      recordId = wingsPersistence.save(encryptedData);
    } catch (DuplicateKeyException e) {
      throw new KmsOperationException("File " + name + " already exists");
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
        return EncryptionUtils.decrypt(readInto, encryptedData.getEncryptionKey(), encryptedData.isBase64Encoded());

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
  public byte[] getFileContents(String accountId, String uuid) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    Preconditions.checkNotNull(encryptedData, "could not find file with id " + uuid);
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      File file;
      switch (encryptionType) {
        case LOCAL:
          file = new File(Files.createTempDir(), generateUuid());
          logger.info("Temp file path [{}]", file.getAbsolutePath());
          fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
          EncryptionUtils.decryptToStream(
              file, encryptedData.getEncryptionKey(), output, encryptedData.isBase64Encoded());
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
      return output.toByteArray();
    } catch (IOException e) {
      throw new WingsException(INVALID_ARGUMENT, e).addParam("args", "Failed to get content");
    }
  }

  @Override
  public boolean updateFile(
      String accountId, String name, String uuid, UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    Preconditions.checkNotNull(encryptedData, "could not find file with id " + uuid);
    String oldName = encryptedData.getName();

    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
        accountId, encryptedData.getUsageRestrictions(), usageRestrictions);

    String savedFileId = String.valueOf(encryptedData.getEncryptedValue());
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    EncryptedData encryptedFileData;
    switch (encryptionType) {
      case LOCAL:
        try {
          byte[] inputBytes = ByteStreams.toByteArray(inputStream);
          byte[] base64Encoded = encodeBase64ToByteArray(inputBytes);
          byte[] encryptedFileContent = EncryptionUtils.encrypt(base64Encoded, accountId);
          try (InputStream encryptedInputStream = new ByteArrayInputStream(encryptedFileContent)) {
            BaseFile baseFile = new BaseFile();
            baseFile.setFileName(name);
            String fileId = fileService.saveFile(baseFile, encryptedInputStream, CONFIGS);
            encryptedFileData =
                EncryptedData.builder().encryptionKey(accountId).encryptedValue(fileId.toCharArray()).build();
            fileService.deleteFile(savedFileId, CONFIGS);
          }
        } catch (IOException e) {
          throw new WingsException(DEFAULT_ERROR_CODE, e);
        }
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
    encryptedData.setUsageRestrictions(usageRestrictions);
    encryptedData.setBase64Encoded(true);
    wingsPersistence.save(encryptedData);

    // update parent's file size
    Set<Parent> parents = new HashSet<>();
    for (String parentId : encryptedData.getParentIds()) {
      parents.add(
          Parent.builder()
              .id(parentId)
              .variableType(SettingVariableTypes.CONFIG_FILE)
              .encryptionDetail(EncryptionDetail.builder().encryptionType(encryptedData.getEncryptionType()).build())
              .build());
    }
    List<UuidAware> configFiles = fetchParents(accountId, parents);
    configFiles.forEach(configFile -> {
      ((ConfigFile) configFile).setSize(inputStream.getTotalBytesRead());
      wingsPersistence.save((ConfigFile) configFile);
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
    });

    return true;
  }

  @SuppressFBWarnings("SBSC_USE_STRINGBUFFER_CONCATENATION")
  @Override
  public boolean deleteFile(String accountId, String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, encryptedData.getUsageRestrictions())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .filter("accountId", accountId)
                                       .filter("encryptedFileId", uuId)
                                       .asList();
    if (!configFiles.isEmpty()) {
      String errorMessage = "Being used by ";
      for (ConfigFile configFile : configFiles) {
        errorMessage += configFile.getFileName() + ", ";
      }

      throw new KmsOperationException(errorMessage);
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
  public PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException {
    PageResponse<EncryptedData> pageResponse = wingsPersistence.query(EncryptedData.class, pageRequest);

    List<EncryptedData> encryptedDataList = pageResponse.getResponse();

    List<EncryptedData> filteredEncryptedDataList = Lists.newArrayList();

    RestrictionsAndAppEnvMap restrictionsAndAppEnvMap =
        usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(accountId, Action.READ);
    Map<String, Set<String>> appEnvMapFromPermissions = restrictionsAndAppEnvMap.getAppEnvMap();
    UsageRestrictions restrictionsFromUserPermissions = restrictionsAndAppEnvMap.getUsageRestrictions();

    for (EncryptedData encryptedData : encryptedDataList) {
      encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
      encryptedData.setEncryptionKey(SECRET_MASK);
      if (details) {
        encryptedData.setEncryptedBy(getSecretManagerName(encryptedData.getType(), encryptedData.getUuid(),
            encryptedData.getKmsId(), encryptedData.getEncryptionType()));

        encryptedData.setSetupUsage(getSecretUsage(encryptedData.getAccountId(), encryptedData.getUuid()).size());
        encryptedData.setRunTimeUsage(getUsageLogsSize(encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT));
        encryptedData.setChangeLog(
            getChangeLogs(encryptedData.getAccountId(), encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT)
                .size());
      }

      UsageRestrictions usageRestrictionsFromEntity = encryptedData.getUsageRestrictions();
      if (usageRestrictionsFromEntity == null) {
        filteredEncryptedDataList.add(encryptedData);
        continue;
      }

      // Observed some entities having empty usage restrictions. Covering that case.
      // Could have been due to a ui bug at some point.
      if (isEmpty(usageRestrictionsFromEntity.getAppEnvRestrictions())) {
        usageRestrictionsFromEntity.setEditable(true);
        filteredEncryptedDataList.add(encryptedData);
        continue;
      }

      Map<String, Set<String>> appEnvMapFromEntityRestrictions =
          usageRestrictionsService.getAppEnvMap(accountId, usageRestrictionsFromEntity.getAppEnvRestrictions());

      if (usageRestrictionsService.hasAccess(accountId, appIdFromRequest, envIdFromRequest, usageRestrictionsFromEntity,
              appEnvMapFromEntityRestrictions, restrictionsFromUserPermissions, appEnvMapFromPermissions)) {
        usageRestrictionsFromEntity.setEditable(usageRestrictionsService.userHasPermissionsToChangeEntity(
            encryptedData.getAccountId(), usageRestrictionsFromEntity, appEnvMapFromEntityRestrictions,
            restrictionsFromUserPermissions, appEnvMapFromPermissions));
        filteredEncryptedDataList.add(encryptedData);
      }
    }

    pageResponse.setResponse(filteredEncryptedDataList);
    pageResponse.setTotal(Long.valueOf(filteredEncryptedDataList.size()));
    return pageResponse;
  }

  @Override
  public List<UuidAware> getSecretUsage(String accountId, String secretTextId) {
    EncryptedData secretText = wingsPersistence.get(EncryptedData.class, secretTextId);
    Preconditions.checkNotNull(secretText, "could not find secret with id " + secretTextId);
    if (secretText.getParentIds() == null) {
      return Collections.emptyList();
    }

    SettingVariableTypes type = secretText.getType() == SettingVariableTypes.SECRET_TEXT
        ? SettingVariableTypes.SERVICE_VARIABLE
        : secretText.getType();
    Set<Parent> parents = new HashSet<>();
    for (String parentId : secretText.getParentIds()) {
      parents.add(Parent.builder()
                      .id(parentId)
                      .variableType(type)
                      .encryptionDetail(EncryptionDetail.builder()
                                            .encryptionType(secretText.getEncryptionType())
                                            .secretManagerName(getSecretManagerName(
                                                type, parentId, secretText.getKmsId(), secretText.getEncryptionType()))
                                            .build())
                      .build());
    }

    return fetchParents(accountId, parents);
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
            Field encryptedRefField = getEncryptedRefField(field, (EncryptableSetting) settingAttribute.getValue());
            encryptedRefField.setAccessible(true);
            secretIds.add((String) encryptedRefField.get(settingAttribute.getValue()));
          }
        }
    }
    return secretIds;
  }

  private List<UuidAware> fetchParents(String accountId, Set<Parent> parents) {
    TreeBasedTable<SettingVariableTypes, EncryptionDetail, List<Parent>> parentByTypes = TreeBasedTable.create();
    parents.forEach(parent -> {
      if (parentByTypes.get(parent.getVariableType(), parent.getEncryptionDetail()) == null) {
        parentByTypes.put(parent.getVariableType(), parent.getEncryptionDetail(), new ArrayList<>());
      }
      parentByTypes.get(parent.getVariableType(), parent.getEncryptionDetail()).add(parent);
    });

    List<UuidAware> rv = new ArrayList<>();
    parentByTypes.cellSet().forEach(cell -> {
      List<String> parentIds = cell.getValue().stream().map(parent -> parent.getId()).collect(Collectors.toList());
      switch (cell.getRowKey()) {
        case KMS:
          rv.add(kmsService.getSecretConfig(accountId));
          break;
        case SERVICE_VARIABLE:
          List<ServiceVariable> serviceVariables = serviceVariableService
                                                       .list(aPageRequest()
                                                                 .addFilter("_id", Operator.IN, parentIds.toArray())
                                                                 .addFilter("accountId", Operator.EQ, accountId)
                                                                 .build())
                                                       .getResponse();
          serviceVariables.forEach(serviceVariable -> {
            serviceVariable.setValue(SECRET_MASK.toCharArray());
            if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
              ServiceTemplate serviceTemplate =
                  wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
              Preconditions.checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
              serviceVariable.setServiceId(serviceTemplate.getServiceId());
            }
            serviceVariable.setEncryptionType(cell.getColumnKey().getEncryptionType());
            serviceVariable.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(serviceVariables);
          break;

        case CONFIG_FILE:
          List<ConfigFile> configFiles = configService
                                             .list(aPageRequest()
                                                       .addFilter("_id", Operator.IN, parentIds.toArray())
                                                       .addFilter("accountId", Operator.EQ, accountId)
                                                       .build())
                                             .getResponse();

          configFiles.forEach(configFile -> {
            if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
              ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, configFile.getEntityId());
              Preconditions.checkNotNull(serviceTemplate, "can't find service template " + configFile);
              configFile.setServiceId(serviceTemplate.getServiceId());
            }
            configFile.setEncryptionType(cell.getColumnKey().getEncryptionType());
            configFile.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(configFiles);
          break;

        case VAULT:
          List<VaultConfig> vaultConfigs = wingsPersistence.createQuery(VaultConfig.class)
                                               .field("_id")
                                               .in(parentIds)
                                               .field("accountId")
                                               .equal(accountId)
                                               .asList();
          vaultConfigs.forEach(vaultConfig -> {
            vaultConfig.setEncryptionType(cell.getColumnKey().getEncryptionType());
            vaultConfig.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(vaultConfigs);
          break;

        default:
          List<SettingAttribute> settingAttributes = settingsService
                                                         .list(aPageRequest()
                                                                   .addFilter("_id", Operator.IN, parentIds.toArray())
                                                                   .addFilter("accountId", Operator.EQ, accountId)
                                                                   .build(),
                                                             null, null)
                                                         .getResponse();
          settingAttributes.forEach(settingAttribute -> {
            settingAttribute.setEncryptionType(cell.getColumnKey().getEncryptionType());
            settingAttribute.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(settingAttributes);
          break;
      }
    });
    return rv;
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
        kmsService.encrypt(UUID.randomUUID().toString().toCharArray(), accountId, kmsConfig);
        alertService.closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      } catch (Exception e) {
        logger.info("Could not validate kms for account {} and kmsId {}", accountId, kmsConfig.getUuid(), e);
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
        logger.info("Could not validate vault for account {} and kmsId {}", accountId, vaultConfig.getUuid(), e);
        alertService.openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      }
    }
  }

  @Data
  @Builder
  @EqualsAndHashCode(exclude = {"encryptionDetail", "variableType"})
  private static class Parent {
    private String id;
    private EncryptionDetail encryptionDetail;
    private SettingVariableTypes variableType;
  }

  @Data
  @Builder
  private static class EncryptionDetail implements Comparable<EncryptionDetail> {
    private EncryptionType encryptionType;
    private String secretManagerName;

    @Override
    public int compareTo(EncryptionDetail o) {
      return this.encryptionType.compareTo(o.encryptionType);
    }
  }
}
