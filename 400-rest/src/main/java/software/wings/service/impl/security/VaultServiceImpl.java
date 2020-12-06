package software.wings.service.impl.security;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.persistence.HPersistence.upToOne;
import static io.harness.security.encryption.AccessType.APP_ROLE;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.settings.SettingVariableTypes.VAULT;

import static java.time.Duration.ofMillis;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedData.EncryptedDataKeys;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretChangeLog;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import io.harness.data.structure.EmptyPredicate;
import io.harness.encryptors.VaultEncryptorsRegistry;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.expression.SecretString;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import io.harness.persistence.HPersistence;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoSerializer;
import io.harness.templatizedsm.RuntimeCredentialsInjector;

import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.beans.VaultConfig.VaultConfigKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.VaultService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * Created by rsingh on 11/2/17.
 */
@OwnedBy(PL)
@Singleton
@Slf4j
public class VaultServiceImpl extends AbstractSecretServiceImpl implements VaultService, RuntimeCredentialsInjector {
  private static final String TOKEN_SECRET_NAME_SUFFIX = "_token";
  private static final String SECRET_ID_SECRET_NAME_SUFFIX = "_secret_id";
  private static final int NUM_OF_RETRIES = 3;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private VaultEncryptorsRegistry vaultEncryptorsRegistry;
  @Inject private AccountService accountService;

  @Override
  public VaultConfig getVaultConfig(String accountId, String entityId) {
    Query<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class)
                                   .filter(SecretManagerConfigKeys.accountId, accountId)
                                   .filter(ID_KEY, entityId);
    return getVaultConfigInternal(query);
  }

  @Override
  public VaultConfig getVaultConfigByName(String accountId, String name) {
    Query<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class)
                                   .filter(SecretManagerConfigKeys.accountId, accountId)
                                   .filter(EncryptedDataKeys.name, name);
    return getVaultConfigInternal(query);
  }

  private VaultConfig getVaultConfigInternal(Query<VaultConfig> query) {
    VaultConfig vaultConfig = query.get();

    if (vaultConfig != null) {
      EncryptedData encryptedToken = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      EncryptedData encryptedSecretId = wingsPersistence.get(EncryptedData.class, vaultConfig.getSecretId());
      if (encryptedToken == null && encryptedSecretId == null) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
            "Either auth token or secret Id field needs to be present for vault secret manager.", USER);
      }

      if (encryptedToken != null) {
        char[] decryptToken = decryptLocal(encryptedToken);
        vaultConfig.setAuthToken(String.valueOf(decryptToken));
      }

      if (encryptedSecretId != null) {
        char[] decryptedSecretId = decryptLocal(encryptedSecretId);
        vaultConfig.setSecretId(String.valueOf(decryptedSecretId));
      }
      boolean isCertValidationRequired = vaultConfig.isCertValidationRequired();
      vaultConfig.setCertValidationRequired(isCertValidationRequired);
    }
    return vaultConfig;
  }

  @Override
  public void renewToken(VaultConfig vaultConfig) {
    String accountId = vaultConfig.getAccountId();
    VaultConfig decryptedVaultConfig = getVaultConfig(accountId, vaultConfig.getUuid());
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    boolean isCertValidationRequired = accountService.isCertValidationRequired(accountId);
    vaultConfig.setCertValidationRequired(isCertValidationRequired);
    delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .renewVaultToken(decryptedVaultConfig);
    wingsPersistence.updateField(
        SecretManagerConfig.class, vaultConfig.getUuid(), VaultConfigKeys.renewedAt, System.currentTimeMillis());
  }

  private String saveSecretField(
      String accountId, String vaultConfigId, String secretValue, String secretNameSuffix, String fieldName) {
    EncryptedData encryptedData = encryptLocal(secretValue.toCharArray());
    // Get by auth token encrypted record by Id or name.
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(SecretManagerConfigKeys.accountId)
                                     .equal(accountId)
                                     .field(EncryptedDataKeys.name)
                                     .equal(vaultConfigId + secretNameSuffix);

    EncryptedData savedEncryptedData = query.get();
    if (savedEncryptedData != null) {
      savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
      savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
      encryptedData = savedEncryptedData;
    }

    encryptedData.setAccountId(accountId);
    encryptedData.addParent(EncryptedDataParent.createParentRef(vaultConfigId, VaultConfig.class, fieldName, VAULT));
    encryptedData.setType(VAULT);
    encryptedData.setName(vaultConfigId + secretNameSuffix);
    return wingsPersistence.save(encryptedData);
  }

  private String updateSecretField(String secretFieldUuid, String accountId, String vaultConfigId, String secretValue,
      String secretNameSuffix, String fieldName) {
    EncryptedData encryptedData = encryptLocal(secretValue.toCharArray());
    // Get by auth token encrypted record by Id or name.
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(SecretManagerConfigKeys.accountId)
                                     .equal(accountId)
                                     .field(ID_KEY)
                                     .equal(secretFieldUuid);

    EncryptedData savedEncryptedData = query.get();
    if (savedEncryptedData == null) {
      throw new UnexpectedException("The vault config is in a bad state. Please contact Harness Support");
    }
    savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
    savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
    savedEncryptedData.setAccountId(accountId);
    savedEncryptedData.addParent(
        EncryptedDataParent.createParentRef(vaultConfigId, VaultConfig.class, fieldName, VAULT));
    savedEncryptedData.setType(VAULT);
    savedEncryptedData.setName(vaultConfigId + secretNameSuffix);
    return wingsPersistence.save(savedEncryptedData);
  }

  @Override
  public void renewAppRoleClientToken(VaultConfig vaultConfig) {
    log.info("Renewing Vault AppRole client token for vault id {}", vaultConfig.getUuid());
    Preconditions.checkNotNull(vaultConfig.getAuthToken());
    VaultConfig decryptedVaultConfig = getVaultConfig(vaultConfig.getAccountId(), vaultConfig.getUuid());
    VaultAppRoleLoginResult loginResult = appRoleLogin(decryptedVaultConfig);
    checkNotNull(loginResult, "Login result during vault appRole login should not be null");
    checkNotNull(loginResult.getClientToken(), "Client token should not be empty");
    log.info("Login result is {} {}", loginResult.getLeaseDuration(), loginResult.getPolicies());
    updateSecretField(vaultConfig.getAuthToken(), vaultConfig.getAccountId(), vaultConfig.getUuid(),
        loginResult.getClientToken(), TOKEN_SECRET_NAME_SUFFIX, VaultConfigKeys.authToken);
    wingsPersistence.updateField(
        SecretManagerConfig.class, vaultConfig.getUuid(), VaultConfigKeys.renewedAt, System.currentTimeMillis());
  }

  private void updateVaultCredentials(VaultConfig savedVaultConfig, String authToken, String secretId) {
    String vaultConfigId = savedVaultConfig.getUuid();
    String accountId = savedVaultConfig.getAccountId();
    String authTokenEncryptedDataId = savedVaultConfig.getAuthToken();
    String secretIdEncryptedDataId = savedVaultConfig.getSecretId();

    // Create a LOCAL encrypted record for Vault authToken
    Preconditions.checkNotNull(authToken);
    Preconditions.checkNotNull(authTokenEncryptedDataId);
    authTokenEncryptedDataId = updateSecretField(authTokenEncryptedDataId, accountId, vaultConfigId, authToken,
        TOKEN_SECRET_NAME_SUFFIX, VaultConfigKeys.authToken);
    savedVaultConfig.setAuthToken(authTokenEncryptedDataId);

    // Create a LOCAL encrypted record for Vault secretId
    if (isNotEmpty(secretId)) {
      if (isNotEmpty(secretIdEncryptedDataId)) {
        secretIdEncryptedDataId = updateSecretField(secretIdEncryptedDataId, accountId, vaultConfigId, secretId,
            SECRET_ID_SECRET_NAME_SUFFIX, VaultConfigKeys.secretId);
      } else {
        secretIdEncryptedDataId =
            saveSecretField(accountId, vaultConfigId, secretId, SECRET_ID_SECRET_NAME_SUFFIX, VaultConfigKeys.secretId);
      }
      savedVaultConfig.setSecretId(secretIdEncryptedDataId);
    }
  }

  private void saveVaultCredentials(VaultConfig savedVaultConfig, String authToken, String secretId) {
    String vaultConfigId = savedVaultConfig.getUuid();
    String accountId = savedVaultConfig.getAccountId();

    // Create a LOCAL encrypted record for Vault authToken
    Preconditions.checkNotNull(authToken);
    String authTokenEncryptedDataId =
        saveSecretField(accountId, vaultConfigId, authToken, TOKEN_SECRET_NAME_SUFFIX, VaultConfigKeys.authToken);
    savedVaultConfig.setAuthToken(authTokenEncryptedDataId);
    // Create a LOCAL encrypted record for Vault secretId
    if (isNotEmpty(secretId)) {
      String secretIdEncryptedDataId =
          saveSecretField(accountId, vaultConfigId, secretId, SECRET_ID_SECRET_NAME_SUFFIX, VaultConfigKeys.secretId);
      savedVaultConfig.setSecretId(secretIdEncryptedDataId);
    }
  }

  String updateVaultConfig(String accountId, VaultConfig vaultConfig, boolean auditChanges, boolean validate) {
    VaultConfig savedVaultConfigWithCredentials = getVaultConfig(accountId, vaultConfig.getUuid());
    VaultConfig oldConfigForAudit = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
    VaultConfig savedVaultConfig = kryoSerializer.clone(oldConfigForAudit);
    // Replaced masked secrets with the real secret value.
    if (SECRET_MASK.equals(vaultConfig.getAuthToken())) {
      vaultConfig.setAuthToken(savedVaultConfigWithCredentials.getAuthToken());
    }
    if (SECRET_MASK.equals(vaultConfig.getSecretId())) {
      vaultConfig.setSecretId(savedVaultConfigWithCredentials.getSecretId());
    }

    boolean credentialChanged =
        !Objects.equals(savedVaultConfigWithCredentials.getAuthToken(), vaultConfig.getAuthToken())
        || !Objects.equals(savedVaultConfigWithCredentials.getSecretId(), vaultConfig.getSecretId());

    // Validate every time when secret manager config change submitted
    if (validate) {
      validateVaultConfig(accountId, vaultConfig);
    }

    if (credentialChanged) {
      updateVaultCredentials(savedVaultConfig, vaultConfig.getAuthToken(), vaultConfig.getSecretId());
    }

    savedVaultConfig.setName(vaultConfig.getName());
    savedVaultConfig.setRenewalInterval(vaultConfig.getRenewalInterval());
    savedVaultConfig.setDefault(vaultConfig.isDefault());
    savedVaultConfig.setReadOnly(vaultConfig.isReadOnly());
    savedVaultConfig.setBasePath(vaultConfig.getBasePath());
    savedVaultConfig.setSecretEngineName(vaultConfig.getSecretEngineName());
    savedVaultConfig.setSecretEngineVersion(vaultConfig.getSecretEngineVersion());
    savedVaultConfig.setAppRoleId(vaultConfig.getAppRoleId());
    savedVaultConfig.setVaultUrl(vaultConfig.getVaultUrl());
    savedVaultConfig.setEngineManuallyEntered(vaultConfig.isEngineManuallyEntered());
    savedVaultConfig.setTemplatizedFields(vaultConfig.getTemplatizedFields());
    savedVaultConfig.setUsageRestrictions(vaultConfig.getUsageRestrictions());
    savedVaultConfig.setScopedToAccount(vaultConfig.isScopedToAccount());
    // PL-3237: Audit secret manager config changes.
    if (auditChanges) {
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedVaultConfig);
    }
    String configId = secretManagerConfigService.save(savedVaultConfig);
    if (isNotEmpty(configId)) {
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.InvalidKMS, getRenewalAlert(oldConfigForAudit));
    }
    return configId;
  }

  private String saveVaultConfig(String accountId, VaultConfig vaultConfig, boolean validate) {
    // Validate every time when secret manager config change submitted
    if (validate) {
      validateVaultConfig(accountId, vaultConfig);
    }
    String authToken = vaultConfig.getAuthToken();
    String secretId = vaultConfig.getSecretId();

    try {
      vaultConfig.setAuthToken(null);
      vaultConfig.setSecretId(null);
      String vaultConfigId = secretManagerConfigService.save(vaultConfig);
      vaultConfig.setUuid(vaultConfigId);
    } catch (DuplicateKeyException e) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Another vault configuration with the same name or URL exists", e, USER_SRE);
    }

    saveVaultCredentials(vaultConfig, authToken, secretId);

    Query<SecretManagerConfig> query =
        wingsPersistence.createQuery(SecretManagerConfig.class).field(ID_KEY).equal(vaultConfig.getUuid());

    UpdateOperations<SecretManagerConfig> updateOperations =
        wingsPersistence.createUpdateOperations(SecretManagerConfig.class)
            .set(VaultConfigKeys.authToken, vaultConfig.getAuthToken());

    if (isNotEmpty(vaultConfig.getSecretId())) {
      updateOperations.set(VaultConfigKeys.secretId, vaultConfig.getSecretId());
    }

    vaultConfig = (VaultConfig) wingsPersistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);

    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, null, vaultConfig);
    return vaultConfig.getUuid();
  }

  @Override
  public String saveOrUpdateVaultConfig(String accountId, VaultConfig vaultConfig, boolean validate) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    // First normalize the base path value. Set default base path if it has not been specified from input.
    String basePath =
        isEmpty(vaultConfig.getBasePath()) ? VaultConfig.DEFAULT_BASE_PATH : vaultConfig.getBasePath().trim();
    vaultConfig.setBasePath(basePath);
    vaultConfig.setAccountId(accountId);

    if (vaultConfig.isReadOnly() && vaultConfig.isDefault()) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "A read only vault cannot be the default secret manager", USER);
    }

    checkIfTemplatizedSecretManagerCanBeCreatedOrUpdated(vaultConfig);

    return isEmpty(vaultConfig.getUuid()) ? saveVaultConfig(accountId, vaultConfig, validate)
                                          : updateVaultConfig(accountId, vaultConfig, true, validate);
  }

  private Optional<String> getTemplatizedField(String templatizedField, VaultConfig vaultConfig) {
    if (templatizedField.equals(VaultConfigKeys.authToken)) {
      return Optional.ofNullable(vaultConfig.getAuthToken());
    } else if (templatizedField.equals(VaultConfigKeys.appRoleId)) {
      return Optional.ofNullable(vaultConfig.getAppRoleId());
    } else if (templatizedField.equals(VaultConfigKeys.secretId)) {
      return Optional.ofNullable(vaultConfig.getSecretId());
    }
    return Optional.empty();
  }

  private void checkIfTemplatizedSecretManagerCanBeCreatedOrUpdated(VaultConfig vaultConfig) {
    if (vaultConfig.isTemplatized()) {
      if (vaultConfig.isDefault()) {
        throw new InvalidRequestException("Cannot set a templatized secret manager as default");
      }

      for (String templatizedField : vaultConfig.getTemplatizedFields()) {
        Optional<String> requiredField = getTemplatizedField(templatizedField, vaultConfig);
        if (!requiredField.isPresent() || SECRET_MASK.equals(requiredField.get())) {
          throw new InvalidRequestException("Invalid value provided for templatized field: " + templatizedField);
        }
      }
    }
  }

  @Override
  public boolean deleteVaultConfig(String accountId, String vaultConfigId) {
    long count = wingsPersistence.createQuery(EncryptedData.class)
                     .filter(SecretManagerConfigKeys.accountId, accountId)
                     .filter(EncryptedDataKeys.kmsId, vaultConfigId)
                     .filter(EncryptedDataKeys.encryptionType, EncryptionType.VAULT)
                     .count(upToOne);

    if (count > 0) {
      String message = "Cannot delete the vault configuration since there are secrets encrypted with it. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }

    VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    checkNotNull(vaultConfig, "No vault config found with id " + vaultConfigId);

    if (isNotEmpty(vaultConfig.getAuthToken())) {
      wingsPersistence.delete(EncryptedData.class, vaultConfig.getAuthToken());
      log.info("Deleted encrypted auth token record {} associated with vault secret manager '{}'",
          vaultConfig.getAuthToken(), vaultConfig.getName());
    }
    if (isNotEmpty(vaultConfig.getSecretId())) {
      wingsPersistence.delete(EncryptedData.class, vaultConfig.getSecretId());
      log.info("Deleted encrypted secret id record {} associated with vault secret manager '{}'",
          vaultConfig.getSecretId(), vaultConfig.getName());
    }

    return deleteSecretManagerAndGenerateAudit(accountId, vaultConfig);
  }

  private List<SecretEngineSummary> listSecretEnginesInternal(VaultConfig vaultConfig) {
    // HAR-7605: Shorter timeout for decryption tasks, and it should retry on timeout or failure.
    int failedAttempts = 0;
    boolean isCertValidationRequired = accountService.isCertValidationRequired(vaultConfig.getAccountId());
    vaultConfig.setCertValidationRequired(isCertValidationRequired);
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(vaultConfig.getAccountId())
                                              .timeout(Duration.ofSeconds(10).toMillis())
                                              .appId(GLOBAL_APP_ID)
                                              .correlationId(vaultConfig.getUuid())
                                              .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .listSecretEngines(vaultConfig);
      } catch (WingsException e) {
        failedAttempts++;
        log.info("Vault Decryption failed for list secret engines for Vault serverer {}. trial num: {}",
            vaultConfig.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public List<SecretEngineSummary> listSecretEngines(VaultConfig vaultConfig) {
    if (isNotEmpty(vaultConfig.getUuid())) {
      VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
      decryptVaultConfigSecrets(vaultConfig.getAccountId(), savedVaultConfig, false);
      if (SecretString.SECRET_MASK.equals(vaultConfig.getAuthToken())) {
        vaultConfig.setAuthToken(savedVaultConfig.getAuthToken());
      }
      if (SecretString.SECRET_MASK.equals(vaultConfig.getSecretId())) {
        vaultConfig.setSecretId(savedVaultConfig.getSecretId());
      }
    }
    return listSecretEnginesInternal(vaultConfig);
  }

  @Override
  public void decryptVaultConfigSecrets(String accountId, VaultConfig vaultConfig, boolean maskSecret) {
    if (maskSecret) {
      vaultConfig.maskSecrets();
    } else {
      EncryptedData tokenData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      EncryptedData secretIdData = wingsPersistence.get(EncryptedData.class, vaultConfig.getSecretId());
      if (tokenData == null && secretIdData == null) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
            "Either auth token or secret Id field needs to be present for vault secret manager.", USER);
      }

      if (tokenData != null) {
        char[] decryptedToken = decryptLocal(tokenData);
        vaultConfig.setAuthToken(String.valueOf(decryptedToken));
      }
      if (secretIdData != null) {
        char[] decryptedSecretId = decryptLocal(secretIdData);
        vaultConfig.setSecretId(String.valueOf(decryptedSecretId));
      }
    }
  }

  @Override
  public List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedData encryptedData, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(vaultConfig.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    boolean isCertValidationRequired = accountService.isCertValidationRequired(vaultConfig.getAccountId());
    vaultConfig.setCertValidationRequired(isCertValidationRequired);
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .getVaultSecretChangeLogs(encryptedData, vaultConfig);
  }

  public void validateVaultConfig(String accountId, VaultConfig vaultConfig) {
    if (isEmpty(vaultConfig.getSecretEngineName()) || vaultConfig.getSecretEngineVersion() == 0) {
      throw new SecretManagementException(
          VAULT_OPERATION_ERROR, "Secret engine or secret engine version was not specified", USER);
    }
    // Need to try using Vault AppRole login to generate a client token if configured so
    if (vaultConfig.getAccessType() == APP_ROLE) {
      VaultAppRoleLoginResult loginResult = appRoleLogin(vaultConfig);
      if (loginResult != null && EmptyPredicate.isNotEmpty(loginResult.getClientToken())) {
        vaultConfig.setAuthToken(loginResult.getClientToken());
      } else {
        String message =
            "Was not able to login Vault using the AppRole auth method. Please check your credentials and try again";
        throw new SecretManagementException(VAULT_OPERATION_ERROR, message, USER);
      }
    }
    if (!vaultConfig.isReadOnly()) {
      vaultEncryptorsRegistry.getVaultEncryptor(EncryptionType.VAULT)
          .createSecret(accountId, VaultConfig.VAULT_VAILDATION_URL, Boolean.TRUE.toString(), vaultConfig);
    }
  }

  public VaultAppRoleLoginResult appRoleLogin(VaultConfig vaultConfig) {
    int failedAttempts = 0;
    boolean isCertValidationRequired = accountService.isCertValidationRequired(vaultConfig.getAccountId());
    vaultConfig.setCertValidationRequired(isCertValidationRequired);
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(vaultConfig.getAccountId())
                                              .timeout(Duration.ofSeconds(5).toMillis())
                                              .appId(vaultConfig.getAccountId())
                                              .correlationId(vaultConfig.getUuid())
                                              .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .appRoleLogin(vaultConfig);
      } catch (WingsException e) {
        failedAttempts++;
        log.info("Vault AppRole login failed Vault server {}. trial num: {}", vaultConfig.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public Optional<SecretManagerConfig> updateRuntimeCredentials(
      SecretManagerConfig secretManagerConfig, Map<String, String> runtimeParameters, boolean shouldUpdateVaultConfig) {
    if (isEmpty(secretManagerConfig.getTemplatizedFields()) || isEmpty(runtimeParameters)) {
      return Optional.empty();
    }

    VaultConfig vaultConfig = (VaultConfig) kryoSerializer.clone(secretManagerConfig);
    for (String templatizedField : secretManagerConfig.getTemplatizedFields()) {
      String templatizedFieldValue = runtimeParameters.get(templatizedField);
      if (StringUtils.isEmpty(templatizedFieldValue)) {
        return Optional.empty();
      }
      if (templatizedField.equals(VaultConfigKeys.appRoleId)) {
        vaultConfig.setAppRoleId(templatizedFieldValue);
      } else if (templatizedField.equals(VaultConfigKeys.secretId)) {
        vaultConfig.setSecretId(templatizedFieldValue);
      } else if (templatizedField.equals(VaultConfigKeys.authToken)) {
        vaultConfig.setAuthToken(templatizedFieldValue);
      }
    }
    if (shouldUpdateVaultConfig) {
      updateVaultConfig(vaultConfig.getAccountId(), vaultConfig, false, true);
    } else if (secretManagerConfig.getTemplatizedFields().contains(VaultConfigKeys.appRoleId)
        || secretManagerConfig.getTemplatizedFields().contains(VaultConfigKeys.secretId)) {
      VaultAppRoleLoginResult loginResult = appRoleLogin(vaultConfig);
      vaultConfig.setAuthToken(loginResult.getClientToken());
    }
    return Optional.of(vaultConfig);
  }

  public KmsSetupAlert getRenewalAlert(VaultConfig vaultConfig) {
    return KmsSetupAlert.builder()
        .kmsId(vaultConfig.getUuid())
        .message(vaultConfig.getName()
            + "(Hashicorp Vault) is not able to renew the token. Please check your setup and ensure that token is renewable")
        .build();
  }
}
