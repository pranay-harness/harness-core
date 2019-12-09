package software.wings.service.impl.security;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedFields;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;
import static io.harness.eraro.ErrorCode.CYBERARK_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.eraro.ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION;
import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeCount;
import static io.harness.security.encryption.EncryptionType.AWS_SECRETS_MANAGER;
import static io.harness.security.encryption.EncryptionType.AZURE_VAULT;
import static io.harness.security.encryption.EncryptionType.CYBERARK;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.security.encryption.EncryptionType.VAULT;
import static io.harness.validation.Validator.equalCheck;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.ServiceVariable.ENCRYPTED_VALUE_KEY;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.service.impl.security.AbstractSecretServiceImpl.checkNotNull;
import static software.wings.service.impl.security.AbstractSecretServiceImpl.checkState;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.service.intfc.security.VaultService.DEFAULT_BASE_PATH;
import static software.wings.service.intfc.security.VaultService.DEFAULT_KEY_NAME;
import static software.wings.service.intfc.security.VaultService.KEY_SPEARATOR;
import static software.wings.service.intfc.security.VaultService.PATH_SEPARATOR;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeBasedTable;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.HIterator;
import io.harness.persistence.UuidAware;
import io.harness.queue.QueuePublisher;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import io.harness.stream.BoundedInputStream;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigFileKeys;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretChangeLog.SecretChangeLogKeys;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;
import software.wings.utils.WingsReflectionUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by rsingh on 10/30/17.
 */
@Slf4j
public class SecretManagerImpl implements SecretManager {
  private static final String ILLEGAL_CHARACTERS = "[~!@#$%^&*'\"/?<>,;]";
  private static final String URL_ROOT_PREFIX = "//";
  // Prefix YAML ingestion generated secret names with this prefix
  private static final String YAML_PREFIX = "YAML_";

  private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private KmsService kmsService;
  @Inject private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject private GcpKmsService gcpKmsService;
  @Inject private VaultService vaultService;
  @Inject private AwsSecretsManagerService secretsManagerService;
  @Inject private AzureVaultService azureVaultService;
  @Inject private CyberArkService cyberArkService;
  @Inject private AlertService alertService;
  @Inject private FileService fileService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private SettingsService settingsService;
  @Inject private QueuePublisher<KmsTransitionEvent> transitionKmsQueue;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ConfigService configService;
  @Inject private AppService appService;
  @Inject private EnvironmentService envService;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private LocalEncryptionService localEncryptionService;
  @Inject private UserService userService;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private AzureSecretsManagerService azureSecretsManagerService;
  @Inject private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public EncryptionType getEncryptionType(String accountId) {
    return secretManagerConfigService.getEncryptionType(accountId);
  }

  @Override
  public List<SecretManagerConfig> listSecretManagers(String accountId) {
    return secretManagerConfigService.listSecretManagers(accountId, true);
  }

  @Override
  public EncryptedData encrypt(String accountId, SettingVariableTypes settingType, char[] secret, String path,
      EncryptedData encryptedData, String secretName, UsageRestrictions usageRestrictions) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Encrypting a secret");
      String toEncrypt = secret == null ? null : String.valueOf(secret);
      // Need to initialize an EncryptedData instance to carry the 'path' value for delegate to validate against.
      if (encryptedData == null) {
        encryptedData = EncryptedData.builder()
                            .name(secretName)
                            .path(path)
                            .accountId(accountId)
                            .type(settingType)
                            .enabled(true)
                            .parentIds(new HashSet<>())
                            .build();
      }

      String kmsId = encryptedData.getKmsId();
      EncryptionType encryptionType = encryptedData.getEncryptionType();
      EncryptionType accountEncryptionType = getEncryptionType(accountId);
      if (encryptionType == null
          || (encryptionType != accountEncryptionType && encryptionType == LOCAL
                 && accountEncryptionType != EncryptionType.CYBERARK)) {
        // PL-3160: 1. For new secrets, it always use account level encryption type to encrypt and save
        // 2. For existing secrets with LOCAL encryption, always use account level default secret manager to save if
        // default is not CYBERARK
        // 3. Else use the secrets' currently associated secret manager for update.
        encryptionType = accountEncryptionType;
        kmsId = null;
      }
      encryptedData.setEncryptionType(encryptionType);

      EncryptedData rv;
      switch (encryptionType) {
        case LOCAL:
          LocalEncryptionConfig localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
          rv = localEncryptionService.encrypt(secret, accountId, localEncryptionConfig);
          rv.setType(settingType);
          break;

        case KMS:
          final KmsConfig kmsConfig = (KmsConfig) getSecretManager(accountId, kmsId, KMS);
          rv = kmsService.encrypt(secret, accountId, kmsConfig);
          rv.setKmsId(kmsConfig.getUuid());
          break;

        case VAULT:
          final VaultConfig vaultConfig = (VaultConfig) getSecretManager(accountId, kmsId, VAULT);
          encryptedData.setKmsId(vaultConfig.getUuid());
          rv = vaultService.encrypt(secretName, toEncrypt, accountId, settingType, vaultConfig, encryptedData);
          rv.setKmsId(vaultConfig.getUuid());
          break;

        case AWS_SECRETS_MANAGER:
          final AwsSecretsManagerConfig secretsManagerConfig =
              (AwsSecretsManagerConfig) getSecretManager(accountId, kmsId, AWS_SECRETS_MANAGER);
          encryptedData.setKmsId(secretsManagerConfig.getUuid());
          rv = secretsManagerService.encrypt(
              secretName, toEncrypt, accountId, settingType, secretsManagerConfig, encryptedData);
          rv.setKmsId(secretsManagerConfig.getUuid());
          break;

        case GCP_KMS:
          final GcpKmsConfig gcpKmsConfig = (GcpKmsConfig) getSecretManager(accountId, kmsId, GCP_KMS);
          encryptedData.setKmsId(gcpKmsConfig.getUuid());
          rv = gcpKmsService.encrypt(toEncrypt, accountId, gcpKmsConfig, encryptedData);
          rv.setKmsId(gcpKmsConfig.getUuid());
          break;

        case AZURE_VAULT:
          final AzureVaultConfig azureConfig = (AzureVaultConfig) getSecretManager(accountId, kmsId, AZURE_VAULT);
          encryptedData.setKmsId(azureConfig.getUuid());
          rv = azureVaultService.encrypt(secretName, toEncrypt, accountId, settingType, azureConfig, encryptedData);
          rv.setKmsId(azureConfig.getUuid());
          break;

        case CYBERARK:
          final CyberArkConfig cyberArkConfig = (CyberArkConfig) getSecretManager(accountId, kmsId, CYBERARK);
          encryptedData.setKmsId(cyberArkConfig.getUuid());
          if (isNotEmpty(encryptedData.getPath())) {
            // CyberArk encrypt need to use decrypt of the secret reference as a way of validating the reference is
            // valid. If the  CyberArk reference is not valid, an exception will be throw.
            cyberArkService.decrypt(encryptedData, accountId, cyberArkConfig);
          } else {
            KmsConfig fallbackKmsConfig = kmsService.getGlobalKmsConfig();
            if (fallbackKmsConfig != null) {
              logger.info(
                  "CyberArk doesn't support creating new secret. This new secret text will be created in the global KMS SecretStore instead");
              rv = kmsService.encrypt(secret, accountId, fallbackKmsConfig);
              rv.setEncryptionType(KMS);
              rv.setKmsId(fallbackKmsConfig.getUuid());
            } else {
              logger.info(
                  "CyberArk doesn't support creating new secret. This new secret text will be created in the local Harness SecretStore instead");
              localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
              rv = localEncryptionService.encrypt(secret, accountId, localEncryptionConfig);
              rv.setEncryptionType(LOCAL);
            }
            rv.setName(secretName);
            rv.setType(settingType);
            rv.setUsageRestrictions(usageRestrictions);
            return rv;
          }
          rv = encryptedData;
          break;

        default:
          throw new IllegalStateException("Invalid type:  " + encryptionType);
      }
      rv.setName(secretName);
      rv.setEncryptionType(encryptionType);
      rv.setType(settingType);
      rv.setUsageRestrictions(usageRestrictions);
      return rv;
    }
  }

  @Override
  public SecretManagerConfig getSecretManager(String accountId, String kmsId, EncryptionType encryptionType) {
    if (encryptionType == LOCAL) {
      return localEncryptionService.getEncryptionConfig(accountId);
    } else {
      return isEmpty(kmsId) ? secretManagerConfigService.getDefaultSecretManager(accountId)
                            : secretManagerConfigService.getSecretManager(accountId, kmsId);
    }
  }

  public String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Encrypting a secret");
      EncryptedData encryptedData = encrypt(accountId, SettingVariableTypes.APM_VERIFICATION, secret.toCharArray(),
          null, null, UUID.randomUUID().toString(), usageRestrictions);
      String recordId = wingsPersistence.save(encryptedData);
      generateAuditForEncryptedRecord(accountId, null, recordId);
      return recordId;
    }
  }

  public Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, refId);
    if (encryptedData == null) {
      logger.info("No encrypted record set for field {} for id: {}", fieldName, refId);
      return Optional.empty();
    }
    EncryptionConfig encryptionConfig =
        getSecretManager(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());

    return Optional.of(EncryptedDataDetail.builder()
                           .encryptedData(SecretManager.buildRecordData(encryptedData))
                           .encryptionConfig(encryptionConfig)
                           .fieldName(fieldName)
                           .build());
  }

  public List<EncryptedDataDetail> getEncryptionDetails(EncryptableSetting object) {
    return getEncryptionDetails(object, null, null);
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(
      EncryptableSetting object, String appId, String workflowExecutionId) {
    // NOTE: appId should not used anywhere in this method
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }

    List<Field> encryptedFields = object.getEncryptedFields();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        Object fieldValue = f.get(object);
        Field encryptedRefField = getEncryptedRefField(f, object);
        encryptedRefField.setAccessible(true);
        String encryptedRefFieldValue = (String) encryptedRefField.get(object);
        boolean isSetByYaml = WingsReflectionUtils.isSetByYaml(object, encryptedRefField);
        if (fieldValue != null && !isSetByYaml) {
          checkState(encryptedRefFieldValue == null, ENCRYPT_DECRYPT_ERROR,
              "both encrypted and non encrypted field set for " + object);
          encryptedDataDetails.add(EncryptedDataDetail.builder()
                                       .encryptedData(EncryptedRecordData.builder()
                                                          .encryptionType(LOCAL)
                                                          .encryptionKey(object.getAccountId())
                                                          .encryptedValue((char[]) fieldValue)
                                                          .build())
                                       .fieldName(f.getName())
                                       .build());
        } else if (encryptedRefFieldValue != null) {
          // PL-2902: Avoid decryption of null value encrypted fields.
          String id = encryptedRefFieldValue;
          if (isSetByYaml) {
            id = id.substring(id.indexOf(':') + 1);
          }

          EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, id);
          if (encryptedData == null) {
            logger.info("No encrypted record set for field {} for id: {}", f.getName(), id);
            continue;
          }

          String accountId = object.getAccountId();
          EncryptionConfig encryptionConfig =
              getSecretManager(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());

          // PL-1836: Need to preprocess global KMS and turn the KMS encryption into a LOCAL encryption.
          final EncryptedRecordData encryptedRecordData;
          if (encryptionConfig.isGlobalKms()
              && featureFlagService.isEnabled(FeatureName.GLOBAL_KMS_PRE_PROCESSING, object.getAccountId())) {
            logger.info("Pre-processing the encrypted secret by global KMS secret manager for secret {}",
                encryptedData.getUuid());

            encryptedRecordData = kmsEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(
                encryptedData, (KmsConfig) encryptionConfig);

            // The encryption type will be set to LOCAL only if manager was able to decrypt.
            // If the decryption failed, we need to retain the kms encryption config, otherwise delegate task would
            // fail.
            if (encryptedRecordData.getEncryptionType().equals(LOCAL)) {
              encryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
              logger.info("Replaced it with LOCAL encryption for secret {}", encryptedData.getUuid());
            }
          } else {
            encryptedRecordData = SecretManager.buildRecordData(encryptedData);
          }

          EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder()
                                                        .encryptedData(encryptedRecordData)
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
              usageLog.setAppId(workflowExecution.getAppId());
              wingsPersistence.save(usageLog);
            }
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, e, USER);
    }

    return encryptedDataDetails;
  }

  @Override
  public void maskEncryptedFields(EncryptableSetting object) {
    List<Field> encryptedFields = object.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        f.set(object, ENCRYPTED_FIELD_MASK.toCharArray());
      }
    } catch (IllegalAccessException e) {
      throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, e, USER);
    }
  }

  @Override
  public void resetUnchangedEncryptedFields(EncryptableSetting sourceObject, EncryptableSetting destinationObject) {
    equalCheck(sourceObject.getClass().getName(), destinationObject.getClass().getName());

    List<Field> encryptedFields = sourceObject.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        if (java.util.Arrays.equals((char[]) f.get(destinationObject), ENCRYPTED_FIELD_MASK.toCharArray())) {
          f.set(destinationObject, f.get(sourceObject));
        }
      }
    } catch (IllegalAccessException e) {
      throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, e, USER);
    }
  }

  @Override
  public PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId,
      String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(accountId, Lists.newArrayList(entityId), variableType);
    // PL-3298: Some setting attribute doesn't have encrypted fields and therefore no secret Ids associated with it.
    // E.g. PHYSICAL_DATA_CENTER config. An empty response will be returned.
    if (isEmpty(secretIds)) {
      return new PageResponse<>(pageRequest);
    }

    pageRequest.addFilter(SecretChangeLogKeys.encryptedDataId, Operator.IN, secretIds.toArray());
    pageRequest.addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId);
    PageResponse<SecretUsageLog> response = wingsPersistence.query(SecretUsageLog.class, pageRequest);
    response.getResponse().forEach(secretUsageLog -> {
      if (isNotEmpty(secretUsageLog.getWorkflowExecutionId())) {
        WorkflowExecution workflowExecution =
            wingsPersistence.get(WorkflowExecution.class, secretUsageLog.getWorkflowExecutionId());
        if (workflowExecution != null) {
          secretUsageLog.setWorkflowExecutionName(workflowExecution.normalizedName());
        }
      }
    });
    return response;
  }

  private Map<String, Long> getUsageLogSizes(
      String accountId, Collection<String> entityIds, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(accountId, entityIds, variableType);
    Query<SecretUsageLog> query = wingsPersistence.createQuery(SecretUsageLog.class)
                                      .filter(ACCOUNT_ID_KEY, accountId)
                                      .field(SecretChangeLogKeys.encryptedDataId)
                                      .in(secretIds);

    final AggregationPipeline aggregationPipeline =
        wingsPersistence.getDatastore(SecretUsageLog.class)
            .createAggregation(SecretUsageLog.class)
            .match(query)
            .group(SecretChangeLogKeys.encryptedDataId, grouping("count", new Accumulator("$sum", 1)))
            .project(projection(SecretChangeLogKeys.encryptedDataId, ID_KEY), projection("count"));

    List<SecretUsageSummary> secretUsageSummaries = new ArrayList<>();
    aggregationPipeline.aggregate(SecretUsageSummary.class).forEachRemaining(secretUsageSummaries::add);

    return secretUsageSummaries.stream().collect(
        Collectors.toMap(summary -> summary.encryptedDataId, summary -> summary.count));
  }

  @Override
  public List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    EncryptedData encryptedData = null;
    if (variableType == SettingVariableTypes.SECRET_TEXT) {
      encryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    }

    return getChangeLogsInternal(accountId, entityId, encryptedData, variableType);
  }

  private List<SecretChangeLog> getChangeLogsInternal(String accountId, String entityId, EncryptedData encryptedData,
      SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(accountId, Lists.newArrayList(entityId), variableType);
    List<SecretChangeLog> secretChangeLogs = wingsPersistence.createQuery(SecretChangeLog.class, excludeCount)
                                                 .filter(ACCOUNT_ID_KEY, accountId)
                                                 .field(SecretChangeLogKeys.encryptedDataId)
                                                 .hasAnyOf(secretIds)
                                                 .order("-" + CREATED_AT_KEY)
                                                 .asList();

    // HAR-7150: Retrieve version history/changelog from Vault if secret text is a path reference.
    if (variableType == SettingVariableTypes.SECRET_TEXT && encryptedData != null) {
      EncryptionType encryptionType = encryptedData.getEncryptionType();
      if (encryptionType == EncryptionType.VAULT && isNotEmpty(encryptedData.getPath())) {
        VaultConfig vaultConfig =
            (VaultConfig) getSecretManager(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());
        secretChangeLogs.addAll(vaultService.getVaultSecretChangeLogs(encryptedData, vaultConfig));
        // Sort the change log by change time in descending order.
        secretChangeLogs.sort(
            (SecretChangeLog o1, SecretChangeLog o2) -> (int) (o2.getLastUpdatedAt() - o1.getLastUpdatedAt()));
      }
    }

    return secretChangeLogs;
  }

  private Map<String, Long> getChangeLogSizes(
      String accountId, Collection<String> entityIds, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(accountId, entityIds, variableType);
    Query<SecretChangeLog> query = wingsPersistence.createQuery(SecretChangeLog.class)
                                       .filter(ACCOUNT_ID_KEY, accountId)
                                       .field(SecretChangeLogKeys.encryptedDataId)
                                       .in(secretIds);

    final AggregationPipeline aggregationPipeline =
        wingsPersistence.getDatastore(SecretChangeLog.class)
            .createAggregation(SecretChangeLog.class)
            .match(query)
            .group(SecretChangeLogKeys.encryptedDataId, grouping("count", new Accumulator("$sum", 1)))
            .project(projection(SecretChangeLogKeys.encryptedDataId, ID_KEY), projection("count"));

    List<ChangeLogSummary> changeLogSummaries = new ArrayList<>();
    aggregationPipeline.aggregate(ChangeLogSummary.class).forEachRemaining(changeLogSummaries::add);

    return changeLogSummaries.stream().collect(
        Collectors.toMap(summary -> summary.encryptedDataId, summary -> summary.count));
  }

  @Override
  public Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId) {
    return listEncryptedSettingAttributes(accountId,
        Sets.newHashSet(SettingCategory.CLOUD_PROVIDER.name(), SettingCategory.CONNECTOR.name(),
            SettingCategory.SETTING.name(), SettingCategory.HELM_REPO.name(), SettingCategory.AZURE_ARTIFACTS.name()));
  }

  @Override
  public Collection<SettingAttribute> listEncryptedSettingAttributes(String accountId, Set<String> categories) {
    // 1. Fetch all setting attributes belonging to the specified category or all settings categories.
    List<SettingAttribute> settingAttributeList = new ArrayList<>();
    Set<String> settingAttributeIds = new HashSet<>();

    // Exclude STRING type of setting attribute as they are never displayed in secret management section.
    Query<SettingAttribute> categoryQuery = wingsPersistence.createQuery(SettingAttribute.class)
                                                .filter(ACCOUNT_ID_KEY, accountId)
                                                .field(SettingAttributeKeys.category)
                                                .in(categories)
                                                .field(SettingAttribute.VALUE_TYPE_KEY)
                                                .notIn(Lists.newArrayList(SettingVariableTypes.STRING));
    loadSettingQueryResult(categoryQuery, settingAttributeIds, settingAttributeList);

    // If SETTING category is included, then make sure WINRM related settings get loaded as it's category field is
    // empty in persistence store and the filter need special handling.
    if (categories.contains(SettingCategory.SETTING.name())) {
      // PL-3318: Some WINRM connection attribute does not have category field set SHOULD be included in the result set.
      Query<SettingAttribute> winRmQuery =
          wingsPersistence.createQuery(SettingAttribute.class)
              .filter(ACCOUNT_ID_KEY, accountId)
              .field(SettingAttributeKeys.category)
              .doesNotExist()
              .field(SettingAttribute.VALUE_TYPE_KEY)
              .in(Lists.newArrayList(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES));
      loadSettingQueryResult(winRmQuery, settingAttributeIds, settingAttributeList);
    }

    // 2. Fetch children encrypted records associated with these setting attributes in a batch
    Map<String, EncryptedData> encryptedDataMap = new HashMap<>();
    try (HIterator<EncryptedData> query = new HIterator<>(wingsPersistence.createQuery(EncryptedData.class)
                                                              .filter(ACCOUNT_ID_KEY, accountId)
                                                              .field(EncryptedDataKeys.parentIds)
                                                              .in(settingAttributeIds)
                                                              .fetch())) {
      for (EncryptedData encryptedData : query) {
        for (String parentId : encryptedData.getParentIds()) {
          encryptedDataMap.put(parentId, encryptedData);
        }
      }
    }

    // 3. Set 'encryptionType' and 'encryptedBy' field of setting attributes based on children encrypted record
    // association
    Map<String, SecretManagerConfig> secretManagerConfigMap = getSecretManagerMap(accountId);
    // Create a new list, and only those setting attributes with encrypted record or in SETTING category will be
    // retained.
    List<SettingAttribute> finalList = new ArrayList<>();
    for (SettingAttribute settingAttribute : settingAttributeList) {
      EncryptedData encryptedData = encryptedDataMap.get(settingAttribute.getUuid());
      if (encryptedData != null) {
        settingAttribute.setEncryptionType(encryptedData.getEncryptionType());
        settingAttribute.setEncryptedBy(
            getSecretManagerName(encryptedData.getKmsId(), encryptedData.getEncryptionType(), secretManagerConfigMap));
        finalList.add(settingAttribute);
      } else if (settingAttribute.getCategory() == SettingCategory.SETTING) {
        finalList.add(settingAttribute);
      }
    }

    return finalList;
  }

  private void loadSettingQueryResult(
      Query<SettingAttribute> query, Set<String> settingAttributeIds, List<SettingAttribute> settingAttributeList) {
    try (HIterator<SettingAttribute> queryIter = new HIterator<>(query.fetch())) {
      for (SettingAttribute settingAttribute : queryIter) {
        settingAttributeList.add(settingAttribute);
        settingAttributeIds.add(settingAttribute.getUuid());
        if (settingAttribute.getValue() instanceof EncryptableSetting) {
          maskEncryptedFields((EncryptableSetting) settingAttribute.getValue());
        }
      }
    }
  }

  @Override
  public String getEncryptedYamlRef(EncryptableSetting object, String... fieldNames) throws IllegalAccessException {
    if (object.getSettingType() == SettingVariableTypes.CONFIG_FILE) {
      String encryptedFieldRefId = ((ConfigFile) object).getEncryptedFileId();
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
      checkNotNull(encryptedData, ENCRYPT_DECRYPT_ERROR, "No encrypted record found for " + object);
      if (encryptedData.getEncryptionType() == EncryptionType.VAULT) {
        return encryptedData.getEncryptionType().getYamlName() + ":" + getVaultSecretRefUrl(encryptedData);
      } else {
        return encryptedData.getEncryptionType().getYamlName() + ":" + encryptedFieldRefId;
      }
    }
    checkState(fieldNames.length <= 1, ENCRYPT_DECRYPT_ERROR, "Gan't give more than one field in the call");
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
    checkNotNull(encryptedField, ENCRYPT_DECRYPT_ERROR,
        "Encrypted field not found " + object + ", args:" + Joiner.on(",").join(fieldNames));

    encryptedField.setAccessible(true);

    Field encryptedFieldRef = getEncryptedRefField(encryptedField, object);
    encryptedFieldRef.setAccessible(true);
    String encryptedFieldRefId = (String) encryptedFieldRef.get(object);
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
    checkNotNull(encryptedData, ENCRYPT_DECRYPT_ERROR, "No encrypted record found for " + object);

    if (encryptedData.getEncryptionType() == EncryptionType.VAULT) {
      return encryptedData.getEncryptionType().getYamlName() + ":" + getVaultSecretRefUrl(encryptedData);
    } else {
      return encryptedData.getEncryptionType().getYamlName() + ":" + encryptedFieldRefId;
    }
  }

  private String getVaultSecretRefUrl(EncryptedData encryptedData) {
    VaultConfig vaultConfig = vaultService.getVaultConfig(encryptedData.getAccountId(), encryptedData.getKmsId());
    String basePath = vaultConfig.getBasePath() == null ? DEFAULT_BASE_PATH : vaultConfig.getBasePath();
    String vaultPath = isEmpty(encryptedData.getPath())
        ? basePath + "/" + encryptedData.getEncryptionKey() + KEY_SPEARATOR + DEFAULT_KEY_NAME
        : encryptedData.getPath();
    return URL_ROOT_PREFIX + vaultConfig.getName() + vaultPath;
  }

  @Override
  public EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef, String accountId) {
    checkState(isNotEmpty(encryptedYamlRef), ENCRYPT_DECRYPT_ERROR, "Null encrypted YAML reference");
    String[] tags = encryptedYamlRef.split(":");
    String encryptionTypeYamlName = tags[0];
    String encryptedDataRef = tags[1];

    EncryptedData encryptedData;
    if (EncryptionType.VAULT.getYamlName().equals(encryptionTypeYamlName)
        && encryptedDataRef.startsWith(URL_ROOT_PREFIX)) {
      if (!encryptedDataRef.contains(KEY_SPEARATOR)) {
        throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR,
            "No key name separator # found in the Vault secret reference " + encryptedDataRef, USER);
      }

      // This is a new Vault path based reference;
      ParsedVaultSecretRef vaultSecretRef = parse(encryptedDataRef, accountId);

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(ACCOUNT_ID_KEY)
          .equal(accountId)
          .criteria(EncryptedDataKeys.encryptionType)
          .equal(EncryptionType.VAULT);
      if (isNotEmpty(vaultSecretRef.relativePath)) {
        query.criteria(EncryptedDataKeys.encryptionKey).equal(vaultSecretRef.relativePath);
      } else if (isNotEmpty(vaultSecretRef.fullPath)) {
        query.criteria(EncryptedDataKeys.path).equal(vaultSecretRef.fullPath);
      }
      encryptedData = query.get();

      if (encryptedData == null) {
        encryptedData = createNewSecretTextFromVaultPathReference(vaultSecretRef, accountId);
      }
    } else {
      // This is an old id based reference
      encryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataRef);
    }
    return encryptedData;
  }

  private EncryptedData createNewSecretTextFromVaultPathReference(
      ParsedVaultSecretRef vaultSecretRef, String accountId) {
    String secretName = getEncryptedDataNameFromRef(vaultSecretRef.fullPath);
    SettingVariableTypes type = SettingVariableTypes.SECRET_TEXT;
    String encryptionKey = type + PATH_SEPARATOR + secretName;
    EncryptedData encryptedData = EncryptedData.builder()
                                      .name(secretName)
                                      .encryptionKey(encryptionKey)
                                      .encryptedValue(encryptionKey.toCharArray())
                                      .encryptionType(EncryptionType.VAULT)
                                      .type(type)
                                      .accountId(accountId)
                                      .kmsId(vaultSecretRef.vaultConfigId)
                                      .usageRestrictions(getDefaultUsageRestrictions())
                                      .build();

    if (vaultSecretRef.relativePath != null) {
      encryptedData.setEncryptionKey(vaultSecretRef.relativePath);
      encryptedData.setEncryptedValue(vaultSecretRef.relativePath.toCharArray());
    } else if (vaultSecretRef.fullPath != null) {
      encryptedData.setPath(vaultSecretRef.fullPath);
    }

    String encryptedDataId = wingsPersistence.save(encryptedData);
    generateAuditForEncryptedRecord(accountId, null, encryptedDataId);
    encryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    char[] decryptedValue = null;
    try {
      // To test if the encrypted Data path is valid.
      decryptedValue = vaultService.decrypt(encryptedData, accountId, vaultSecretRef.vaultConfig);
    } catch (Exception e) {
      logger.error("Failed to decrypted vault secret at path " + encryptedData.getPath(), e);
    }

    if (isNotEmpty(decryptedValue)) {
      logger.info("Created a vault path and key reference secret '{}' to refer to the Vault secret at {}", secretName,
          vaultSecretRef.fullPath);
    } else {
      // If invalid reference, delete the encrypted data instance.
      EncryptedData record = wingsPersistence.get(EncryptedData.class, encryptedDataId);
      if (record != null) {
        deleteAndReportForAuditRecord(accountId, record);
      }
      throw new SecretManagementException(
          ENCRYPT_DECRYPT_ERROR, "Vault path '" + vaultSecretRef.fullPath + "' is invalid", USER);
    }
    return encryptedData;
  }

  private UsageRestrictions getDefaultUsageRestrictions() {
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    EnvFilter envFilter = EnvFilter.builder().filterTypes(newHashSet(PROD, NON_PROD)).build();
    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    UsageRestrictions usageRestrictions = new UsageRestrictions();
    usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
    return usageRestrictions;
  }

  private ParsedVaultSecretRef parse(String encryptedDataRef, String accountId) {
    if (!encryptedDataRef.startsWith(URL_ROOT_PREFIX) || !encryptedDataRef.contains(KEY_SPEARATOR)) {
      throw new SecretManagementException(
          ENCRYPT_DECRYPT_ERROR, "Vault secret reference '" + encryptedDataRef + "' has illegal format", USER);
    } else {
      String secretMangerNameAndPath = encryptedDataRef.substring(2);

      int index = secretMangerNameAndPath.indexOf(PATH_SEPARATOR);
      String fullPath = secretMangerNameAndPath.substring(index);
      String secretManagerName = secretMangerNameAndPath.substring(0, index);
      VaultConfig vaultConfig = vaultService.getVaultConfigByName(accountId, secretManagerName);
      if (vaultConfig == null) {
        throw new SecretManagementException(
            ENCRYPT_DECRYPT_ERROR, "Vault secret manager '" + secretManagerName + "' doesn't exist", USER);
      }

      String basePath = vaultConfig.getBasePath() == null ? DEFAULT_BASE_PATH : vaultConfig.getBasePath();
      index = fullPath.indexOf(KEY_SPEARATOR);
      String keyName = fullPath.substring(index + 1);

      String vaultPath = null;
      if (fullPath.startsWith(basePath)) {
        vaultPath = fullPath.substring(basePath.length() + 1, index);
      }

      return ParsedVaultSecretRef.builder()
          .secretManagerName(secretManagerName)
          .vaultConfigId(vaultConfig.getUuid())
          .vaultConfig(vaultConfig)
          .basePath(basePath)
          .fullPath(fullPath)
          .relativePath(vaultPath)
          .keyName(keyName)
          .build();
    }
  }

  private String getEncryptedDataNameFromRef(String fullVaultPath) {
    return YAML_PREFIX + fullVaultPath.replaceAll(PATH_SEPARATOR, "_").replaceAll(KEY_SPEARATOR, "_");
  }

  @Override
  public boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretManagerId,
      EncryptionType toEncryptionType, String toSecretManagerId) {
    checkState(isNotEmpty(accountId), "accountId can't be blank");
    checkNotNull(fromEncryptionType, "fromEncryptionType can't be blank");
    checkState(isNotEmpty(fromSecretManagerId), "fromSecretManagerId can't be blank");
    checkNotNull(toEncryptionType, "toEncryptionType can't be blank");
    checkState(isNotEmpty(toSecretManagerId), "toSecretManagerId can't be blank");

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .filter(ACCOUNT_ID_KEY, accountId)
                                     .filter(EncryptedDataKeys.kmsId, fromSecretManagerId);

    if (toEncryptionType == EncryptionType.VAULT) {
      query = query.field(EncryptedDataKeys.type).notEqual(SettingVariableTypes.VAULT);
    }

    try (HIterator<EncryptedData> iterator = new HIterator<>(query.fetch())) {
      for (EncryptedData dataToTransition : iterator) {
        transitionKmsQueue.send(KmsTransitionEvent.builder()
                                    .accountId(accountId)
                                    .entityId(dataToTransition.getUuid())
                                    .fromEncryptionType(fromEncryptionType)
                                    .fromKmsId(fromSecretManagerId)
                                    .toEncryptionType(toEncryptionType)
                                    .toKmsId(toSecretManagerId)
                                    .build());
      }
    }
    return true;
  }

  @Override
  public void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType,
      String fromKmsId, EncryptionType toEncryptionType, String toKmsId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    // This is needed as encryptedData will be updated in the process of
    EncryptedData existingEncryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    checkNotNull(encryptedData, "No encrypted data with id " + entityId);
    checkState(encryptedData.getEncryptionType() == fromEncryptionType,
        "mismatch between saved encrypted type and from encryption type");
    EncryptionConfig fromConfig = getSecretManager(accountId, fromKmsId, fromEncryptionType);
    checkNotNull(
        fromConfig, "No kms found for account " + accountId + " with id " + entityId + " type: " + fromEncryptionType);
    EncryptionConfig toConfig = getSecretManager(accountId, toKmsId, toEncryptionType);
    checkNotNull(
        toConfig, "No kms found for account " + accountId + " with id " + entityId + " type: " + fromEncryptionType);

    // Can't not transition secrets with path reference to a different secret manager. Customer has to manually
    // transfer.
    if (isNotEmpty(encryptedData.getPath())) {
      logger.warn(
          "Encrypted secret '{}' with path '{}' in account '{}' is not allowed to be transferred to a different secret manager.",
          encryptedData.getName(), encryptedData.getPath(), accountId);
      return;
    }

    if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
      changeFileSecretManager(accountId, encryptedData, fromEncryptionType, fromConfig, toEncryptionType, toConfig);
      return;
    }

    // 1. Decrypt the secret from the Source secret manager
    char[] decrypted;
    switch (fromEncryptionType) {
      case LOCAL:
        decrypted = localEncryptionService.decrypt(encryptedData, accountId, (LocalEncryptionConfig) fromConfig);
        break;
      case KMS:
        decrypted = kmsService.decrypt(encryptedData, accountId, (KmsConfig) fromConfig);
        break;
      case GCP_KMS:
        decrypted = gcpKmsService.decrypt(encryptedData, accountId, (GcpKmsConfig) fromConfig);
        break;
      case VAULT:
        decrypted = vaultService.decrypt(encryptedData, accountId, (VaultConfig) fromConfig);
        break;
      case AWS_SECRETS_MANAGER:
        decrypted = secretsManagerService.decrypt(encryptedData, accountId, (AwsSecretsManagerConfig) fromConfig);
        break;
      case AZURE_VAULT:
        decrypted = azureVaultService.decrypt(encryptedData, accountId, (AzureVaultConfig) fromConfig);
        break;
      case CYBERARK:
        decrypted = cyberArkService.decrypt(encryptedData, accountId, (CyberArkConfig) fromConfig);
        break;
      default:
        throw new IllegalStateException("Invalid type : " + fromEncryptionType);
    }

    // 2. Create/encrypt the secrect into the target secret manager
    EncryptedData encrypted;
    String encryptionKey = encryptedData.getEncryptionKey();
    String secretValue = decrypted == null ? null : String.valueOf(decrypted);
    switch (toEncryptionType) {
      case LOCAL:
        encrypted = localEncryptionService.encrypt(decrypted, accountId, (LocalEncryptionConfig) toConfig);
        break;
      case KMS:
        encrypted = kmsService.encrypt(decrypted, accountId, (KmsConfig) toConfig);
        break;
      case GCP_KMS:
        String decryptedString = decrypted == null ? null : String.valueOf(decrypted);
        encrypted = gcpKmsService.encrypt(decryptedString, accountId, (GcpKmsConfig) toConfig,
            EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      case VAULT:
        encrypted = vaultService.encrypt(encryptedData.getName(), secretValue, accountId, encryptedData.getType(),
            (VaultConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      case AWS_SECRETS_MANAGER:
        encrypted =
            secretsManagerService.encrypt(encryptedData.getName(), secretValue, accountId, encryptedData.getType(),
                (AwsSecretsManagerConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      case AZURE_VAULT:
        encrypted = azureVaultService.encrypt(encryptedData.getName(), secretValue, accountId, encryptedData.getType(),
            (AzureVaultConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      case CYBERARK:
        throw new SecretManagementException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Deprecate operation is not supported for CyberArk secret manager", USER);
      default:
        throw new IllegalStateException("Invalid type : " + toEncryptionType);
    }

    // 3. Delete the secrets from secret engines once it's transitioned out to a new secret manager.
    // PL-3160: this applies to VAULT/AWS_SECRETS_MANAGER/AZURE_VAULT, But we should never delete the 'Referenced'
    // secrets.
    String secretName = null;
    switch (fromEncryptionType) {
      case VAULT:
        if (isEmpty(encryptedData.getPath())) {
          secretName = encryptionKey;
          vaultService.deleteSecret(accountId, secretName, (VaultConfig) fromConfig);
        }
        break;
      case AWS_SECRETS_MANAGER:
        if (isEmpty(encryptedData.getPath())) {
          secretName = encryptedData.getEncryptionKey();
          secretsManagerService.deleteSecret(accountId, secretName, (AwsSecretsManagerConfig) fromConfig);
        }
        break;
      case AZURE_VAULT:
        if (isEmpty(encryptedData.getPath())) {
          secretName = encryptedData.getEncryptionKey();
          azureVaultService.delete(accountId, secretName, (AzureVaultConfig) fromConfig);
        }
        break;
      default:
        // No operation for other secret manager types
        break;
    }
    if (secretName != null) {
      logger.info("Deleting secret name {} from secret manager {} of type {} in account {}", secretName,
          fromConfig.getUuid(), fromEncryptionType, accountId);
    }

    encryptedData.setKmsId(toKmsId);
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setEncryptionKey(encrypted.getEncryptionKey());
    encryptedData.setEncryptedValue(encrypted.getEncryptedValue());

    String recordId = wingsPersistence.save(encryptedData);
    generateAuditForEncryptedRecord(accountId, existingEncryptedData, recordId);
  }

  private void changeFileSecretManager(String accountId, EncryptedData encryptedData, EncryptionType fromEncryptionType,
      EncryptionConfig fromConfig, EncryptionType toEncryptionType, EncryptionConfig toConfig) {
    byte[] decryptedFileContent = getFileContents(accountId, encryptedData.getUuid());
    EncryptedData existingEncryptedRecord =
        isBlank(encryptedData.getUuid()) ? null : wingsPersistence.get(EncryptedData.class, encryptedData.getUuid());

    EncryptedData encryptedFileData;
    switch (toEncryptionType) {
      case LOCAL:
        encryptedFileData = localEncryptionService.encryptFile(
            accountId, (LocalEncryptionConfig) toConfig, encryptedData.getName(), decryptedFileContent);
        break;

      case KMS:
        encryptedFileData =
            kmsService.encryptFile(accountId, (KmsConfig) toConfig, encryptedData.getName(), decryptedFileContent);
        break;

      case GCP_KMS:
        encryptedFileData = gcpKmsService.encryptFile(
            accountId, (GcpKmsConfig) toConfig, encryptedData.getName(), decryptedFileContent, encryptedData);
        break;

      case VAULT:
        encryptedFileData = vaultService.encryptFile(
            accountId, (VaultConfig) toConfig, encryptedData.getName(), decryptedFileContent, encryptedData);
        break;

      case AWS_SECRETS_MANAGER:
        encryptedFileData = secretsManagerService.encryptFile(accountId, (AwsSecretsManagerConfig) toConfig,
            encryptedData.getName(), decryptedFileContent, encryptedData);
        break;

      case AZURE_VAULT:
        encryptedFileData = azureVaultService.encryptFile(
            accountId, (AzureVaultConfig) toConfig, encryptedData.getName(), decryptedFileContent, encryptedData);
        break;

      case CYBERARK:
        throw new SecretManagementException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Deprecate operation is not supported for CyberArk secret manager", USER);

      default:
        throw new IllegalArgumentException("Invalid target encryption type " + toEncryptionType);
    }

    String savedFileId = String.valueOf(encryptedData.getEncryptedValue());
    switch (fromEncryptionType) {
      case LOCAL:
        // Fall through so as the old file will be deleted just like in KMS case.
      case KMS:
        // Delete file from file service only if the source secret manager is of KMS type.
        fileService.deleteFile(savedFileId, CONFIGS);
        break;
      case GCP_KMS:
        fileService.deleteFile(savedFileId, CONFIGS);
        break;
      case VAULT:
        // Delete the Vault secret corresponding to the encrypted file if it's not a path reference
        if (isEmpty(encryptedData.getPath())) {
          vaultService.deleteSecret(accountId, encryptedData.getEncryptionKey(), (VaultConfig) fromConfig);
        }
        break;
      case AWS_SECRETS_MANAGER:
        // Delete the AWS secrets (encrypted file type) after it's transitioned out if it's not a referenced secret
        if (isEmpty(encryptedData.getPath())) {
          secretsManagerService.deleteSecret(
              accountId, encryptedData.getEncryptionKey(), (AwsSecretsManagerConfig) fromConfig);
        }
        break;
      case AZURE_VAULT:
        if (isEmpty(encryptedData.getPath())) {
          azureVaultService.delete(accountId, encryptedData.getEncryptionKey(), (AzureVaultConfig) fromConfig);
        }
        break;
      case CYBERARK:
        throw new SecretManagementException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Deprecate operation is not supported for CyberArk secret manager", USER);

      default:
        break;
    }

    encryptedData.setEncryptionKey(encryptedFileData.getEncryptionKey());
    encryptedData.setEncryptedValue(encryptedFileData.getEncryptedValue());
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setKmsId(toConfig.getUuid());
    encryptedData.setBase64Encoded(true);
    String recordId = wingsPersistence.save(encryptedData);
    generateAuditForEncryptedRecord(accountId, existingEncryptedRecord, recordId);
  }

  @Override
  public void renewVaultTokensAndValidateGlobalSecretManager() {
    Query<Account> query = wingsPersistence.createQuery(Account.class, excludeAuthority);
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      for (Account account : records) {
        try {
          vaultService.renewTokens(account.getUuid());
          vaultService.appRoleLogin(account.getUuid());
        } catch (Exception e) {
          logger.info("Failed to renew vault token for account id {}", account.getUuid(), e);
        }
      }
    }
    validateGlobalSecretManager();
  }

  @Override
  public EncryptedData getSecretMappedToAccountByName(String accountId, String name) {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .filter(ACCOUNT_ID_KEY, accountId)
                                     .filter(EncryptedDataKeys.name, name)
                                     .field(EncryptedDataKeys.usageRestrictions)
                                     .doesNotExist();
    return query.get();
  }

  @Override
  public EncryptedData getSecretMappedToAppByName(String accountId, String appId, String envId, String name) {
    PageRequest<EncryptedData> pageRequest = aPageRequest()
                                                 .addFilter(EncryptedDataKeys.name, Operator.EQ, name)
                                                 .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                                 .build();
    try {
      PageResponse<EncryptedData> response = listSecrets(accountId, pageRequest, appId, envId, false);
      List<EncryptedData> secrets = response.getResponse();
      return isNotEmpty(secrets) ? secrets.get(0) : null;
    } catch (Exception e) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Failed to list secrets", e, USER);
    }
  }

  @Override
  public EncryptedData getSecretById(String accountId, String id) {
    return wingsPersistence.createQuery(EncryptedData.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(EncryptedData.ID_KEY, id)
        .get();
  }

  @Override
  public EncryptedData getSecretByName(String accountId, String name) {
    return wingsPersistence.createQuery(EncryptedData.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(EncryptedDataKeys.name, name)
        .get();
  }

  @Override
  public String saveSecret(
      String accountId, String name, String value, String path, UsageRestrictions usageRestrictions) {
    return upsertSecretInternal(accountId, null, name, value, path, usageRestrictions);
  }

  @Override
  public List<String> importSecretsViaFile(String accountId, InputStream uploadStream) {
    List<SecretText> secretTexts = new ArrayList<>();

    InputStreamReader inputStreamReader = null;
    BufferedReader reader = null;
    try {
      inputStreamReader = new InputStreamReader(uploadStream, Charset.defaultCharset());
      reader = new BufferedReader(inputStreamReader);
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        String path = parts.length > 2 ? trim(parts[2]) : null;
        SecretText secretText = SecretText.builder().name(trim(parts[0])).value(trim(parts[1])).path(path).build();
        secretTexts.add(secretText);
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Error while importing secrets for accountId " + accountId, e);
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
        if (inputStreamReader != null) {
          inputStreamReader.close();
        }
      } catch (IOException e) {
        // Ignore.
      }
    }
    return importSecrets(accountId, secretTexts);
  }

  @Override
  public List<String> importSecrets(String accountId, List<SecretText> secretTexts) {
    List<String> secretIds = new ArrayList<>();
    for (SecretText secretText : secretTexts) {
      try {
        String secretId = upsertSecretInternal(accountId, null, secretText.getName(), secretText.getValue(),
            secretText.getPath(), secretText.getUsageRestrictions());
        secretIds.add(secretId);
        logger.info("Imported secret '{}' successfully with uid: {}", secretText.getName(), secretId);
      } catch (WingsException e) {
        logger.warn("Failed to save import secret '{}' with error: {}", secretText.getName(), e.getMessage());
      }
    }
    return secretIds;
  }

  @Override
  public String saveSecretUsingLocalMode(
      String accountId, String name, String value, String path, UsageRestrictions usageRestrictions) {
    return upsertSecretInternal(accountId, null, name, value, path, EncryptionType.LOCAL, usageRestrictions);
  }

  @Override
  public boolean transitionAllSecretsToHarnessSecretManager(String accountId) {
    // For now, the default/harness secret manager is the LOCAL secret manager, and it's HIDDEN from end-user
    EncryptionConfig harnessSecretManager = localEncryptionService.getEncryptionConfig(accountId);
    List<SecretManagerConfig> allEncryptionConfigs = listSecretManagers(accountId);
    for (EncryptionConfig encryptionConfig : allEncryptionConfigs) {
      logger.info("Transitioning secret from secret manager {} of type {} into Harness secret manager for account {}",
          encryptionConfig.getUuid(), encryptionConfig.getEncryptionType(), accountId);
      transitionSecrets(accountId, encryptionConfig.getEncryptionType(), encryptionConfig.getUuid(),
          harnessSecretManager.getEncryptionType(), harnessSecretManager.getUuid());
    }
    return true;
  }

  @Override
  public void clearDefaultFlagOfSecretManagers(String accountId) {
    // Set custom secret managers as non-default
    List<SecretManagerConfig> defaultSecretManagerConfigs =
        listSecretManagers(accountId).stream().filter(EncryptionConfig::isDefault).collect(Collectors.toList());

    for (EncryptionConfig config : defaultSecretManagerConfigs) {
      switch (config.getEncryptionType()) {
        case VAULT:
          VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, config.getUuid());
          vaultConfig.setDefault(false);
          vaultService.saveVaultConfig(accountId, vaultConfig);
          break;
        case LOCAL:
          break;
        case KMS:
          KmsConfig kmsConfig = kmsService.getKmsConfig(accountId, config.getUuid());
          if (!kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID) && kmsConfig.isDefault()) {
            kmsConfig.setDefault(false);
            kmsService.saveKmsConfig(accountId, kmsConfig);
          }
          break;
        case GCP_KMS:
          GcpKmsConfig gcpKmsConfig = gcpSecretsManagerService.getGcpKmsConfig(accountId, config.getUuid());
          if (!gcpKmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID) && gcpKmsConfig.isDefault()) {
            gcpKmsConfig.setDefault(false);
            gcpSecretsManagerService.saveGcpKmsConfig(accountId, gcpKmsConfig);
          }
          break;
        case AWS_SECRETS_MANAGER:
          AwsSecretsManagerConfig awsConfig =
              secretsManagerService.getAwsSecretsManagerConfig(accountId, config.getUuid());
          awsConfig.setDefault(false);
          secretsManagerService.saveAwsSecretsManagerConfig(accountId, awsConfig);
          break;
        case AZURE_VAULT:
          AzureVaultConfig azureConfig = azureSecretsManagerService.getEncryptionConfig(accountId, config.getUuid());
          azureConfig.setDefault(false);
          azureSecretsManagerService.saveAzureSecretsManagerConfig(accountId, azureConfig);
          break;
        case CYBERARK:
          CyberArkConfig cyberArkConfig = cyberArkService.getConfig(accountId, config.getUuid());
          cyberArkConfig.setDefault(false);
          cyberArkService.saveConfig(accountId, cyberArkConfig);
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + config.getEncryptionType());
      }
    }

    logger.info("Cleared default flag for secret managers in account {}.", accountId);
  }

  @Override
  public boolean updateSecret(
      String accountId, String uuId, String name, String value, String path, UsageRestrictions usageRestrictions) {
    String encryptedDataId =
        upsertSecretInternal(accountId, uuId, name, value, path, getEncryptionType(accountId), usageRestrictions);
    return encryptedDataId != null;
  }

  private String upsertSecretInternal(
      String accountId, String uuid, String name, String value, String path, UsageRestrictions usageRestrictions) {
    return upsertSecretInternal(accountId, uuid, name, value, path, null, usageRestrictions);
  }

  /**
   * This API is to combine multiple secret operations such as INSERT/UPDATE/UPSERT.
   * <p>
   * If 'uuid' passed in is null, this is an INSERT operation. If 'upsert' flag is true, it's an UPSERT operation it
   * will UPDATE if the record already exists, and INSERT if it doesn't exists.
   * <p>
   * It will return the generated UUID in a INSERT operation, and return null in the UPDATE operation if the record
   * doesn't exist.
   */
  private String upsertSecretInternal(String accountId, String uuid, String name, String value, String path,
      EncryptionType encryptionType, UsageRestrictions usageRestrictions) {
    String auditMessage;
    String encryptedDataId;

    if (containsIllegalCharacters(name)) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Secret name '" + name + "' contains illegal characters", USER);
    }

    char[] secretValue = isEmpty(value) ? null : value.toCharArray();
    if (isEmpty(uuid)) {
      // INSERT use case
      usageRestrictionsService.validateUsageRestrictionsOnEntitySave(accountId, usageRestrictions);

      if (encryptionType == null) {
        encryptionType = getEncryptionType(accountId);
      }
      validateSecretPath(encryptionType, path);

      EncryptedData encryptedData =
          encrypt(accountId, SettingVariableTypes.SECRET_TEXT, secretValue, path, null, name, usageRestrictions);
      encryptedData.addSearchTag(name);
      try {
        encryptedDataId = wingsPersistence.save(encryptedData);
        generateAuditForEncryptedRecord(accountId, null, encryptedDataId);
      } catch (DuplicateKeyException e) {
        String reason = "Variable " + name + " already exists";
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, reason, e, USER);
      }

      auditMessage = "Created";
    } else {
      // UPDATE use case
      EncryptedData savedData = wingsPersistence.get(EncryptedData.class, uuid);
      if (savedData == null) {
        // UPDATE use case. Return directly when record doesn't exist.
        return null;
      }

      // PL-3160: Make sure update/edit of existing secret to stick with the currently associated secret manager
      // Not switching to the current default secret manager.
      encryptionType = savedData.getEncryptionType();
      validateSecretPath(encryptionType, path);

      // savedData will be updated and saved again as a part of update, so need this oldEntity
      EncryptedData oldEntity = KryoUtils.clone(savedData);

      // validate usage restriction.
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
          accountId, savedData.getUsageRestrictions(), usageRestrictions);
      if (!Objects.equals(savedData.getUsageRestrictions(), usageRestrictions)) {
        // Validate if change of the usage scope is resulting in with dangling references in service/environments.
        validateAppEnvChangesInUsageRestrictions(savedData, usageRestrictions);
      }

      encryptedDataId = uuid;
      boolean nameChanged = !Objects.equals(name, savedData.getName());
      boolean valueChanged = isNotEmpty(value) && !value.equals(SECRET_MASK);
      boolean pathChanged = !Objects.equals(path, savedData.getPath());

      boolean needReencryption = false;

      StringBuilder builder = new StringBuilder();
      if (nameChanged) {
        builder.append("Changed name");
        savedData.removeSearchTag(null, savedData.getName(), null);
        savedData.setName(name);
        savedData.addSearchTag(name);

        // PL-1125: Remove old secret name in Vault if secret text's name changed to not have dangling entries.
        if (isEmpty(savedData.getPath())) {
          // For harness managed secrets, we need to delete the corresponding entries in the secret manager.
          String secretName = savedData.getEncryptionKey();
          switch (savedData.getEncryptionType()) {
            case VAULT:
              needReencryption = true;
              VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, savedData.getKmsId());
              if (!valueChanged) {
                // retrieve/decrypt the old secret value.
                secretValue = vaultService.decrypt(savedData, accountId, vaultConfig);
              }
              vaultService.deleteSecret(accountId, secretName, vaultConfig);
              break;
            case AWS_SECRETS_MANAGER:
              needReencryption = true;
              AwsSecretsManagerConfig secretsManagerConfig =
                  secretsManagerService.getAwsSecretsManagerConfig(accountId, savedData.getKmsId());
              if (!valueChanged) {
                // retrieve/decrypt the old secret value.
                secretValue = secretsManagerService.decrypt(savedData, accountId, secretsManagerConfig);
              }
              secretsManagerService.deleteSecret(accountId, secretName, secretsManagerConfig);
              break;
            case AZURE_VAULT:
              needReencryption = true;
              AzureVaultConfig azureVaultConfig =
                  azureSecretsManagerService.getEncryptionConfig(accountId, savedData.getKmsId());
              if (!valueChanged) {
                // retrieve/decrypt the old secret value.
                secretValue = azureVaultService.decrypt(savedData, accountId, azureVaultConfig);
              }
              azureVaultService.delete(accountId, secretName, azureVaultConfig);
              break;
            default:
              // Not relevant for other secret manager types
              break;
          }
        }
      }
      if (valueChanged) {
        needReencryption = true;
        builder.append(builder.length() > 0 ? " & value" : " Changed value");
      }
      if (pathChanged) {
        needReencryption = true;
        builder.append(builder.length() > 0 ? " & path" : " Changed path");
        savedData.setPath(path);
      }
      if (usageRestrictions != null) {
        builder.append(builder.length() > 0 ? " & usage restrictions" : "Changed usage restrictions");
      }
      auditMessage = builder.toString();

      // Re-encrypt if secret value or path has changed. Update should not change the existing Encryption type and
      // secret manager if the secret is 'path' enabled!
      if (needReencryption) {
        EncryptedData encryptedData =
            encrypt(accountId, SettingVariableTypes.SECRET_TEXT, secretValue, path, savedData, name, usageRestrictions);
        savedData.setEncryptionKey(encryptedData.getEncryptionKey());
        savedData.setEncryptedValue(encryptedData.getEncryptedValue());
        savedData.setEncryptionType(encryptedData.getEncryptionType());
        savedData.setKmsId(encryptedData.getKmsId());
      }
      savedData.setUsageRestrictions(usageRestrictions);
      wingsPersistence.save(savedData);
      if (eligibleForCrudAudit(savedData)) {
        auditServiceHelper.reportForAuditingUsingAccountId(savedData.getAccountId(), oldEntity, savedData, Type.UPDATE);
      }
    }

    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(encryptedDataId)
                                .description(auditMessage)
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }

    return encryptedDataId;
  }

  private void validateSecretPath(EncryptionType encryptionType, String path) {
    if (isNotEmpty(path)) {
      switch (encryptionType) {
        case VAULT:
          // Path should always have a "#" in and a key name after the #.
          if (path.indexOf('#') < 0) {
            throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
                "Secret path need to include the # sign with the the key name after. E.g. /foo/bar/my-secret#my-key.",
                USER);
          }
          break;
        case AWS_SECRETS_MANAGER:
          break;
        case CYBERARK:
          break;
        default:
          throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
              "Secret path can be specified only if the secret manager is of VAULT/AWS_SECRETS_MANAGER/CYBERARK type!",
              USER);
      }
    }
  }

  private boolean eligibleForCrudAudit(EncryptedData savedData) {
    return SettingVariableTypes.CONFIG_FILE.equals(savedData.getType())
        || SettingVariableTypes.SECRET_TEXT.equals(savedData.getType());
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
    // No validation of `validateAppEnvChangesInUsageRestrictions` is performed in this method
    // because usually this update is a result of removing application/environment.

    savedData.setUsageRestrictions(usageRestrictions);

    try {
      wingsPersistence.save(savedData);
    } catch (DuplicateKeyException e) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to save Restrictions", USER);
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
                                                 .filter(ACCOUNT_ID_KEY, accountId)
                                                 .filter(ENCRYPTED_VALUE_KEY, uuId)
                                                 .asList();
    if (!serviceVariables.isEmpty()) {
      String reason = "Can't delete this secret because it is still being used in service variable(s): "
          + serviceVariables.stream().map(ServiceVariable::getName).collect(joining(", "))
          + ". Please remove the usages of this secret and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, reason, USER);
    }

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, encryptedData.getUsageRestrictions())) {
      throw new SecretManagementException(USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, USER);
    }

    EncryptionType encryptionType = encryptedData.getEncryptionType();
    switch (encryptionType) {
      case AWS_SECRETS_MANAGER:
        if (isEmpty(encryptedData.getPath())) {
          // For harness managed secrets, we need to delete the corresponding entries in the Vault service.
          String keyName = encryptedData.getEncryptionKey();
          AwsSecretsManagerConfig secretsManagerConfig =
              secretsManagerService.getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId());
          secretsManagerService.deleteSecret(accountId, keyName, secretsManagerConfig);
        }
        break;
      case VAULT:
        if (isEmpty(encryptedData.getPath())) {
          // For harness managed secrets, we need to delete the corresponding entries in the Vault service.
          String keyName = encryptedData.getEncryptionKey();
          VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, encryptedData.getKmsId());
          vaultService.deleteSecret(accountId, keyName, vaultConfig);
        }
        break;
      case AZURE_VAULT:
        String keyName = encryptedData.getEncryptionKey();
        AzureVaultConfig encryptionConfig =
            azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId());
        azureVaultService.delete(accountId, keyName, encryptionConfig);
        break;
      default:
        break;
    }

    return deleteAndReportForAuditRecord(accountId, encryptedData);
  }

  @Override
  public boolean deleteSecretUsingUuid(String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    return deleteAndReportForAuditRecord(encryptedData.getAccountId(), encryptedData);
  }

  @Override
  public String saveFile(String accountId, String name, long fileSize, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream) {
    return upsertFileInternal(accountId, name, null, fileSize, usageRestrictions, inputStream);
  }

  @Override
  public File getFile(String accountId, String uuid, File readInto) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    checkNotNull(encryptedData, "could not find file with id " + uuid);
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    switch (encryptionType) {
      case LOCAL:
        return localEncryptionService.decryptFile(readInto, accountId, encryptedData);

      case KMS:
        fileService.download(String.valueOf(encryptedData.getEncryptedValue()), readInto, CONFIGS);
        return kmsService.decryptFile(readInto, accountId, encryptedData);

      case GCP_KMS:
        fileService.download(String.valueOf(encryptedData.getEncryptedValue()), readInto, CONFIGS);
        return gcpKmsService.decryptFile(readInto, accountId, encryptedData);

      case VAULT:
        return vaultService.decryptFile(readInto, accountId, encryptedData);

      case AWS_SECRETS_MANAGER:
        return secretsManagerService.decryptFile(readInto, accountId, encryptedData);

      case AZURE_VAULT:
        return azureVaultService.decryptFile(readInto, accountId, encryptedData);

      case CYBERARK:
        throw new SecretManagementException(
            CYBERARK_OPERATION_ERROR, "Encrypted file operations are not supported for CyberArk secret manager", USER);

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }
  }

  @Override
  public byte[] getFileContents(String accountId, String uuid) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    checkNotNull(encryptedData, "could not find file with id " + uuid);
    return getFileContents(accountId, encryptedData);
  }

  private byte[] getFileContents(String accountId, EncryptedData encryptedData) {
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    File file = null;
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      switch (encryptionType) {
        case LOCAL:
          localEncryptionService.decryptToStream(accountId, encryptedData, output);
          break;

        case KMS: {
          file = new File(Files.createTempDir(), generateUuid());
          logger.info("Temp file path [{}]", file.getAbsolutePath());
          fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
          kmsService.decryptToStream(file, accountId, encryptedData, output);
          break;
        }

        case GCP_KMS: {
          file = new File(Files.createTempDir(), generateUuid());
          logger.info("Temp file path [{}]", file.getAbsolutePath());
          fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
          gcpKmsService.decryptToStream(file, accountId, encryptedData, output);
          break;
        }

        case VAULT:
          vaultService.decryptToStream(accountId, encryptedData, output);
          break;

        case AWS_SECRETS_MANAGER:
          secretsManagerService.decryptToStream(accountId, encryptedData, output);
          break;

        case AZURE_VAULT:
          azureVaultService.decryptToStream(accountId, encryptedData, output);
          break;

        case CYBERARK:
          throw new SecretManagementException(CYBERARK_OPERATION_ERROR,
              "Encrypted file operations are not supported for CyberArk secret manager", USER);

        default:
          throw new IllegalArgumentException("Invalid type " + encryptionType);
      }
      output.flush();
      return output.toByteArray();
    } catch (IOException e) {
      throw new SecretManagementException(INVALID_ARGUMENT, "Failed to get content", e, USER);
    } finally {
      // Delete temporary file if it exists.
      if (file != null && file.exists()) {
        boolean deleted = file.delete();
        if (!deleted) {
          logger.warn("Temporary file {} can't be deleted.", file.getAbsolutePath());
        }
      }
    }
  }

  @Override
  public boolean updateFile(String accountId, String name, String uuid, long fileSize,
      UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    String recordId = upsertFileInternal(accountId, name, uuid, fileSize, usageRestrictions, inputStream);
    return isNotEmpty(recordId);
  }

  /**
   * This internal method should be able to handle the UPSERT of encrypted record. It will UPDATE the existing record if
   * the uuid exists, and INSERT if this record is new. The refactoring of this method helps taking care of the IMPORT
   * use case in which we would like to preserve the 'uuid' field while importing the exported encrypted keys from other
   * system.
   */
  private String upsertFileInternal(String accountId, String name, String uuid, long fileSize,
      UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    if (isEmpty(name)) {
      throw new SecretManagementException(ErrorCode.FILE_INTEGRITY_CHECK_FAILED, null, USER, null);
    }

    if (inputStream.getSize() > 0 && fileSize > inputStream.getSize()) {
      final Map<String, String> params = new HashMap<>();
      params.put("size", inputStream.getSize() / (1024 * 1024) + " MB");
      throw new SecretManagementException(
          ErrorCode.FILE_SIZE_EXCEEDS_LIMIT, null, USER, Collections.unmodifiableMap(params));
    }

    boolean update = false;
    String oldName = null;
    String savedFileId = null;
    EncryptedData encryptedData = null;
    EncryptedData oldEntityData = null;
    final EncryptionType encryptionType;

    if (containsIllegalCharacters(name)) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Encrypted file name '" + name + "' contains illegal characters", USER);
    }

    if (isNotEmpty(uuid)) {
      encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
      if (encryptedData == null) {
        // Pure UPDATE case, need to throw exception is the record doesn't exist.
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "could not find file with id " + uuid, USER);
      }

      // This is needed for auditing as encryptedData will be changed in the process of update
      oldEntityData = KryoUtils.clone(encryptedData);
    }

    if (encryptedData == null) {
      // INSERT in UPSERT case, get the system default encryption type.
      encryptionType = getEncryptionType(accountId);
      usageRestrictionsService.validateUsageRestrictionsOnEntitySave(accountId, usageRestrictions);
    } else {
      // UPDATE in UPSERT case
      update = true;
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
          accountId, encryptedData.getUsageRestrictions(), usageRestrictions);
      if (!Objects.equals(encryptedData.getUsageRestrictions(), usageRestrictions)) {
        // Validate if change of the usage scope is resulting in with dangling references in service/environments.
        validateAppEnvChangesInUsageRestrictions(encryptedData, usageRestrictions);
      }

      oldName = encryptedData.getName();
      savedFileId = String.valueOf(encryptedData.getEncryptedValue());
      encryptionType = encryptedData.getEncryptionType();
    }

    byte[] inputBytes;
    try {
      inputBytes = ByteStreams.toByteArray(inputStream);
    } catch (IOException e) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, e, USER);
    }

    String kmsId = update ? encryptedData.getKmsId() : null;
    EncryptionConfig encryptionConfig;
    EncryptedData newEncryptedFile = null;
    // HAR-9736: Update of encrypted file may not pick a new file for upload and no need to encrypt empty file.
    if (isNotEmpty(inputBytes)) {
      switch (encryptionType) {
        case LOCAL:
          LocalEncryptionConfig localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
          newEncryptedFile = localEncryptionService.encryptFile(accountId, localEncryptionConfig, name, inputBytes);
          if (update) {
            fileService.deleteFile(savedFileId, CONFIGS);
          }
          break;

        case KMS:
          encryptionConfig = getSecretManager(accountId, kmsId, KMS);
          KmsConfig kmsConfig = (KmsConfig) encryptionConfig;
          newEncryptedFile = kmsService.encryptFile(accountId, kmsConfig, name, inputBytes);
          newEncryptedFile.setKmsId(kmsConfig.getUuid());
          if (update) {
            fileService.deleteFile(savedFileId, CONFIGS);
          }
          break;

        case GCP_KMS:
          encryptionConfig = getSecretManager(accountId, kmsId, KMS);
          GcpKmsConfig gcpKmsConfig = (GcpKmsConfig) encryptionConfig;
          newEncryptedFile = gcpKmsService.encryptFile(accountId, gcpKmsConfig, name, inputBytes, encryptedData);
          newEncryptedFile.setKmsId(gcpKmsConfig.getUuid());
          if (update) {
            fileService.deleteFile(savedFileId, CONFIGS);
          }
          break;

        case VAULT:
          encryptionConfig = getSecretManager(accountId, kmsId, VAULT);
          VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
          newEncryptedFile = vaultService.encryptFile(accountId, vaultConfig, name, inputBytes, encryptedData);
          newEncryptedFile.setKmsId(vaultConfig.getUuid());
          break;

        case AWS_SECRETS_MANAGER:
          encryptionConfig = getSecretManager(accountId, kmsId, AWS_SECRETS_MANAGER);
          AwsSecretsManagerConfig secretsManagerConfig = (AwsSecretsManagerConfig) encryptionConfig;
          newEncryptedFile =
              secretsManagerService.encryptFile(accountId, secretsManagerConfig, name, inputBytes, encryptedData);
          newEncryptedFile.setKmsId(secretsManagerConfig.getUuid());
          break;

        case AZURE_VAULT:
          // if it's an update call, we need to update the secret value in the same secret store.
          // Otherwise it should be saved in the default secret store of the account.
          encryptionConfig = getSecretManager(accountId, kmsId, AZURE_VAULT);
          AzureVaultConfig azureConfig = (AzureVaultConfig) encryptionConfig;
          logger.info("Creating file in azure vault with secret name: {}, in vault: {}, in accountName: {}", name,
              azureConfig.getName(), accountId);
          newEncryptedFile = azureVaultService.encryptFile(accountId, azureConfig, name, inputBytes, encryptedData);
          newEncryptedFile.setKmsId(azureConfig.getUuid());
          break;

        case CYBERARK:
          throw new SecretManagementException(CYBERARK_OPERATION_ERROR,
              "Encrypted file operations are not supported for CyberArk secret manager", USER);

        default:
          throw new IllegalArgumentException("Invalid type " + encryptionType);
      }
      newEncryptedFile.setEncryptionType(encryptionType);
      newEncryptedFile.setType(SettingVariableTypes.CONFIG_FILE);
    }

    long uploadFileSize = inputBytes.length;
    if (update) {
      if (newEncryptedFile != null) {
        // PL-1125: Remove old encrypted file in Vault if its name has changed so as not to have dangling entries.
        if (!Objects.equals(oldName, name)) {
          String secretName = encryptedData.getEncryptionKey();
          switch (encryptionType) {
            case VAULT:
              VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, encryptedData.getKmsId());
              vaultService.deleteSecret(accountId, secretName, vaultConfig);
              break;
            case AWS_SECRETS_MANAGER:
              AwsSecretsManagerConfig secretsManagerConfig =
                  secretsManagerService.getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId());
              secretsManagerService.deleteSecret(accountId, secretName, secretsManagerConfig);
              break;
            case AZURE_VAULT:
              AzureVaultConfig azureConfig =
                  azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId());
              azureVaultService.delete(accountId, secretName, azureConfig);
              logger.info("Deleting file {} after update from vault {} in accountid {}", secretName,
                  azureConfig.getName(), accountId);
              break;
            default:
              // Does not apply to other secret manager types
              break;
          }
        }

        encryptedData.setEncryptionKey(newEncryptedFile.getEncryptionKey());
        encryptedData.setEncryptedValue(newEncryptedFile.getEncryptedValue());
        encryptedData.setKmsId(newEncryptedFile.getKmsId());
        encryptedData.setEncryptionType(newEncryptedFile.getEncryptionType());
        encryptedData.setFileSize(uploadFileSize);
      }
    } else {
      encryptedData = newEncryptedFile;
      encryptedData.setUuid(uuid);
      encryptedData.setType(SettingVariableTypes.CONFIG_FILE);
      encryptedData.setAccountId(accountId);
      encryptedData.setFileSize(uploadFileSize);
    }
    encryptedData.setName(name);
    encryptedData.setEncryptionType(encryptionType);
    encryptedData.setUsageRestrictions(usageRestrictions);
    encryptedData.setBase64Encoded(true);

    String recordId;
    try {
      recordId = wingsPersistence.save(encryptedData);
      generateAuditForEncryptedRecord(accountId, oldEntityData, recordId);
    } catch (DuplicateKeyException e) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "File " + name + " already exists", USER);
    }

    if (update && newEncryptedFile != null) {
      // update parent's file size
      Set<Parent> parents = new HashSet<>();
      if (isNotEmpty(encryptedData.getParentIds())) {
        for (String parentId : encryptedData.getParentIds()) {
          parents.add(Parent.builder()
                          .id(parentId)
                          .variableType(SettingVariableTypes.CONFIG_FILE)
                          .encryptionDetail(
                              EncryptionDetail.builder().encryptionType(encryptedData.getEncryptionType()).build())
                          .build());
        }
      }
      List<UuidAware> configFiles = fetchParents(accountId, parents);
      configFiles.forEach(configFile -> {
        ((ConfigFile) configFile).setSize(uploadFileSize);
        wingsPersistence.save((ConfigFile) configFile);
      });
    }

    // Logging the secret file changes.
    if (UserThreadLocal.get() != null) {
      String auditMessage;
      if (update) {
        auditMessage = (isNotEmpty(oldName) && oldName.equals(name)) ? "Changed File" : "Changed Name and File";
        auditMessage = usageRestrictions == null ? auditMessage : auditMessage + " or Usage Restrictions";
      } else {
        auditMessage = "File uploaded";
      }
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(uuid)
                                .encryptedDataId(recordId)
                                .description(auditMessage)
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }

    return recordId;
  }

  private void generateAuditForEncryptedRecord(String accountId, EncryptedData oldEntityData, String newRecordId) {
    Type type = oldEntityData == null ? Type.CREATE : Type.UPDATE;
    EncryptedData newRecordData = wingsPersistence.get(EncryptedData.class, newRecordId);
    if (eligibleForCrudAudit(newRecordData)) {
      auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldEntityData, newRecordData, type);
    }
  }

  @Override
  public boolean deleteFile(String accountId, String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, encryptedData.getUsageRestrictions())) {
      throw new SecretManagementException(USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, USER);
    }

    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .filter(ACCOUNT_ID_KEY, accountId)
                                       .filter(ConfigFileKeys.encryptedFileId, uuId)
                                       .asList();
    if (!configFiles.isEmpty()) {
      StringBuilder errorMessage = new StringBuilder("Being used by ");
      for (ConfigFile configFile : configFiles) {
        errorMessage.append(configFile.getFileName()).append(", ");
      }

      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, errorMessage.toString(), USER);
    }

    switch (encryptedData.getEncryptionType()) {
      case LOCAL:
      case KMS:
      case GCP_KMS:
        fileService.deleteFile(String.valueOf(encryptedData.getEncryptedValue()), CONFIGS);
        break;
      case VAULT:
        vaultService.deleteSecret(accountId, encryptedData.getEncryptionKey(),
            vaultService.getVaultConfig(accountId, encryptedData.getKmsId()));
        break;
      case AWS_SECRETS_MANAGER:
        secretsManagerService.deleteSecret(accountId, encryptedData.getEncryptionKey(),
            secretsManagerService.getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId()));
        break;
      case AZURE_VAULT:
        azureVaultService.delete(accountId, encryptedData.getEncryptionKey(),
            azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId()));
        break;
      case CYBERARK:
        throw new SecretManagementException(
            CYBERARK_OPERATION_ERROR, "Delete file operation is not supported for CyberArk secret manager", USER);
      default:
        throw new IllegalStateException("Invalid type " + encryptedData.getEncryptionType());
    }
    return deleteAndReportForAuditRecord(accountId, encryptedData);
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException {
    List<EncryptedData> filteredEncryptedDataList = Lists.newArrayList();

    int batchOffset = pageRequest.getStart();
    final int batchPageSize;

    // Increase the batch fetch page size to 2 times the requested, just in case some of the data
    // are filtered out based on usage restrictions. Or decrease the batch fetch size to 1000 if
    // the requested page size is too big (>1000);
    final int inputPageSize = pageRequest.getPageSize();
    if (2 * inputPageSize > PageRequest.DEFAULT_UNLIMITED) {
      batchPageSize = PageRequest.DEFAULT_UNLIMITED;
    } else {
      batchPageSize = 2 * inputPageSize;
    }

    boolean isAccountAdmin = userService.isAccountAdmin(accountId);

    RestrictionsAndAppEnvMap restrictionsAndAppEnvMap =
        usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(accountId, Action.READ);
    Map<String, Set<String>> appEnvMapFromPermissions = restrictionsAndAppEnvMap.getAppEnvMap();
    UsageRestrictions restrictionsFromUserPermissions = restrictionsAndAppEnvMap.getUsageRestrictions();

    int numRecordsReturnedCurrentBatch;
    do {
      PageRequest<EncryptedData> batchPageRequest = pageRequest.copy();
      batchPageRequest.setOffset(String.valueOf(batchOffset));
      batchPageRequest.setLimit(String.valueOf(batchPageSize));

      PageResponse<EncryptedData> batchPageResponse = wingsPersistence.query(EncryptedData.class, batchPageRequest);
      List<EncryptedData> encryptedDataList = batchPageResponse.getResponse();
      numRecordsReturnedCurrentBatch = encryptedDataList.size();

      // Set the new offset if another batch retrieval is needed if the requested page size is not fulfilled yet.
      batchOffset = filterSecreteDataBasedOnUsageRestrictions(accountId, isAccountAdmin, appIdFromRequest,
          envIdFromRequest, details, inputPageSize, batchOffset, appEnvMapFromPermissions,
          restrictionsFromUserPermissions, encryptedDataList, filteredEncryptedDataList);
    } while (numRecordsReturnedCurrentBatch == batchPageSize && filteredEncryptedDataList.size() < inputPageSize);

    if (details) {
      fillInDetails(accountId, filteredEncryptedDataList);
    }

    // UI should read the adjust batchOffset while sending another page request!
    return aPageResponse()
        .withOffset(String.valueOf(batchOffset))
        .withLimit(String.valueOf(inputPageSize))
        .withResponse(filteredEncryptedDataList)
        .withTotal(Long.valueOf(filteredEncryptedDataList.size()))
        .build();
  }

  private void fillInDetails(String accountId, List<EncryptedData> encryptedDataList) throws IllegalAccessException {
    if (isEmpty(encryptedDataList)) {
      return;
    }

    Set<String> encryptedDataIds = encryptedDataList.stream().map(EncryptedData::getUuid).collect(Collectors.toSet());
    Map<String, Long> usageLogSizes = getUsageLogSizes(accountId, encryptedDataIds, SettingVariableTypes.SECRET_TEXT);
    Map<String, Long> changeLogSizes = getChangeLogSizes(accountId, encryptedDataIds, SettingVariableTypes.SECRET_TEXT);

    Map<String, SecretManagerConfig> secretManagerConfigMap = getSecretManagerMap(accountId);
    for (EncryptedData encryptedData : encryptedDataList) {
      String entityId = encryptedData.getUuid();

      encryptedData.setEncryptedBy(
          getSecretManagerName(encryptedData.getKmsId(), encryptedData.getEncryptionType(), secretManagerConfigMap));
      int secretUsageSize = encryptedData.getParentIds() == null ? 0 : encryptedData.getParentIds().size();
      encryptedData.setSetupUsage(secretUsageSize);

      if (usageLogSizes.containsKey(entityId)) {
        encryptedData.setRunTimeUsage(usageLogSizes.get(entityId).intValue());
      }
      if (changeLogSizes.containsKey(entityId)) {
        encryptedData.setChangeLog(changeLogSizes.get(entityId).intValue());
      }
    }
  }

  /**
   * Filter the retrieved encrypted data list based on usage restrictions. Some of the
   * encrypted data won't be presented to the end-user if the end-user doesn't have
   * access permissions.
   * <p>
   * The filtered list size should never exceed the over page size from the page request.
   * <p>
   * It will return an adjusted batch offset if another batch retrieval is needed as the original page request
   * has not fulfilled. The new batch load will start from the adjusted offset.
   */
  private int filterSecreteDataBasedOnUsageRestrictions(String accountId, boolean isAccountAdmin,
      String appIdFromRequest, String envIdFromRequest, boolean details, int inputPageSize, int batchOffset,
      Map<String, Set<String>> appEnvMapFromPermissions, UsageRestrictions restrictionsFromUserPermissions,
      List<EncryptedData> encryptedDataList, List<EncryptedData> filteredEncryptedDataList) {
    int index = 0;
    Set<String> appsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMap = envService.getAppIdEnvMap(appsByAccountId);

    for (EncryptedData encryptedData : encryptedDataList) {
      index++;

      UsageRestrictions usageRestrictionsFromEntity = encryptedData.getUsageRestrictions();
      if (usageRestrictionsService.hasAccess(accountId, isAccountAdmin, appIdFromRequest, envIdFromRequest,
              usageRestrictionsFromEntity, restrictionsFromUserPermissions, appEnvMapFromPermissions, appIdEnvMap)) {
        filteredEncryptedDataList.add(encryptedData);
        encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
        encryptedData.setEncryptionKey(SECRET_MASK);

        // Already got all data the page request wanted. Break out of the loop, no more filtering
        // to save some CPU cycles and reduce the latency.
        if (filteredEncryptedDataList.size() == inputPageSize) {
          break;
        }
      }
    }

    // The requested page size may have not been filled, may need to fetch another batch and adjusting the offset
    // accordingly;
    return batchOffset + index;
  }

  @Override
  public PageResponse<EncryptedData> listSecretsMappedToAccount(
      String accountId, PageRequest<EncryptedData> pageRequest, boolean details) throws IllegalAccessException {
    // Also covers the case where its system originated call
    boolean isAccountAdmin = userService.isAccountAdmin(accountId);

    if (!isAccountAdmin) {
      return aPageResponse().withResponse(Collections.emptyList()).build();
    }

    pageRequest.addFilter(EncryptedDataKeys.usageRestrictions, Operator.NOT_EXISTS);

    PageResponse<EncryptedData> pageResponse = wingsPersistence.query(EncryptedData.class, pageRequest);
    List<EncryptedData> encryptedDataList = pageResponse.getResponse();

    for (EncryptedData encryptedData : encryptedDataList) {
      encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
      encryptedData.setEncryptionKey(SECRET_MASK);
    }

    if (details) {
      fillInDetails(accountId, encryptedDataList);
    }

    pageResponse.setResponse(encryptedDataList);
    pageResponse.setTotal((long) encryptedDataList.size());
    return pageResponse;
  }

  @Override
  public List<UuidAware> getSecretUsage(String accountId, String secretTextId) {
    EncryptedData secretText = wingsPersistence.get(EncryptedData.class, secretTextId);
    checkNotNull(secretText, "could not find secret with id " + secretTextId);
    if (secretText.getParentIds() == null) {
      return Collections.emptyList();
    }

    Map<String, SecretManagerConfig> secretManagerConfigMap = getSecretManagerMap(accountId);
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
                                            .secretManagerName(getSecretManagerName(secretText.getKmsId(),
                                                secretText.getEncryptionType(), secretManagerConfigMap))
                                            .build())
                      .build());
    }

    return fetchParents(accountId, parents);
  }

  private List<String> getSecretIds(String accountId, Collection<String> entityIds, SettingVariableTypes variableType)
      throws IllegalAccessException {
    final List<String> secretIds = new ArrayList<>();
    switch (variableType) {
      case SERVICE_VARIABLE:
        ServiceVariable serviceVariable = wingsPersistence.createQuery(ServiceVariable.class)
                                              .filter(ACCOUNT_ID_KEY, accountId)
                                              .field(ID_KEY)
                                              .in(entityIds)
                                              .get();

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
        secretIds.addAll(entityIds);
        break;

      default:
        SettingAttribute settingAttribute = wingsPersistence.createQuery(SettingAttribute.class)
                                                .filter(ACCOUNT_ID_KEY, accountId)
                                                .field(ID_KEY)
                                                .in(entityIds)
                                                .get();

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
      List<Parent> parentList = parentByTypes.get(parent.getVariableType(), parent.getEncryptionDetail());
      if (parentList == null) {
        parentList = new ArrayList<>();
        parentByTypes.put(parent.getVariableType(), parent.getEncryptionDetail(), parentList);
      }
      parentList.add(parent);
    });

    List<UuidAware> rv = new ArrayList<>();
    parentByTypes.cellSet().forEach(cell -> {
      List<String> parentIds = cell.getValue().stream().map(parent -> parent.getId()).collect(Collectors.toList());
      switch (cell.getRowKey()) {
        case KMS:
          EncryptionConfig encryptionConfig = secretManagerConfigService.getDefaultSecretManager(accountId);
          if (encryptionConfig instanceof KmsConfig) {
            rv.add((KmsConfig) encryptionConfig);
          }
          break;
        case SERVICE_VARIABLE:
          List<ServiceVariable> serviceVariables = serviceVariableService
                                                       .list(aPageRequest()
                                                                 .addFilter(ID_KEY, Operator.IN, parentIds.toArray())
                                                                 .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                                                 .build())
                                                       .getResponse();
          serviceVariables.forEach(serviceVariable -> {
            serviceVariable.setValue(SECRET_MASK.toCharArray());
            if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
              ServiceTemplate serviceTemplate =
                  wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
              checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
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
                                                       .addFilter(ID_KEY, Operator.IN, parentIds.toArray())
                                                       .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                                       .build())
                                             .getResponse();

          configFiles.forEach(configFile -> {
            if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
              ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, configFile.getEntityId());
              checkNotNull(serviceTemplate, "can't find service template " + configFile);
              configFile.setServiceId(serviceTemplate.getServiceId());
            }
            configFile.setEncryptionType(cell.getColumnKey().getEncryptionType());
            configFile.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(configFiles);
          break;

        case VAULT:
          List<VaultConfig> vaultConfigs = wingsPersistence.createQuery(VaultConfig.class)
                                               .field(ID_KEY)
                                               .in(parentIds)
                                               .field(ACCOUNT_ID_KEY)
                                               .equal(accountId)
                                               .asList();
          vaultConfigs.forEach(vaultConfig -> {
            vaultConfig.setEncryptionType(cell.getColumnKey().getEncryptionType());
            vaultConfig.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(vaultConfigs);
          break;

        case AWS_SECRETS_MANAGER:
          List<AwsSecretsManagerConfig> secretsManagerConfigs =
              wingsPersistence.createQuery(AwsSecretsManagerConfig.class)
                  .field(ID_KEY)
                  .in(parentIds)
                  .field(ACCOUNT_ID_KEY)
                  .equal(accountId)
                  .asList();
          secretsManagerConfigs.forEach(secretsManagerConfig -> {
            secretsManagerConfig.setEncryptionType(cell.getColumnKey().getEncryptionType());
            secretsManagerConfig.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(secretsManagerConfigs);
          break;

        case CYBERARK:
          List<CyberArkConfig> cyberArkConfigs = wingsPersistence.createQuery(CyberArkConfig.class)
                                                     .field(ID_KEY)
                                                     .in(parentIds)
                                                     .field(ACCOUNT_ID_KEY)
                                                     .equal(accountId)
                                                     .asList();
          cyberArkConfigs.forEach(cyberArkConfig -> {
            cyberArkConfig.setEncryptionType(cell.getColumnKey().getEncryptionType());
            cyberArkConfig.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(cyberArkConfigs);
          break;

        default:
          List<SettingAttribute> settingAttributes = settingsService
                                                         .list(aPageRequest()
                                                                   .addFilter(ID_KEY, Operator.IN, parentIds.toArray())
                                                                   .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
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

  private Map<String, SecretManagerConfig> getSecretManagerMap(String accountId) {
    Map<String, SecretManagerConfig> result = new HashMap<>();
    List<SecretManagerConfig> secretManagerConfigs = listSecretManagers(accountId);
    for (SecretManagerConfig secretManagerConfig : secretManagerConfigs) {
      result.put(secretManagerConfig.getUuid(), secretManagerConfig);
    }
    return result;
  }

  private String getSecretManagerName(
      String kmsId, EncryptionType encryptionType, Map<String, SecretManagerConfig> secretManagerConfigMap) {
    if (encryptionType == LOCAL) {
      return HARNESS_DEFAULT_SECRET_MANAGER;
    } else if (secretManagerConfigMap.containsKey(kmsId)) {
      return secretManagerConfigMap.get(kmsId).getName();
    } else {
      logger.warn("Secret manager with id {} and type {} can't be resolved.", kmsId, encryptionType);
      return null;
    }
  }

  private void validateGlobalSecretManager() {
    KmsConfig kmsConfig = kmsService.getGlobalKmsConfig();
    if (kmsConfig != null) {
      try {
        kmsService.encrypt(UUID.randomUUID().toString().toCharArray(), GLOBAL_ACCOUNT_ID, kmsConfig);
        logger.info("Successfully validated global secret manager {} of type {}", kmsConfig.getUuid(),
            kmsConfig.getEncryptionType());
      } catch (Exception e) {
        logger.error("Could not validate global secret manager with id {} of type {}", kmsConfig.getUuid(),
            kmsConfig.getEncryptionType(), e);
      }
    }
  }

  private static boolean containsIllegalCharacters(String name) {
    String[] parts = name.split(ILLEGAL_CHARACTERS, 2);
    return parts.length > 1;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class).filter(ACCOUNT_ID_KEY, accountId).asList();
    for (EncryptedData encryptedData : encryptedDataList) {
      deleteSecret(accountId, encryptedData.getUuid());
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

  // Creating a map of appId/envIds which are referring the specific secret through service variable etc.
  private Map<String, Set<String>> getSetupAppEnvMap(EncryptedData encryptedData) {
    Map<String, Set<String>> referredAppEnvMap = new HashMap<>();
    List<UuidAware> secretUsages = getSecretUsage(encryptedData.getAccountId(), encryptedData.getUuid());
    for (UuidAware uuidAware : secretUsages) {
      String appId = null;
      String envId = null;
      String entityId = null;
      EntityType entityType = null;
      if (uuidAware instanceof ServiceVariable) {
        ServiceVariable serviceVariable = (ServiceVariable) uuidAware;
        appId = serviceVariable.getAppId();
        envId = serviceVariable.getEnvId();
        entityType = serviceVariable.getEntityType();
        entityId = serviceVariable.getEntityId();
      } else if (uuidAware instanceof ConfigFile) {
        ConfigFile configFile = (ConfigFile) uuidAware;
        appId = configFile.getAppId();
        envId = configFile.getEnvId();
        entityType = configFile.getEntityType();
        entityId = configFile.getEntityId();
      }

      // Retrieve envId from entity Id reference.
      if (entityType == EntityType.ENVIRONMENT) {
        Environment environment = envService.get(appId, entityId);
        if (environment != null) {
          envId = environment.getUuid();
        }
      }

      if (isNotEmpty(appId) && !GLOBAL_APP_ID.equals(appId)) {
        Set<String> envIds = referredAppEnvMap.computeIfAbsent(appId, k -> new HashSet<>());
        if (isNotEmpty(envId) && !GLOBAL_ENV_ID.equals(envId)) {
          envIds.add(envId);
        }
      }
    }
    return referredAppEnvMap;
  }

  /**
   * We should fail the secret usage restriction update if the app/env is still referred by other setup entities
   * but the update will remove such references. It will result in RBAC enforcement inconsistencies if this type of
   * operations are not prevented.
   */
  private void validateAppEnvChangesInUsageRestrictions(
      EncryptedData encryptedData, UsageRestrictions usageRestrictions) {
    Map<String, Set<String>> setupAppEnvMap = getSetupAppEnvMap(encryptedData);
    if (setupAppEnvMap.size() == 0) {
      // This secret is not referred by any setup entities. no need to check.
      return;
    }

    usageRestrictionsService.validateSetupUsagesOnUsageRestrictionsUpdate(
        encryptedData.getAccountId(), setupAppEnvMap, usageRestrictions);
  }

  private boolean deleteAndReportForAuditRecord(String accountId, EncryptedData encryptedData) {
    boolean deleted = wingsPersistence.delete(EncryptedData.class, encryptedData.getUuid());
    if (deleted && eligibleForCrudAudit(encryptedData)) {
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
    }

    return deleted;
  }

  @Builder
  private static class ParsedVaultSecretRef {
    String secretManagerName;
    String vaultConfigId;
    VaultConfig vaultConfig;
    String basePath;
    String relativePath;
    String fullPath;
    String keyName;
  }

  @Builder
  private static class SecretUsageSummary {
    String encryptedDataId;
    long count;
  }

  @Builder
  private static class ChangeLogSummary {
    String encryptedDataId;
    long count;
  }
}
