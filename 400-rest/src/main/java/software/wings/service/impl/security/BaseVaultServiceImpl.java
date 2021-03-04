package software.wings.service.impl.security;

import static io.harness.beans.EncryptedData.EncryptedDataKeys;
import static io.harness.beans.SecretManagerConfig.SecretManagerConfigKeys;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.BaseVaultConfig.BaseVaultConfigKeys;
import static software.wings.settings.SettingVariableTypes.VAULT;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.beans.EncryptedData;
import io.harness.beans.EncryptedDataParent;
import io.harness.beans.SecretManagerConfig;
import io.harness.exception.SecretManagementException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.helpers.ext.vault.VaultAppRoleLoginResult;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.BaseVaultConfig;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingVariableTypes;

import java.time.Duration;
import java.util.List;

@Slf4j
public class BaseVaultServiceImpl extends AbstractSecretServiceImpl {
  private static final int NUM_OF_RETRIES = 3;
  private static final String TOKEN_SECRET_NAME_SUFFIX = "_token";
  private static final String SECRET_ID_SECRET_NAME_SUFFIX = "_secret_id";

  @Inject private AccountService accountService;

  protected boolean deleteVaultConfigInternal(String accountId, String vaultConfigId, long count) {
    if (count > 0) {
      String message = "Cannot delete the vault configuration since there are secrets encrypted with it. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }

    BaseVaultConfig baseVaultConfig = wingsPersistence.get(BaseVaultConfig.class, vaultConfigId);
    checkNotNull(baseVaultConfig, "No SSH vault config found with id " + vaultConfigId);

    if (isNotEmpty(baseVaultConfig.getAuthToken())) {
      wingsPersistence.delete(EncryptedData.class, baseVaultConfig.getAuthToken());
      log.info("Deleted encrypted auth token record {} associated with SSH vault secret engine '{}'",
          baseVaultConfig.getAuthToken(), baseVaultConfig.getName());
    }
    if (isNotEmpty(baseVaultConfig.getSecretId())) {
      wingsPersistence.delete(EncryptedData.class, baseVaultConfig.getSecretId());
      log.info("Deleted encrypted secret id record {} associated with SSH vault secret engine '{}'",
          baseVaultConfig.getSecretId(), baseVaultConfig.getName());
    }

    return deleteSecretManagerAndGenerateAudit(accountId, baseVaultConfig);
  }

  protected BaseVaultConfig getVaultConfigInternal(Query<BaseVaultConfig> query) {
    BaseVaultConfig baseVaultConfig = query.get();

    if (baseVaultConfig != null) {
      EncryptedData encryptedToken = wingsPersistence.get(EncryptedData.class, baseVaultConfig.getAuthToken());
      EncryptedData encryptedSecretId = wingsPersistence.get(EncryptedData.class, baseVaultConfig.getSecretId());
      if (encryptedToken == null && encryptedSecretId == null) {
        throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
            "Either auth token or secret Id field needs to be present for vault secret manager.", USER);
      }

      if (encryptedToken != null) {
        char[] decryptToken = decryptLocal(encryptedToken);
        baseVaultConfig.setAuthToken(String.valueOf(decryptToken));
      }

      if (encryptedSecretId != null) {
        char[] decryptedSecretId = decryptLocal(encryptedSecretId);
        baseVaultConfig.setSecretId(String.valueOf(decryptedSecretId));
      }
      boolean isCertValidationRequired = baseVaultConfig.isCertValidationRequired();
      baseVaultConfig.setCertValidationRequired(isCertValidationRequired);
    }
    return baseVaultConfig;
  }

  public KmsSetupAlert getRenewalAlert(BaseVaultConfig baseVaultConfig) {
    return KmsSetupAlert.builder()
        .kmsId(baseVaultConfig.getUuid())
        .message(baseVaultConfig.getName()
            + "(Hashicorp Vault) is not able to renew the token. Please check your setup and ensure that token is renewable")
        .build();
  }

  public VaultAppRoleLoginResult appRoleLogin(BaseVaultConfig baseVaultConfig) {
    int failedAttempts = 0;
    boolean isCertValidationRequired = accountService.isCertValidationRequired(baseVaultConfig.getAccountId());
    baseVaultConfig.setCertValidationRequired(isCertValidationRequired);
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
            .accountId(baseVaultConfig.getAccountId())
            .timeout(Duration.ofSeconds(5).toMillis())
            .appId(baseVaultConfig.getAccountId())
            .correlationId(baseVaultConfig.getUuid())
            .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .appRoleLogin(baseVaultConfig);
      } catch (WingsException e) {
        failedAttempts++;
        log.warn(
            "Vault AppRole login failed Vault server {}. trial num: {}", baseVaultConfig.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  public void renewToken(BaseVaultConfig vaultConfig) {
    String accountId = vaultConfig.getAccountId();
    VaultConfig decryptedVaultConfig = getVaultConfig(accountId, vaultConfig.getUuid());
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    boolean isCertValidationRequired = accountService.isCertValidationRequired(accountId);
    vaultConfig.setCertValidationRequired(isCertValidationRequired);
    delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .renewVaultToken(decryptedVaultConfig);
    wingsPersistence.updateField(
        SecretManagerConfig.class, vaultConfig.getUuid(), BaseVaultConfigKeys.renewedAt, System.currentTimeMillis());
  }

  public VaultConfig getVaultConfig(String accountId, String entityId) {
    if (isEmpty(accountId) || isEmpty(entityId)){
      return new VaultConfig();
    }
    Query<BaseVaultConfig> query = wingsPersistence.createQuery(BaseVaultConfig.class)
        .filter(SecretManagerConfigKeys.accountId, accountId)
        .filter(ID_KEY, entityId);
    return (VaultConfig) getVaultConfigInternal(query);
  }

  public void renewAppRoleClientToken(BaseVaultConfig vaultConfig) {
    log.info("Renewing Vault AppRole client token for vault id {}", vaultConfig.getUuid());
    Preconditions.checkNotNull(vaultConfig.getAuthToken());
    VaultConfig decryptedVaultConfig = getVaultConfig(vaultConfig.getAccountId(), vaultConfig.getUuid());
    VaultAppRoleLoginResult loginResult = appRoleLogin(decryptedVaultConfig);
    checkNotNull(loginResult, "Login result during vault appRole login should not be null");
    checkNotNull(loginResult.getClientToken(), "Client token should not be empty");
    log.info("Login result is {} {}", loginResult.getLeaseDuration(), loginResult.getPolicies());
    updateSecretField(vaultConfig.getAuthToken(), vaultConfig.getAccountId(), vaultConfig.getUuid(),
        loginResult.getClientToken(), TOKEN_SECRET_NAME_SUFFIX, BaseVaultConfigKeys.authToken, VAULT);
    wingsPersistence.updateField(
        SecretManagerConfig.class, vaultConfig.getUuid(), BaseVaultConfigKeys.renewedAt, System.currentTimeMillis());
  }

  private String saveSecretField(String accountId, String vaultConfigId, String secretValue, String secretNameSuffix,
      String fieldName, SettingVariableTypes settingVariableTypes) {
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
    encryptedData.addParent(
        EncryptedDataParent.createParentRef(vaultConfigId, SSHVaultConfig.class, fieldName, settingVariableTypes));
    encryptedData.setType(settingVariableTypes);
    encryptedData.setName(vaultConfigId + secretNameSuffix);
    encryptedData.setKmsId(accountId);
    return wingsPersistence.save(encryptedData);
  }

  protected void saveVaultCredentials(
      BaseVaultConfig savedVaultConfig, String authToken, String secretId, SettingVariableTypes settingVariableTypes) {
    String vaultConfigId = savedVaultConfig.getUuid();
    String accountId = savedVaultConfig.getAccountId();

    // Create a LOCAL encrypted record for Vault authToken
    Preconditions.checkNotNull(authToken);
    String authTokenEncryptedDataId = saveSecretField(accountId, vaultConfigId, authToken, TOKEN_SECRET_NAME_SUFFIX,
        BaseVaultConfigKeys.authToken, settingVariableTypes);
    savedVaultConfig.setAuthToken(authTokenEncryptedDataId);
    // Create a LOCAL encrypted record for Vault secretId
    if (isNotEmpty(secretId)) {
      String secretIdEncryptedDataId = saveSecretField(accountId, vaultConfigId, secretId, SECRET_ID_SECRET_NAME_SUFFIX,
          BaseVaultConfigKeys.secretId, settingVariableTypes);
      savedVaultConfig.setSecretId(secretIdEncryptedDataId);
    }
  }

  protected String updateSecretField(String secretFieldUuid, String accountId, String vaultConfigId, String secretValue,
      String secretNameSuffix, String fieldName, SettingVariableTypes settingVariableTypes) {
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
        EncryptedDataParent.createParentRef(vaultConfigId, VaultConfig.class, fieldName, settingVariableTypes));
    savedEncryptedData.setType(settingVariableTypes);
    savedEncryptedData.setName(vaultConfigId + secretNameSuffix);
    savedEncryptedData.setKmsId(accountId);
    return wingsPersistence.save(savedEncryptedData);
  }

  protected void updateVaultCredentials(
      BaseVaultConfig savedVaultConfig, String authToken, String secretId, SettingVariableTypes settingVariableTypes) {
    String vaultConfigId = savedVaultConfig.getUuid();
    String accountId = savedVaultConfig.getAccountId();
    String authTokenEncryptedDataId = savedVaultConfig.getAuthToken();
    String secretIdEncryptedDataId = savedVaultConfig.getSecretId();

    // Create a LOCAL encrypted record for Vault authToken
    Preconditions.checkNotNull(authToken);
    Preconditions.checkNotNull(authTokenEncryptedDataId);
    authTokenEncryptedDataId = updateSecretField(authTokenEncryptedDataId, accountId, vaultConfigId, authToken,
        TOKEN_SECRET_NAME_SUFFIX, BaseVaultConfigKeys.authToken, settingVariableTypes);
    savedVaultConfig.setAuthToken(authTokenEncryptedDataId);

    // Create a LOCAL encrypted record for Vault secretId
    if (isNotEmpty(secretId)) {
      if (isNotEmpty(secretIdEncryptedDataId)) {
        secretIdEncryptedDataId = updateSecretField(secretIdEncryptedDataId, accountId, vaultConfigId, secretId,
            SECRET_ID_SECRET_NAME_SUFFIX, BaseVaultConfigKeys.secretId, settingVariableTypes);
      } else {
        secretIdEncryptedDataId = saveSecretField(accountId, vaultConfigId, secretId, SECRET_ID_SECRET_NAME_SUFFIX,
            BaseVaultConfigKeys.secretId, settingVariableTypes);
      }
      savedVaultConfig.setSecretId(secretIdEncryptedDataId);
    }
  }

  void decryptVaultConfigSecretsInternal(BaseVaultConfig vaultConfig, boolean maskSecret) {
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

  List<SecretEngineSummary> listSecretEnginesInternal(BaseVaultConfig vaultConfig) {
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
        log.warn("Vault Decryption failed for list secret engines for Vault serverer {}. trial num: {}",
            vaultConfig.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }
}