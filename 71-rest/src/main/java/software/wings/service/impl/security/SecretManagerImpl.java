package software.wings.service.impl.security;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.EncryptedData.PARENT_ID_KEY;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.service.DelegateAgentFileService.FileBucket.CONFIGS;
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
import static io.harness.persistence.HQuery.excludeCount;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.security.encryption.EncryptionType.AWS_SECRETS_MANAGER;
import static io.harness.security.encryption.EncryptionType.AZURE_VAULT;
import static io.harness.security.encryption.EncryptionType.CUSTOM;
import static io.harness.security.encryption.EncryptionType.CYBERARK;
import static io.harness.security.encryption.EncryptionType.GCP_KMS;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.security.encryption.EncryptionType.VAULT;
import static io.harness.validation.PersistenceValidator.duplicateCheck;
import static io.harness.validation.Validator.equalCheck;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.ServiceVariable.ServiceVariableKeys;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.service.impl.security.AbstractSecretServiceImpl.checkNotNull;
import static software.wings.service.impl.security.AbstractSecretServiceImpl.checkState;
import static software.wings.service.impl.security.AbstractSecretServiceImpl.encryptLocal;
import static software.wings.service.intfc.security.VaultService.DEFAULT_BASE_PATH;
import static software.wings.service.intfc.security.VaultService.DEFAULT_KEY_NAME;
import static software.wings.service.intfc.security.VaultService.KEY_SPEARATOR;
import static software.wings.service.intfc.security.VaultService.PATH_SEPARATOR;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.DuplicateKeyException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SecretChangeLog;
import io.harness.beans.SecretChangeLog.SecretChangeLogKeys;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretText;
import io.harness.beans.SecretText.SecretTextKeys;
import io.harness.beans.SecretUsageLog;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.exception.WingsException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HIterator;
import io.harness.persistence.UuidAware;
import io.harness.queue.QueuePublisher;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.JsonUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.stream.BoundedInputStream;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SecretManagerRuntimeParameters;
import software.wings.beans.SecretManagerRuntimeParameters.SecretManagerRuntimeParametersKeys;
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
import software.wings.security.UsageRestrictions;
import software.wings.security.UsageRestrictions.AppEnvRestriction;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.security.encryption.setupusage.SecretSetupUsageService;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.SettingServiceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.CustomSecretsManagerEncryptionService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.GcpKmsService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.SettingVariableTypes;
import software.wings.utils.WingsReflectionUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
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
@OwnedBy(PL)
@Slf4j
@Singleton
public class SecretManagerImpl implements SecretManager {
  private static final String ILLEGAL_CHARACTERS = "[~!@#$%^&*'\"/?<>,;.]";
  private static final String URL_ROOT_PREFIX = "//";
  // Prefix YAML ingestion generated secret names with this prefix
  private static final String YAML_PREFIX = "YAML_";
  private static final String DEPRECATION_NOT_SUPPORTED =
      "Deprecate operation is not supported for CyberArk secret manager";
  static final Set<EncryptionType> ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD = EnumSet.of(LOCAL, GCP_KMS, KMS);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private KmsService kmsService;
  @Inject private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject private GcpKmsService gcpKmsService;
  @Inject private GlobalEncryptDecryptClient globalEncryptDecryptClient;
  @Inject private VaultService vaultService;
  @Inject private AwsSecretsManagerService secretsManagerService;
  @Inject private AzureVaultService azureVaultService;
  @Inject private CyberArkService cyberArkService;
  @Inject private FileService fileService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private QueuePublisher<KmsTransitionEvent> transitionKmsQueue;
  @Inject private AppService appService;
  @Inject private EnvironmentService envService;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private LocalEncryptionService localEncryptionService;
  @Inject private UserService userService;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private AzureSecretsManagerService azureSecretsManagerService;
  @Inject private CustomSecretsManagerEncryptionService customSecretsManagerEncryptionService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private SecretSetupUsageService secretSetupUsageService;
  @Inject @Named("hashicorpvault") private RuntimeCredentialsInjector vaultRuntimeCredentialsInjector;
  @Inject private SettingServiceHelper settingServiceHelper;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public EncryptionType getEncryptionType(String accountId) {
    return secretManagerConfigService.getEncryptionType(accountId);
  }

  @Override
  public EncryptionType getEncryptionBySecretManagerId(String kmsId, String accountId) {
    return secretManagerConfigService.getEncryptionBySecretManagerId(kmsId, accountId);
  }

  @Override
  public List<SecretManagerConfig> listSecretManagers(String accountId) {
    return secretManagerConfigService.listSecretManagers(accountId, true);
  }

  @Override
  public SecretManagerConfig getSecretManager(String accountId, String kmsId) {
    return secretManagerConfigService.getSecretManager(accountId, kmsId, true);
  }

  @Override
  public EncryptedData encrypt(String accountId, SettingVariableTypes settingType, char[] secret,
      EncryptedData encryptedData, SecretText secretText) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Encrypting a secret");
      String path = null;
      Set<EncryptedDataParams> parameters = null;
      boolean scopedToAccount = false;
      String secretName = null;
      UsageRestrictions usageRestrictions = null;
      if (secretText != null) {
        secretName = secretText.getName();
        path = secretText.getPath();
        parameters = secretText.getParameters();
        usageRestrictions = secretText.getUsageRestrictions();
        scopedToAccount = secretText.isScopedToAccount();
      }

      String toEncrypt = secret == null ? null : String.valueOf(secret);
      // Need to initialize an EncryptedData instance to carry the 'path' value for delegate to validate against.
      if (encryptedData == null) {
        encryptedData = EncryptedData.builder()
                            .name(secretName)
                            .path(path)
                            .parameters(parameters)
                            .accountId(accountId)
                            .type(settingType)
                            .enabled(true)
                            .scopedToAccount(scopedToAccount)
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
          break;

        case KMS:
          KmsConfig kmsConfig = (KmsConfig) getSecretManager(accountId, kmsId, KMS);
          rv = kmsService.encrypt(secret, accountId, kmsConfig);
          rv.setKmsId(kmsConfig.getUuid());
          break;

        case VAULT:
          VaultConfig vaultConfig = (VaultConfig) getSecretManager(accountId, kmsId, VAULT);
          encryptedData.setKmsId(vaultConfig.getUuid());
          rv = vaultService.encrypt(secretName, toEncrypt, accountId, settingType, vaultConfig, encryptedData);
          rv.setKmsId(vaultConfig.getUuid());
          break;

        case AWS_SECRETS_MANAGER:
          AwsSecretsManagerConfig secretsManagerConfig =
              (AwsSecretsManagerConfig) getSecretManager(accountId, kmsId, AWS_SECRETS_MANAGER);
          encryptedData.setKmsId(secretsManagerConfig.getUuid());
          rv = secretsManagerService.encrypt(
              secretName, toEncrypt, accountId, settingType, secretsManagerConfig, encryptedData);
          rv.setKmsId(secretsManagerConfig.getUuid());
          break;

        case GCP_KMS:
          GcpKmsConfig gcpKmsConfig = (GcpKmsConfig) getSecretManager(accountId, kmsId, GCP_KMS);
          encryptedData.setKmsId(gcpKmsConfig.getUuid());
          rv = gcpKmsService.encrypt(toEncrypt, accountId, gcpKmsConfig, encryptedData);
          rv.setKmsId(gcpKmsConfig.getUuid());
          break;

        case AZURE_VAULT:
          AzureVaultConfig azureConfig = (AzureVaultConfig) getSecretManager(accountId, kmsId, AZURE_VAULT);
          encryptedData.setKmsId(azureConfig.getUuid());
          rv = azureVaultService.encrypt(secretName, toEncrypt, accountId, settingType, azureConfig, encryptedData);
          rv.setKmsId(azureConfig.getUuid());
          break;

        case CYBERARK:
          CyberArkConfig cyberArkConfig = (CyberArkConfig) getSecretManager(accountId, kmsId, CYBERARK);
          encryptedData.setKmsId(cyberArkConfig.getUuid());
          if (isNotEmpty(encryptedData.getPath())) {
            // CyberArk encrypt need to use decrypt of the secret reference as a way of validating the reference is
            // valid. If the  CyberArk reference is not valid, an exception will be throw.
            cyberArkService.decrypt(encryptedData, accountId, cyberArkConfig);
          } else {
            SecretManagerConfig secretManagerConfig = secretManagerConfigService.getGlobalSecretManager(accountId);
            if (secretManagerConfig != null) {
              logger.info(
                  "CyberArk doesn't support creating new secret. This new secret text will be created in the global KMS SecretStore instead");
              if (secretManagerConfig.getEncryptionType() == GCP_KMS) {
                rv = gcpKmsService.encrypt(toEncrypt, accountId, (GcpKmsConfig) secretManagerConfig, encryptedData);
              } else {
                rv = kmsService.encrypt(secret, accountId, (KmsConfig) secretManagerConfig);
              }
              rv.setEncryptionType(secretManagerConfig.getEncryptionType());
              rv.setKmsId(secretManagerConfig.getUuid());
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
            rv.setScopedToAccount(scopedToAccount);
            return rv;
          }
          rv = encryptedData;
          break;

        case CUSTOM:
          CustomSecretsManagerConfig customSecretsManagerConfig =
              (CustomSecretsManagerConfig) getSecretManager(accountId, kmsId, CUSTOM);
          customSecretsManagerEncryptionService.validateSecret(encryptedData, customSecretsManagerConfig);
          rv = encryptedData;
          break;

        default:
          throw new IllegalStateException("Invalid type:  " + encryptionType);
      }
      rv.setName(secretName);
      rv.setEncryptionType(encryptionType);
      rv.setType(settingType);
      rv.setUsageRestrictions(usageRestrictions);
      rv.setScopedToAccount(scopedToAccount);
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

  @Override
  public String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Encrypting a secret");
      EncryptedData encryptedData = encrypt(accountId, SettingVariableTypes.APM_VERIFICATION, secret.toCharArray(),
          null, SecretText.builder().name(UUID.randomUUID().toString()).usageRestrictions(usageRestrictions).build());
      String recordId = saveEncryptedData(encryptedData);
      generateAuditForEncryptedRecord(accountId, null, recordId);
      return recordId;
    }
  }

  @Override
  public Optional<EncryptedDataDetail> encryptedDataDetails(
      String accountId, String fieldName, String encryptedDataId, String workflowExecutionId) {
    EncryptedData encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                                      .filter(EncryptedDataKeys.accountId, accountId)
                                      .filter(ID_KEY, encryptedDataId)
                                      .get();
    if (encryptedData == null) {
      logger.info("No encrypted record set for field {} for id: {}", fieldName, encryptedDataId);
      return Optional.empty();
    }

    SecretManagerConfig encryptionConfig =
        getSecretManager(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());

    if (SecretManagerConfig.isTemplatized(encryptionConfig) && isNotEmpty(workflowExecutionId)) {
      encryptionConfig = updateRuntimeParametersAndGetConfig(workflowExecutionId, encryptionConfig);
    }

    if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
      setEncryptedValueToFileContent(encryptedData);
    }

    // PL-1836: Need to preprocess global KMS and turn the KMS encryption into a LOCAL encryption.
    EncryptedRecordData encryptedRecordData;
    if (encryptionConfig.isGlobalKms()) {
      logger.info(
          "Pre-processing the encrypted secret by global KMS secret manager for secret {}", encryptedData.getUuid());

      encryptedRecordData = globalEncryptDecryptClient.convertEncryptedRecordToLocallyEncrypted(
          encryptedData, accountId, encryptionConfig);

      // The encryption type will be set to LOCAL only if manager was able to decrypt.
      // If the decryption failed, we need to retain the kms encryption config, otherwise delegate task would
      // fail.
      if (encryptedRecordData.getEncryptionType() == LOCAL) {
        encryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
        logger.info("Replaced it with LOCAL encryption for secret {}", encryptedData.getUuid());
      }
    } else {
      encryptedRecordData = SecretManager.buildRecordData(encryptedData);
    }
    //[PL-12731]: Issue with morphia caching logic https://github.com/MorphiaOrg/morphia/issues/281.
    encryptionConfig.setUuid(null);
    EncryptedDataDetail encryptedDataDetail;
    if (encryptionConfig.getEncryptionType() == CUSTOM) {
      encryptedDataDetail = customSecretsManagerEncryptionService.buildEncryptedDataDetail(
          encryptedData, (CustomSecretsManagerConfig) encryptionConfig);
      encryptedDataDetail.setFieldName(fieldName);
    } else {
      encryptedDataDetail = EncryptedDataDetail.builder()
                                .encryptedData(encryptedRecordData)
                                .encryptionConfig(encryptionConfig)
                                .fieldName(fieldName)
                                .build();
    }
    this.updateUsageLogsForSecretText(workflowExecutionId, encryptedData);
    return Optional.ofNullable(encryptedDataDetail);
  }

  @Override
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

          Optional<EncryptedDataDetail> encryptedDataDetail =
              encryptedDataDetails(object.getAccountId(), f.getName(), id, workflowExecutionId);
          if (encryptedDataDetail.isPresent()) {
            encryptedDataDetails.add(encryptedDataDetail.get());
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new SecretManagementException(ENCRYPT_DECRYPT_ERROR, e, USER);
    }

    return encryptedDataDetails;
  }

  SecretManagerConfig updateRuntimeParametersAndGetConfig(
      String workflowExecutionId, SecretManagerConfig encryptionConfig) {
    Optional<SecretManagerRuntimeParameters> secretManagerRuntimeParametersOptional =
        getSecretManagerRuntimeCredentialsForExecution(workflowExecutionId, encryptionConfig.getUuid());
    if (!secretManagerRuntimeParametersOptional.isPresent()) {
      String errorMessage = String.format(
          "The workflow is using secrets from templatized secret manager: %s. Please configure a Templatized Secret Manager step to provide credentials for the secret manager.",
          encryptionConfig.getName());
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, errorMessage, USER);
    }
    Map<String, String> runtimeParameters =
        JsonUtils.asObject(secretManagerRuntimeParametersOptional.get().getRuntimeParameters(),
            new TypeReference<Map<String, String>>() {});
    return updateRuntimeParameters(encryptionConfig, runtimeParameters, false);
  }

  private void updateUsageLogsForSecretText(String workflowExecutionId, EncryptedData encryptedData) {
    if (isNotEmpty(workflowExecutionId)) {
      WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
      if (workflowExecution == null) {
        logger.warn("No workflow execution with id {} found.", workflowExecutionId);
      } else {
        SecretUsageLog usageLog = SecretUsageLog.builder()
                                      .encryptedDataId(encryptedData.getUuid())
                                      .workflowExecutionId(workflowExecutionId)
                                      .accountId(encryptedData.getAccountId())
                                      .appId(workflowExecution.getAppId())
                                      .envId(workflowExecution.getEnvId())
                                      .build();
        wingsPersistence.save(usageLog);
      }
    }
  }

  @VisibleForTesting
  public void setEncryptedValueToFileContent(EncryptedData encryptedData) {
    if (ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getEncryptionType())) {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      fileService.downloadToStream(String.valueOf(encryptedData.getEncryptedValue()), os, CONFIGS);
      encryptedData.setEncryptedValue(CHARSET.decode(ByteBuffer.wrap(os.toByteArray())).array());
    }

    if (isNotEmpty(encryptedData.getBackupEncryptedValue())
        && ENCRYPTION_TYPES_REQUIRING_FILE_DOWNLOAD.contains(encryptedData.getBackupEncryptionType())) {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      fileService.downloadToStream(String.valueOf(encryptedData.getBackupEncryptedValue()), os, CONFIGS);
      encryptedData.setBackupEncryptedValue(CHARSET.decode(ByteBuffer.wrap(os.toByteArray())).array());
    }
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
    List<String> secretIds = getSecretIds(accountId, Lists.newArrayList(entityId), variableType);
    // PL-3298: Some setting attribute doesn't have encrypted fields and therefore no secret Ids associated with it.
    // E.g. PHYSICAL_DATA_CENTER config. An empty response will be returned.
    if (isEmpty(secretIds)) {
      return new PageResponse<>(pageRequest);
    }

    pageRequest.addFilter(SecretChangeLogKeys.encryptedDataId, Operator.IN, secretIds.toArray());
    pageRequest.addFilter(SecretChangeLogKeys.accountId, Operator.EQ, accountId);
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

  @Override
  public Set<SecretSetupUsage> getSecretUsage(String accountId, String secretId) {
    return secretSetupUsageService.getSecretUsage(accountId, secretId);
  }

  private Map<String, Long> getUsageLogSizes(
      String accountId, Collection<String> entityIds, SettingVariableTypes variableType) throws IllegalAccessException {
    List<String> secretIds = getSecretIds(accountId, entityIds, variableType);
    Query<SecretUsageLog> query = wingsPersistence.createQuery(SecretUsageLog.class)
                                      .filter(SecretChangeLogKeys.accountId, accountId)
                                      .field(SecretChangeLogKeys.encryptedDataId)
                                      .in(secretIds);

    AggregationPipeline aggregationPipeline =
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
    List<String> secretIds = getSecretIds(accountId, Lists.newArrayList(entityId), variableType);
    List<SecretChangeLog> secretChangeLogs = wingsPersistence.createQuery(SecretChangeLog.class, excludeCount)
                                                 .filter(SecretChangeLogKeys.accountId, accountId)
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
    List<String> secretIds = getSecretIds(accountId, entityIds, variableType);
    Query<SecretChangeLog> query = wingsPersistence.createQuery(SecretChangeLog.class)
                                       .filter(SecretChangeLogKeys.accountId, accountId)
                                       .field(SecretChangeLogKeys.encryptedDataId)
                                       .in(secretIds);

    AggregationPipeline aggregationPipeline =
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
                                                .filter(SettingAttributeKeys.accountId, accountId)
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
              .filter(SettingAttributeKeys.accountId, accountId)
              .field(SettingAttributeKeys.category)
              .doesNotExist()
              .field(SettingAttribute.VALUE_TYPE_KEY)
              .in(Lists.newArrayList(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES));
      loadSettingQueryResult(winRmQuery, settingAttributeIds, settingAttributeList);
    }

    // 2. Fetch children encrypted records associated with these setting attributes in a batch
    Map<String, EncryptedData> encryptedDataMap = new HashMap<>();
    try (HIterator<EncryptedData> query = new HIterator<>(wingsPersistence.createQuery(EncryptedData.class)
                                                              .filter(EncryptedDataKeys.accountId, accountId)
                                                              .field(PARENT_ID_KEY)
                                                              .in(settingAttributeIds)
                                                              .fetch())) {
      for (EncryptedData encryptedData : query) {
        for (EncryptedDataParent encryptedDataParent : encryptedData.getParents()) {
          encryptedDataMap.put(encryptedDataParent.getId(), encryptedData);
        }
      }
    }

    // 3. Set 'encryptionType' and 'encryptedBy' field of setting attributes based on children encrypted record
    // association
    List<SettingAttribute> finalList = new ArrayList<>();
    for (SettingAttribute settingAttribute : settingAttributeList) {
      EncryptedData encryptedData = encryptedDataMap.get(settingAttribute.getUuid());
      if (encryptedData != null) {
        settingAttribute.setEncryptionType(encryptedData.getEncryptionType());
        settingAttribute.setEncryptedBy(
            secretManagerConfigService.getSecretManagerName(encryptedData.getKmsId(), accountId));
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
        settingServiceHelper.updateSettingAttributeBeforeResponse(settingAttribute, true);
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
    String basePath = vaultConfig.getBasePath() == null
        ? DEFAULT_BASE_PATH
        : PATH_SEPARATOR.concat(StringUtils.strip(vaultConfig.getBasePath(), PATH_SEPARATOR));
    String vaultPath = isEmpty(encryptedData.getPath())
        ? basePath + PATH_SEPARATOR + encryptedData.getEncryptionKey() + KEY_SPEARATOR + DEFAULT_KEY_NAME
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
      // This is a new Vault path based reference
      ParsedVaultSecretRef vaultSecretRef = parse(encryptedDataRef, accountId);
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(EncryptedDataKeys.accountId)
          .equal(accountId)
          .criteria(EncryptedDataKeys.encryptionType)
          .equal(EncryptionType.VAULT);
      if (isNotEmpty(vaultSecretRef.relativePath) && isNotEmpty(vaultSecretRef.fullPath)) {
        query.and(query.or(query.criteria(EncryptedDataKeys.encryptionKey).equal(vaultSecretRef.relativePath),
            query.criteria(EncryptedDataKeys.path).equal(vaultSecretRef.fullPath)));
      } else if (isNotEmpty(vaultSecretRef.relativePath)) {
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

    String encryptedDataId = saveEncryptedData(encryptedData);
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
      String basePath = vaultConfig.getBasePath() == null
          ? DEFAULT_BASE_PATH
          : PATH_SEPARATOR.concat(StringUtils.strip(vaultConfig.getBasePath(), PATH_SEPARATOR));
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

    SecretManagerConfig secretManagerConfig = wingsPersistence.get(SecretManagerConfig.class, fromSecretManagerId);
    if (secretManagerConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID)) {
      return transitionSecretsFromGlobalSecretManager(accountId, toEncryptionType, toSecretManagerId);
    }

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .filter(EncryptedDataKeys.accountId, accountId)
                                     .filter(EncryptedDataKeys.kmsId, fromSecretManagerId);

    if (toEncryptionType == VAULT) {
      if (vaultService.isReadOnly(toSecretManagerId)) {
        throw new SecretManagementException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Cannot transfer secrets to a read only vault", USER);
      }
      query = query.field(EncryptedDataKeys.type).notEqual(SettingVariableTypes.VAULT);
    }

    if (fromEncryptionType == VAULT && vaultService.isReadOnly(fromSecretManagerId)) {
      throw new SecretManagementException(
          UNSUPPORTED_OPERATION_EXCEPTION, "Cannot transfer secrets from a read only vault", USER);
    }

    if (toEncryptionType == CUSTOM || fromEncryptionType == CUSTOM) {
      throw new SecretManagementException(
          UNSUPPORTED_OPERATION_EXCEPTION, "Cannot transfer secret to or from a custom secret manager", USER);
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
  public boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretManagerId,
      EncryptionType toEncryptionType, String toSecretManagerId,
      Map<String, String> runtimeParametersForSourceSecretManager,
      Map<String, String> runtimeParametersForDestinationSecretManager) {
    if (!StringUtils.isEmpty(fromSecretManagerId)) {
      SecretManagerConfig sourceSecretManagerConfig = getSecretManager(accountId, fromSecretManagerId);
      if (SecretManagerConfig.isTemplatized(sourceSecretManagerConfig)) {
        throw new SecretManagementException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Cannot transfer secrets from a templatized secret manager", USER);
      }
    }
    if (!StringUtils.isEmpty(toSecretManagerId)) {
      SecretManagerConfig destinationSecretManagerConfig = getSecretManager(accountId, toSecretManagerId);
      if (SecretManagerConfig.isTemplatized(destinationSecretManagerConfig)) {
        throw new SecretManagementException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Cannot transfer secrets to a templatized secret manager", USER);
      }
    }
    return transitionSecrets(accountId, fromEncryptionType, fromSecretManagerId, toEncryptionType, toSecretManagerId);
  }

  private boolean transitionSecretsFromGlobalSecretManager(
      String accountId, EncryptionType toEncryptionType, String toSecretManagerId) {
    List<SecretManagerConfig> secretManagerConfigList = secretManagerConfigService.getAllGlobalSecretManagers();
    for (SecretManagerConfig secretManagerConfig : secretManagerConfigList) {
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                       .filter(EncryptedDataKeys.accountId, accountId)
                                       .filter(EncryptedDataKeys.kmsId, secretManagerConfig.getUuid());

      if (toEncryptionType == EncryptionType.VAULT) {
        query = query.field(EncryptedDataKeys.type).notEqual(SettingVariableTypes.VAULT);
      }

      try (HIterator<EncryptedData> iterator = new HIterator<>(query.fetch())) {
        for (EncryptedData dataToTransition : iterator) {
          transitionKmsQueue.send(KmsTransitionEvent.builder()
                                      .accountId(accountId)
                                      .entityId(dataToTransition.getUuid())
                                      .fromEncryptionType(secretManagerConfig.getEncryptionType())
                                      .fromKmsId(secretManagerConfig.getUuid())
                                      .toEncryptionType(toEncryptionType)
                                      .toKmsId(toSecretManagerId)
                                      .build());
        }
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
        throw new SecretManagementException(UNSUPPORTED_OPERATION_EXCEPTION, DEPRECATION_NOT_SUPPORTED, USER);
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
    encryptedData.setBackupKmsId(encrypted.getBackupKmsId());
    encryptedData.setBackupEncryptionKey(encrypted.getBackupEncryptionKey());
    encryptedData.setBackupEncryptedValue(encrypted.getBackupEncryptedValue());
    encryptedData.setBackupEncryptionType(encrypted.getBackupEncryptionType());

    String recordId = saveEncryptedData(encryptedData);
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
        throw new SecretManagementException(UNSUPPORTED_OPERATION_EXCEPTION, DEPRECATION_NOT_SUPPORTED, USER);

      default:
        break;
    }

    encryptedData.setEncryptionKey(encryptedFileData.getEncryptionKey());
    encryptedData.setEncryptedValue(encryptedFileData.getEncryptedValue());
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setKmsId(toConfig.getUuid());
    encryptedData.setBase64Encoded(true);

    encryptedData.setBackupKmsId(encryptedFileData.getBackupKmsId());
    encryptedData.setBackupEncryptionKey(encryptedFileData.getBackupEncryptionKey());
    encryptedData.setBackupEncryptedValue(encryptedFileData.getBackupEncryptedValue());
    encryptedData.setBackupEncryptionType(encryptedFileData.getBackupEncryptionType());
    String recordId = saveEncryptedData(encryptedData);
    generateAuditForEncryptedRecord(accountId, existingEncryptedRecord, recordId);
  }

  @Override
  public EncryptedData getSecretMappedToAccountByName(String accountId, String name) {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .filter(EncryptedDataKeys.accountId, accountId)
                                     .filter(EncryptedDataKeys.name, name)
                                     .field(EncryptedDataKeys.usageRestrictions)
                                     .doesNotExist();
    return query.get();
  }

  @Override
  public EncryptedData getSecretMappedToAppByName(String accountId, String appId, String envId, String name) {
    PageRequest<EncryptedData> pageRequest = aPageRequest()
                                                 .addFilter(EncryptedDataKeys.name, Operator.EQ, name)
                                                 .addFilter(EncryptedDataKeys.accountId, Operator.EQ, accountId)
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
        .filter(EncryptedDataKeys.accountId, accountId)
        .filter(EncryptedDataKeys.ID_KEY, id)
        .get();
  }

  @Override
  public EncryptedData getSecretByName(String accountId, String name) {
    return wingsPersistence.createQuery(EncryptedData.class)
        .filter(EncryptedDataKeys.accountId, accountId)
        .filter(EncryptedDataKeys.name, name)
        .get();
  }

  @Override
  public String saveSecret(String accountId, SecretText secretText) {
    if (!StringUtils.isEmpty(secretText.getKmsId())) {
      SecretManagerConfig secretManagerConfig = getSecretManager(accountId, secretText.getKmsId());
      if (SecretManagerConfig.isTemplatized(secretManagerConfig)) {
        updateRuntimeParameters(secretManagerConfig, secretText.getRuntimeParameters(), true);
      }
    }
    return saveSecretInternal(accountId, secretText);
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
        String secretId = saveSecret(accountId, secretText);
        secretIds.add(secretId);
        logger.info("Imported secret '{}' successfully with uid: {}", secretText.getName(), secretId);
      } catch (WingsException e) {
        logger.warn("Failed to save import secret '{}' with error: {}", secretText.getName(), e.getMessage());
      }
    }
    return secretIds;
  }

  @Override
  public String saveSecretUsingLocalMode(String accountId, SecretText secretText) {
    secretText.setKmsId(accountId);
    return saveSecretInternal(accountId, secretText);
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
          vaultService.saveOrUpdateVaultConfig(accountId, vaultConfig, true);
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
            gcpSecretsManagerService.saveGcpKmsConfig(accountId, gcpKmsConfig, true);
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
  public Optional<SecretManagerRuntimeParameters> getSecretManagerRuntimeCredentialsForExecution(
      String executionId, String secretManagerId) {
    SecretManagerRuntimeParameters secretManagerRuntimeParameters =
        wingsPersistence.createQuery(SecretManagerRuntimeParameters.class)
            .field(SecretManagerRuntimeParametersKeys.executionId)
            .equal(executionId)
            .field(SecretManagerRuntimeParametersKeys.secretManagerId)
            .equal(secretManagerId)
            .get();
    if (Objects.nonNull(secretManagerRuntimeParameters)) {
      EncryptedData encryptedData =
          wingsPersistence.get(EncryptedData.class, secretManagerRuntimeParameters.getRuntimeParameters());
      secretManagerRuntimeParameters.setRuntimeParameters(
          String.valueOf(AbstractSecretServiceImpl.decryptLocal(encryptedData)));
      return Optional.of(secretManagerRuntimeParameters);
    }
    return Optional.empty();
  }

  @Override
  public SecretManagerRuntimeParameters configureSecretManagerRuntimeCredentialsForExecution(
      String accountId, String kmsId, String executionId, Map<String, String> runtimeParameters) {
    String runtimeParametersString = JsonUtils.asJson(runtimeParameters);

    EncryptedData encryptedData = encryptLocal(runtimeParametersString.toCharArray());
    encryptedData.setAccountId(accountId);
    encryptedData.setName(String.format("%s_%s_%s", executionId, kmsId, accountId));
    encryptedData.setEncryptionType(VAULT);
    String encryptedDataId = saveEncryptedData(encryptedData);

    SecretManagerRuntimeParameters secretManagerRuntimeParameters = SecretManagerRuntimeParameters.builder()
                                                                        .executionId(executionId)
                                                                        .accountId(accountId)
                                                                        .secretManagerId(kmsId)
                                                                        .runtimeParameters(encryptedDataId)
                                                                        .build();
    wingsPersistence.save(secretManagerRuntimeParameters);
    return secretManagerRuntimeParameters;
  }

  private RuntimeCredentialsInjector getRuntimeCredentialsInjectorInstance(EncryptionType encryptionType) {
    if (encryptionType == VAULT) {
      return vaultRuntimeCredentialsInjector;
    }
    throw new UnsupportedOperationException("Runtime credentials not supported for encryption type: " + encryptionType);
  }

  boolean updateSecretInternal(String accountId, String encryptedDataId, SecretText secretText) {
    EncryptedData savedData = Optional.ofNullable(wingsPersistence.get(EncryptedData.class, encryptedDataId))
                                  .<SecretManagementException>orElseThrow(() -> {
                                    String reason = "Secret " + secretText.getName() + "does not exists";
                                    throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, reason, null, USER);
                                  });
    EncryptedData oldEntity = kryoSerializer.clone(savedData);

    if (containsIllegalCharacters(secretText.getName())) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Secret name should not have any of the following characters " + ILLEGAL_CHARACTERS, USER);
    }

    char[] secretValue = isEmpty(secretText.getValue()) ? null : secretText.getValue().toCharArray();
    if (secretText.getParameters() == null) {
      secretText.setParameters(new HashSet<>());
    }
    // PL-3160: Make sure update/edit of existing secret to stick with the currently associated secret manager
    // Not switching to the current default secret manager.
    EncryptionType encryptionType = savedData.getEncryptionType();
    validateSecretPath(encryptionType, secretText.getPath());

    // validate usage restriction.
    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
        accountId, savedData.getUsageRestrictions(), secretText.getUsageRestrictions(), secretText.isScopedToAccount());
    if (!Objects.equals(savedData.getUsageRestrictions(), secretText.getUsageRestrictions())) {
      // Validate if change of the usage scope is resulting in with dangling references in service/environments.
      validateAppEnvChangesInUsageRestrictions(savedData, secretText.getUsageRestrictions());
    }

    boolean nameChanged = !Objects.equals(secretText.getName(), savedData.getName());
    boolean valueChanged = isNotEmpty(secretText.getValue()) && !secretText.getValue().equals(SECRET_MASK);
    boolean pathChanged = !Objects.equals(secretText.getPath(), savedData.getPath());
    boolean variablesChanged = !Objects.equals(secretText.getParameters(), savedData.getParameters());

    boolean needReencryption = false;

    StringBuilder builder = new StringBuilder();
    if (nameChanged) {
      builder.append("Changed name");
      savedData.removeSearchTag(null, savedData.getName(), null);
      savedData.setName(secretText.getName());
      savedData.addSearchTag(secretText.getName());

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
      savedData.setPath(secretText.getPath());
    }
    if (variablesChanged) {
      needReencryption = true;
      builder.append(builder.length() > 0 ? " & secret variables" : " Changed secret variables");
      savedData.setParameters(secretText.getParameters());
    }
    if (secretText.getUsageRestrictions() != null) {
      builder.append(builder.length() > 0 ? " & usage restrictions" : "Changed usage restrictions");
    }
    String auditMessage = builder.toString();

    // Re-encrypt if secret value or path has changed. Update should not change the existing Encryption type and
    // secret manager if the secret is 'path' enabled!
    if (needReencryption) {
      EncryptedData encryptedData =
          encrypt(accountId, SettingVariableTypes.SECRET_TEXT, secretValue, savedData, secretText);
      savedData.setEncryptionKey(encryptedData.getEncryptionKey());
      savedData.setEncryptedValue(encryptedData.getEncryptedValue());
      savedData.setEncryptionType(encryptedData.getEncryptionType());
      savedData.setKmsId(encryptedData.getKmsId());
      savedData.setBackupKmsId(encryptedData.getBackupKmsId());
      savedData.setBackupEncryptionType(encryptedData.getBackupEncryptionType());
      savedData.setBackupEncryptedValue(encryptedData.getBackupEncryptedValue());
      savedData.setBackupEncryptionKey(encryptedData.getBackupEncryptionKey());
    }
    savedData.setUsageRestrictions(secretText.getUsageRestrictions());
    savedData.setScopedToAccount(secretText.isScopedToAccount());
    saveEncryptedData(savedData);
    if (eligibleForCrudAudit(savedData)) {
      auditServiceHelper.reportForAuditingUsingAccountId(savedData.getAccountId(), oldEntity, savedData, Type.UPDATE);
    }

    insertSecretChangeLog(accountId, encryptedDataId, auditMessage);
    return encryptedDataId != null;
  }

  public boolean updateSecret(String accountId, String encryptedDataId, SecretText secretText) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataId);
    if (Objects.nonNull(encryptedData)) {
      SecretManagerConfig secretManagerConfig = getSecretManager(accountId, encryptedData.getKmsId());
      if (SecretManagerConfig.isTemplatized(secretManagerConfig)) {
        updateRuntimeParameters(secretManagerConfig, secretText.getRuntimeParameters(), true);
      }
    }
    return updateSecretInternal(accountId, encryptedDataId, secretText);
  }

  String saveSecretInternal(String accountId, SecretText secretText) {
    if (containsIllegalCharacters(secretText.getName())) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Secret name should not have any of the following characters " + ILLEGAL_CHARACTERS, USER);
    }
    char[] secretValue = isEmpty(secretText.getValue()) ? null : secretText.getValue().toCharArray();
    // INSERT use case
    usageRestrictionsService.validateUsageRestrictionsOnEntitySave(
        accountId, secretText.getUsageRestrictions(), secretText.isScopedToAccount());
    EncryptionType encryptionType = secretText.getKmsId() == null
        ? getEncryptionType(accountId)
        : getEncryptionBySecretManagerId(secretText.getKmsId(), accountId);

    validateSecretPath(encryptionType, secretText.getPath());
    EncryptedData encrypted = EncryptedData.builder()
                                  .name(secretText.getName())
                                  .path(secretText.getPath())
                                  .parameters(secretText.getParameters())
                                  .accountId(accountId)
                                  .type(SettingVariableTypes.SECRET_TEXT)
                                  .encryptionType(encryptionType)
                                  .kmsId(secretText.getKmsId())
                                  .enabled(true)
                                  .build();
    EncryptedData encryptedData =
        encrypt(accountId, SettingVariableTypes.SECRET_TEXT, secretValue, encrypted, secretText);
    encryptedData.addSearchTag(secretText.getName());
    String encryptedDataId;
    try {
      encryptedDataId = saveEncryptedData(encryptedData);
      generateAuditForEncryptedRecord(accountId, null, encryptedDataId);
    } catch (DuplicateKeyException e) {
      String reason = "Variable " + secretText.getName() + " already exists";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, reason, e, USER);
    }
    String auditMessage = "Created";
    insertSecretChangeLog(accountId, encryptedDataId, auditMessage);
    return encryptedDataId;
  }

  private void insertSecretChangeLog(String accountId, String encryptedDataId, String auditMessage) {
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
  }

  private boolean eligibleForCrudAudit(EncryptedData savedData) {
    return SettingVariableTypes.CONFIG_FILE == savedData.getType()
        || SettingVariableTypes.SECRET_TEXT == savedData.getType();
  }

  @Override
  public boolean updateUsageRestrictionsForSecretOrFile(
      String accountId, String uuId, UsageRestrictions usageRestrictions, boolean scopedToAccount) {
    EncryptedData savedData = wingsPersistence.get(EncryptedData.class, uuId);
    if (savedData == null) {
      return false;
    }
    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
        accountId, savedData.getUsageRestrictions(), usageRestrictions, scopedToAccount);
    // No validation of `validateAppEnvChangesInUsageRestrictions` is performed in this method
    // because usually this update is a result of removing application/environment.

    savedData.setUsageRestrictions(usageRestrictions);
    savedData.setScopedToAccount(scopedToAccount);

    try {
      saveEncryptedData(savedData);
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
  public boolean deleteSecret(String accountId, String secretId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    checkNotNull(encryptedData, "No encrypted record found with id " + secretId);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(
            accountId, encryptedData.getUsageRestrictions(), encryptedData.isScopedToAccount())) {
      throw new SecretManagementException(USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, USER);
    }

    Set<SecretSetupUsage> secretSetupUsages = getSecretUsage(accountId, secretId);
    if (!secretSetupUsages.isEmpty()) {
      String reason = "Can not delete secret because it is still being used. See setup usage(s) of the secret.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, reason, USER);
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
        if (isEmpty(encryptedData.getPath())) {
          String keyName = encryptedData.getEncryptionKey();
          AzureVaultConfig encryptionConfig =
              azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId());
          azureVaultService.delete(accountId, keyName, encryptionConfig);
        }
        break;
      default:
        break;
    }

    return deleteAndReportForAuditRecord(accountId, encryptedData);
  }

  @Override
  public boolean deleteSecret(String accountId, String secretId, Map<String, String> runtimeParameters) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    if (Objects.nonNull(encryptedData)) {
      SecretManagerConfig secretManagerConfig = getSecretManager(accountId, encryptedData.getKmsId());
      if (SecretManagerConfig.isTemplatized(secretManagerConfig)) {
        updateRuntimeParameters(secretManagerConfig, runtimeParameters, true);
      }
    }
    return deleteSecret(accountId, secretId);
  }

  @Override
  public boolean deleteSecretUsingUuid(String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    return deleteAndReportForAuditRecord(encryptedData.getAccountId(), encryptedData);
  }

  @Override
  public String saveFile(String accountId, String kmsId, String name, UsageRestrictions usageRestrictions,
      BoundedInputStream inputStream, boolean scopedToAccount, boolean hiddenFromListing) {
    return upsertFileInternal(accountId, kmsId, null, inputStream.getSize(), inputStream,
        SecretText.builder()
            .name(name)
            .usageRestrictions(usageRestrictions)
            .scopedToAccount(scopedToAccount)
            .hideFromListing(hiddenFromListing)
            .build());
  }

  @Override
  public String saveFile(String accountId, String kmsId, String name, long fileSize,
      UsageRestrictions usageRestrictions, BoundedInputStream inputStream, boolean scopedToAccount) {
    return upsertFileInternal(accountId, kmsId, null, fileSize, inputStream,
        SecretText.builder().name(name).usageRestrictions(usageRestrictions).scopedToAccount(scopedToAccount).build());
  }

  @Override
  public String saveFile(String accountId, String kmsId, String name, long fileSize,
      UsageRestrictions usageRestrictions, BoundedInputStream inputStream, Map<String, String> runtimeParameters,
      boolean scopedToAccount) {
    if (!StringUtils.isEmpty(kmsId)) {
      SecretManagerConfig secretManagerConfig = getSecretManager(accountId, kmsId);
      if (SecretManagerConfig.isTemplatized(secretManagerConfig)) {
        updateRuntimeParameters(secretManagerConfig, runtimeParameters, true);
      }
    }
    return saveFile(accountId, kmsId, name, fileSize, usageRestrictions, inputStream, scopedToAccount);
  }

  @Override
  public File getFile(String accountId, String uuid, File readInto) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    checkNotNull(encryptedData, "could not find file with id " + uuid);
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    switch (encryptionType) {
      case LOCAL:
        fileService.download(String.valueOf(encryptedData.getEncryptedValue()), readInto, CONFIGS);
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
      UsageRestrictions usageRestrictions, BoundedInputStream inputStream, boolean scopedToAccount) {
    EncryptedData encryptedData = getSecretById(accountId, uuid);
    checkNotNull(encryptedData, "File not found.");
    String recordId = upsertFileInternal(accountId, null, uuid, fileSize, inputStream,
        SecretText.builder().name(name).usageRestrictions(usageRestrictions).scopedToAccount(scopedToAccount).build());

    return isNotEmpty(recordId);
  }

  @Override
  public boolean updateFile(String accountId, String name, String uuid, long fileSize,
      UsageRestrictions usageRestrictions, BoundedInputStream inputStream, Map<String, String> runtimeParameters,
      boolean scopedToAccount) {
    EncryptedData encryptedData = getSecretById(accountId, uuid);
    if (Objects.nonNull(encryptedData)) {
      SecretManagerConfig secretManagerConfig = getSecretManager(accountId, encryptedData.getKmsId());
      if (SecretManagerConfig.isTemplatized(secretManagerConfig)) {
        updateRuntimeParameters(secretManagerConfig, runtimeParameters, true);
      }
    }
    return updateFile(accountId, name, uuid, fileSize, usageRestrictions, inputStream, scopedToAccount);
  }

  /**
   * This internal method should be able to handle the UPSERT of encrypted record. It will UPDATE the existing record if
   * the uuid exists, and INSERT if this record is new. The refactoring of this method helps taking care of the IMPORT
   * use case in which we would like to preserve the 'uuid' field while importing the exported encrypted keys from other
   * system.
   */
  private String upsertFileInternal(String accountId, String kmsId, String uuid, long fileSize,
      BoundedInputStream inputStream, SecretText secretText) {
    String name = null;
    if (secretText != null) {
      name = secretText.getName();
    }

    if (isEmpty(name)) {
      throw new SecretManagementException(ErrorCode.FILE_INTEGRITY_CHECK_FAILED, null, USER, null);
    }

    if (containsIllegalCharacters(name)) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Encrypted file name should not have any of the following characters " + ILLEGAL_CHARACTERS, USER);
    }

    if (inputStream.getSize() > 0 && fileSize > inputStream.getSize()) {
      Map<String, String> params = new HashMap<>();
      params.put("size", inputStream.getSize() / (1024 * 1024) + " MB");
      throw new SecretManagementException(
          ErrorCode.FILE_SIZE_EXCEEDS_LIMIT, null, USER, Collections.unmodifiableMap(params));
    }

    return upsertConfigFile(accountId, kmsId, uuid, inputStream, secretText);
  }

  private String upsertConfigFile(
      String accountId, String kmsId, String uuid, BoundedInputStream inputStream, SecretText secretText) {
    UsageRestrictions usageRestrictions = null;
    boolean scopedToAccount = false;
    String name = null;
    boolean hideFromListing = false;

    if (secretText != null) {
      name = secretText.getName();
      usageRestrictions = secretText.getUsageRestrictions();
      scopedToAccount = secretText.isScopedToAccount();
      hideFromListing = secretText.isHideFromListing();
    }

    boolean update = false;
    String oldName = null;
    String savedFileId = null;
    EncryptedData encryptedData = null;
    EncryptedData oldEntityData = null;
    EncryptionType encryptionType;

    if (isNotEmpty(uuid)) {
      encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
      if (encryptedData == null) {
        // Pure UPDATE case, need to throw exception is the record doesn't exist.
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "could not find file with id " + uuid, USER);
      }

      // This is needed for auditing as encryptedData will be changed in the process of update
      oldEntityData = kryoSerializer.clone(encryptedData);
    }

    if (encryptedData == null) {
      // INSERT in UPSERT case, get the system default encryption type.
      // If kmsId is null, the encryption should be LOCAL and not account default ?
      if (kmsId == null) {
        encryptionType = getEncryptionType(accountId);
      } else if (kmsId.equals(accountId)) {
        encryptionType = LOCAL;
      } else {
        encryptionType = getEncryptionBySecretManagerId(kmsId, accountId);
      }
      usageRestrictionsService.validateUsageRestrictionsOnEntitySave(accountId, usageRestrictions, scopedToAccount);
    } else {
      // UPDATE in UPSERT case
      update = true;
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
          accountId, encryptedData.getUsageRestrictions(), usageRestrictions, scopedToAccount);
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

    kmsId = update ? encryptedData.getKmsId() : kmsId;
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
        encryptedData.setBackupEncryptionKey(newEncryptedFile.getBackupEncryptionKey());
        encryptedData.setBackupEncryptedValue(newEncryptedFile.getBackupEncryptedValue());
        encryptedData.setBackupEncryptionType(newEncryptedFile.getBackupEncryptionType());
        encryptedData.setBackupKmsId(newEncryptedFile.getBackupKmsId());
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
    encryptedData.setScopedToAccount(scopedToAccount);
    encryptedData.setBase64Encoded(true);
    encryptedData.setHideFromListing(hideFromListing);

    String recordId;
    try {
      recordId = saveEncryptedData(encryptedData);
      generateAuditForEncryptedRecord(accountId, oldEntityData, recordId);
    } catch (DuplicateKeyException e) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "File " + name + " already exists", USER);
    }

    if (update && newEncryptedFile != null) {
      // update parent's file size
      List<UuidAware> entities =
          getSecretUsage(accountId, recordId).stream().map(SecretSetupUsage::getEntity).collect(Collectors.toList());
      entities.forEach(entity -> {
        if (entity instanceof ConfigFile) {
          ((ConfigFile) entity).setSize(uploadFileSize);
          wingsPersistence.save((ConfigFile) entity);
        }
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

  @VisibleForTesting
  public String saveEncryptedData(EncryptedData encryptedData) {
    if (encryptedData == null) {
      return null;
    }
    return duplicateCheck(() -> wingsPersistence.save(encryptedData), EncryptedDataKeys.name, encryptedData.getName());
  }

  private void generateAuditForEncryptedRecord(String accountId, EncryptedData oldEntityData, String newRecordId) {
    Type type = oldEntityData == null ? Type.CREATE : Type.UPDATE;
    EncryptedData newRecordData = wingsPersistence.get(EncryptedData.class, newRecordId);
    if (eligibleForCrudAudit(newRecordData)) {
      auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldEntityData, newRecordData, type);
    }
  }

  SecretManagerConfig updateRuntimeParameters(
      SecretManagerConfig secretManagerConfig, Map<String, String> runtimeParameters, boolean shouldUpdateVaultConfig) {
    Optional<SecretManagerConfig> updatedSecretManagerConfig =
        getRuntimeCredentialsInjectorInstance(secretManagerConfig.getEncryptionType())
            .updateRuntimeCredentials(secretManagerConfig, runtimeParameters, shouldUpdateVaultConfig);
    if (!updatedSecretManagerConfig.isPresent()) {
      throw new InvalidRequestException("values of one or more run time fields are missing.");
    }
    return updatedSecretManagerConfig.get();
  }

  @Override
  public boolean deleteFile(String accountId, String uuid) {
    EncryptedData encryptedData = wingsPersistence.createQuery(EncryptedData.class)
                                      .filter(EncryptedDataKeys.accountId, accountId)
                                      .filter(EncryptedDataKeys.uuid, uuid)
                                      .get();

    checkNotNull(encryptedData, "No encrypted record found with id " + uuid);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(
            accountId, encryptedData.getUsageRestrictions(), encryptedData.isScopedToAccount())) {
      throw new SecretManagementException(USER_NOT_AUTHORIZED_DUE_TO_USAGE_RESTRICTIONS, USER);
    }

    Set<SecretSetupUsage> secretSetupUsages = getSecretUsage(accountId, uuid);
    if (!secretSetupUsages.isEmpty()) {
      String reason = "Can not delete file because it is still being used. See setup usage(s) of the secret.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, reason, USER);
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
  public boolean deleteFile(String accountId, String uuId, Map<String, String> runtimeParameters) {
    EncryptedData encryptedData = getSecretById(accountId, uuId);
    if (Objects.nonNull(encryptedData)) {
      SecretManagerConfig secretManagerConfig = getSecretManager(accountId, encryptedData.getKmsId());
      if (SecretManagerConfig.isTemplatized(secretManagerConfig)) {
        updateRuntimeParameters(secretManagerConfig, runtimeParameters, true);
      }
    }
    return deleteFile(accountId, uuId);
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details, boolean listHidden)
      throws IllegalAccessException {
    if (!listHidden) {
      addFilterHideFromListing(pageRequest);
    }

    return listSecrets(accountId, pageRequest, appIdFromRequest, envIdFromRequest, details);
  }

  private void addFilterHideFromListing(PageRequest<EncryptedData> pageRequest) {
    SearchFilter op1 = SearchFilter.builder().fieldName(SecretTextKeys.hideFromListing).op(Operator.NOT_EXISTS).build();

    SearchFilter op2 = SearchFilter.builder()
                           .fieldName(SecretTextKeys.hideFromListing)
                           .op(Operator.EQ)
                           .fieldValues(new Object[] {Boolean.FALSE})
                           .build();

    pageRequest.addFilter(SecretTextKeys.hideFromListing, Operator.OR, op1, op2);
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException {
    List<EncryptedData> filteredEncryptedDataList = Lists.newArrayList();

    int batchOffset = pageRequest.getStart();
    int batchPageSize;

    // Increase the batch fetch page size to 2 times the requested, just in case some of the data
    // are filtered out based on usage restrictions. Or decrease the batch fetch size to 1000 if
    // the requested page size is too big such as greater than 1000
    int inputPageSize = pageRequest.getPageSize();
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

    for (EncryptedData encryptedData : encryptedDataList) {
      String entityId = encryptedData.getUuid();

      encryptedData.setEncryptedBy(
          secretManagerConfigService.getSecretManagerName(encryptedData.getKmsId(), accountId));
      int secretUsageSize = encryptedData.getParents().size();
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
      boolean isScopedToAccount = encryptedData.isScopedToAccount();
      if (usageRestrictionsService.hasAccess(accountId, isAccountAdmin, appIdFromRequest, envIdFromRequest,
              usageRestrictionsFromEntity, restrictionsFromUserPermissions, appEnvMapFromPermissions, appIdEnvMap,
              isScopedToAccount)) {
        filteredEncryptedDataList.add(encryptedData);
        encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
        encryptedData.setEncryptionKey(SECRET_MASK);
        encryptedData.setBackupEncryptedValue(SECRET_MASK.toCharArray());
        encryptedData.setBackupEncryptionKey(SECRET_MASK);

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
      encryptedData.setBackupEncryptedValue(SECRET_MASK.toCharArray());
      encryptedData.setBackupEncryptionKey(SECRET_MASK);
    }

    if (details) {
      fillInDetails(accountId, encryptedDataList);
    }

    pageResponse.setResponse(encryptedDataList);
    pageResponse.setTotal((long) encryptedDataList.size());
    return pageResponse;
  }

  private List<String> getSecretIds(String accountId, Collection<String> entityIds, SettingVariableTypes variableType)
      throws IllegalAccessException {
    List<String> secretIds = new ArrayList<>();
    switch (variableType) {
      case SERVICE_VARIABLE:
        ServiceVariable serviceVariable = wingsPersistence.createQuery(ServiceVariable.class)
                                              .filter(ServiceVariableKeys.accountId, accountId)
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
                                                .filter(SettingAttributeKeys.accountId, accountId)
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

  private static boolean containsIllegalCharacters(String name) {
    String[] parts = name.split(ILLEGAL_CHARACTERS, 2);
    return parts.length > 1;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).asList();
    for (EncryptedData encryptedData : encryptedDataList) {
      deleteSecret(accountId, encryptedData.getUuid());
    }
  }

  // Creating a map of appId/envIds which are referring the specific secret through service variable etc.
  private Map<String, Set<String>> getSetupAppEnvMap(EncryptedData encryptedData) {
    Map<String, Set<String>> referredAppEnvMap = new HashMap<>();
    List<UuidAware> secretUsages = getSecretUsage(encryptedData.getAccountId(), encryptedData.getUuid())
                                       .stream()
                                       .map(SecretSetupUsage::getEntity)
                                       .collect(Collectors.toList());
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

  @Override
  public void validateThatSecretManagerSupportsText(String accountId, String secretManagerId) {
    SecretManagerConfig secretManagerConfig = getSecretManager(accountId, secretManagerId, null);
    // setting the encrypted value and it is not supported by cyberArk
    if (secretManagerConfig != null && secretManagerConfig.getEncryptionType() == CYBERARK) {
      throw new InvalidRequestException(
          String.format("Secret values is not supported for the secretManager with id %s, type %s", secretManagerId,
              secretManagerConfig.getEncryptionType()));
    }
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

  private Set<EncryptedData> fetchSecretsFromSecretIds(@NonNull Set<String> secretIds, @NonNull String accountId) {
    Set<EncryptedData> encryptedDataSet = new HashSet<>();
    try (HIterator<EncryptedData> iterator = new HIterator<>(wingsPersistence.createQuery(EncryptedData.class)
                                                                 .field(ID_KEY)
                                                                 .in(secretIds)
                                                                 .field(EncryptedDataKeys.accountId)
                                                                 .equal(accountId)
                                                                 .fetch())) {
      while (iterator.hasNext()) {
        encryptedDataSet.add(iterator.next());
      }
    }
    return encryptedDataSet;
  }

  public boolean canUseSecretsInAppAndEnv(
      @NonNull Set<String> secretIds, @NonNull String accountId, String appIdFromRequest, String envIdFromRequest) {
    boolean isAccountAdmin = userService.isAccountAdmin(accountId);
    RestrictionsAndAppEnvMap restrictionsAndAppEnvMap =
        usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(accountId, Action.READ);
    Map<String, Set<String>> appEnvMapFromPermissions = restrictionsAndAppEnvMap.getAppEnvMap();
    UsageRestrictions restrictionsFromUserPermissions = restrictionsAndAppEnvMap.getUsageRestrictions();

    Set<String> appsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMapForAccount = envService.getAppIdEnvMap(appsByAccountId);

    return canUseSecretsInAppAndEnv(secretIds, accountId, appIdFromRequest, envIdFromRequest, isAccountAdmin,
        restrictionsFromUserPermissions, appEnvMapFromPermissions, appIdEnvMapForAccount);
  }

  public boolean canUseSecretsInAppAndEnv(@NonNull Set<String> secretIds, @NonNull String accountId,
      String appIdFromRequest, String envIdFromRequest, boolean isAccountAdmin,
      UsageRestrictions restrictionsFromUserPermissions, Map<String, Set<String>> appEnvMapFromPermissions,
      Map<String, List<Base>> appIdEnvMapForAccount) {
    Set<EncryptedData> encryptedDataSet = fetchSecretsFromSecretIds(secretIds, accountId);

    for (EncryptedData encryptedData : encryptedDataSet) {
      if (!usageRestrictionsService.hasAccess(accountId, isAccountAdmin, appIdFromRequest, envIdFromRequest,
              encryptedData.getUsageRestrictions(), restrictionsFromUserPermissions, appEnvMapFromPermissions,
              appIdEnvMapForAccount, encryptedData.isScopedToAccount())) {
        return false;
      }
    }
    return true;
  }

  public boolean hasUpdateAccessToSecrets(@NonNull Set<String> secretIds, @NonNull String accountId) {
    Set<EncryptedData> encryptedDataSet = fetchSecretsFromSecretIds(secretIds, accountId);

    for (EncryptedData encryptedData : encryptedDataSet) {
      if (!usageRestrictionsService.userHasPermissionsToChangeEntity(
              accountId, encryptedData.getUsageRestrictions(), encryptedData.isScopedToAccount())) {
        return false;
      }
    }
    return true;
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
