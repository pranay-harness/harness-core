package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.SECRET_NAME_KEY;

import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.exception.WingsException;
import io.harness.expression.SecretString;
import io.harness.persistence.HIterator;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.impl.security.vault.SecretEngineSummary;
import software.wings.service.impl.security.vault.VaultAppRoleLoginResult;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 11/2/17.
 */
@Singleton
@Slf4j
public class VaultServiceImpl extends AbstractSecretServiceImpl implements VaultService {
  private static final String TOKEN_SECRET_NAME_SUFFIX = "_token";
  private static final String SECRET_ID_SECRET_NAME_SUFFIX = "_secret_id";

  @Inject private AlertService alertService;

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData) {
    if (vaultConfig.isReadOnly() && (encryptedData == null || isEmpty(encryptedData.getPath()))) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          "Cannot use a read only vault to add an inline encrypted text or file. Add the secret in vault and refer it from here",
          USER);
    }

    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return (EncryptedData) delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .encrypt(name, value, accountId, settingType, vaultConfig, encryptedData);
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig) {
    // HAR-7605: Shorter timeout for decryption tasks, and it should retry on timeout or failure.
    int failedAttempts = 0;
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(accountId)
                                              .timeout(Duration.ofSeconds(5).toMillis())
                                              .appId(GLOBAL_APP_ID)
                                              .correlationId(data.getName())
                                              .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .decrypt(data, vaultConfig);
      } catch (WingsException e) {
        failedAttempts++;
        logger.info("Vault Decryption failed for encryptedData {}. trial num: {}", data.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public VaultConfig getVaultConfig(String accountId, String entityId) {
    Query<VaultConfig> query =
        wingsPersistence.createQuery(VaultConfig.class).filter(ACCOUNT_ID_KEY, accountId).filter(ID_KEY, entityId);
    return getVaultConfigInternal(query);
  }

  @Override
  public boolean isReadOnly(String configId) {
    VaultConfig vaultConfig = (VaultConfig) wingsPersistence.get(SecretManagerConfig.class, configId);
    if (vaultConfig == null) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Vault config not found", USER);
    }
    return vaultConfig.isReadOnly();
  }

  @Override
  public VaultConfig getVaultConfigByName(String accountId, String name) {
    Query<VaultConfig> query = wingsPersistence.createQuery(VaultConfig.class)
                                   .filter(ACCOUNT_ID_KEY, accountId)
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
        char[] decryptToken = decryptVaultToken(encryptedToken);
        vaultConfig.setAuthToken(String.valueOf(decryptToken));
      }

      if (encryptedSecretId != null) {
        char[] decryptedSecretId = decryptVaultToken(encryptedSecretId);
        vaultConfig.setSecretId(String.valueOf(decryptedSecretId));
      }
    }

    return vaultConfig;
  }

  @Override
  public void renewTokens(String accountId) {
    long currentTime = System.currentTimeMillis();
    logger.info("renewing vault token for {}", accountId);
    try (HIterator<SecretManagerConfig> query = new HIterator<>(
             wingsPersistence.createQuery(SecretManagerConfig.class).filter(ACCOUNT_ID_KEY, accountId).fetch())) {
      for (SecretManagerConfig secretManagerConfig : query) {
        if (!(secretManagerConfig instanceof VaultConfig)) {
          continue;
        }

        VaultConfig vaultConfig = (VaultConfig) secretManagerConfig;
        // don't renew if renewal interval not configured
        if (vaultConfig.getRenewIntervalHours() <= 0) {
          logger.info("renewing not configured for {} for account {}", vaultConfig.getUuid(), accountId);
          continue;
        }
        // don't renew if renewed within configured time
        if (TimeUnit.MILLISECONDS.toHours(currentTime - vaultConfig.getRenewedAt())
            < vaultConfig.getRenewIntervalHours()) {
          logger.info("{} renewed at {} not renewing now for account {}", vaultConfig.getUuid(),
              new Date(vaultConfig.getRenewedAt()), accountId);
          continue;
        }

        VaultConfig decryptedVaultConfig = getVaultConfig(accountId, vaultConfig.getUuid());
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(accountId)
                                              .appId(GLOBAL_APP_ID)
                                              .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                              .build();
        KmsSetupAlert kmsSetupAlert =
            KmsSetupAlert.builder()
                .kmsId(vaultConfig.getUuid())
                .message(vaultConfig.getName()
                    + "(Hashicorp Vault) is not able to renew the token. Please check your setup and ensure that token is renewable")
                .build();
        try {
          delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
              .renewVaultToken(decryptedVaultConfig);
          alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
          vaultConfig.setRenewedAt(System.currentTimeMillis());
          wingsPersistence.save(vaultConfig);
        } catch (Exception e) {
          logger.info("Error while renewing token for : " + vaultConfig, e);
          alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
        }
      }
    }
  }

  @Override
  public void renewAppRoleClientToken(String accountId) {
    logger.info("Renewing Vault AppRole client token for {}", accountId);
    try (HIterator<SecretManagerConfig> query = new HIterator<>(
             wingsPersistence.createQuery(SecretManagerConfig.class).filter(ACCOUNT_ID_KEY, accountId).fetch())) {
      for (SecretManagerConfig secretManagerConfig : query) {
        if (!(secretManagerConfig instanceof VaultConfig)) {
          continue;
        }

        VaultConfig encryptedVaultConfig = (VaultConfig) secretManagerConfig;
        if (isNotEmpty(encryptedVaultConfig.getAppRoleId())) {
          VaultConfig decryptedVaultConfig = getVaultConfig(accountId, encryptedVaultConfig.getUuid());
          try {
            VaultAppRoleLoginResult loginResult = appRoleLogin(decryptedVaultConfig);
            if (loginResult != null && isNotEmpty(loginResult.getClientToken())) {
              logger.info("Login result is {} {}", loginResult.getLeaseDuration(), loginResult.getPolicies());
              encryptedVaultConfig.setRenewedAt(System.currentTimeMillis());
              saveSecretField(encryptedVaultConfig, encryptedVaultConfig, encryptedVaultConfig.getUuid(),
                  loginResult.getClientToken(), TOKEN_SECRET_NAME_SUFFIX);
              wingsPersistence.save(encryptedVaultConfig);
            }
          } catch (Exception e) {
            logger.info("Error while renewing client token for Vault AppRole  " + encryptedVaultConfig.getAppRoleId()
                    + " in secret manager " + encryptedVaultConfig.getName(),
                e);
          }
        }
      }
    }
  }

  @Override
  public String saveVaultConfig(String accountId, VaultConfig vaultConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);

    // First normalize the base path value. Set default base path if it has not been specified from input.
    String basePath = isEmpty(vaultConfig.getBasePath()) ? DEFAULT_BASE_PATH : vaultConfig.getBasePath().trim();
    vaultConfig.setBasePath(basePath);
    vaultConfig.setAccountId(accountId);

    VaultConfig oldConfigForAudit = null;
    VaultConfig savedVaultConfig = null;
    boolean credentialChanged = true;
    if (!isEmpty(vaultConfig.getUuid())) {
      savedVaultConfig = getVaultConfig(accountId, vaultConfig.getUuid());
      // Replaced masked secrets with the real secret value.
      if (SECRET_MASK.equals(vaultConfig.getAuthToken())) {
        vaultConfig.setAuthToken(savedVaultConfig.getAuthToken());
      }
      if (SECRET_MASK.equals(vaultConfig.getSecretId())) {
        vaultConfig.setSecretId(savedVaultConfig.getSecretId());
      }
      credentialChanged = !savedVaultConfig.getVaultUrl().equals(vaultConfig.getVaultUrl())
          || !Objects.equals(savedVaultConfig.getAppRoleId(), vaultConfig.getAppRoleId())
          || !Objects.equals(savedVaultConfig.getAuthToken(), vaultConfig.getAuthToken())
          || !Objects.equals(savedVaultConfig.getSecretId(), vaultConfig.getSecretId());

      // secret field un-decrypted version of saved KMS config
      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
      oldConfigForAudit = KryoUtils.clone(savedVaultConfig);
    }

    if (vaultConfig.isReadOnly() && vaultConfig.isDefault()) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "A read only vault cannot be the default secret manager", USER);
    }

    // Validate every time when secret manager config change submitted
    validateVaultConfig(accountId, vaultConfig);

    if (!credentialChanged) {
      // update without token/secretId or url changes
      savedVaultConfig.setName(vaultConfig.getName());
      savedVaultConfig.setRenewIntervalHours(vaultConfig.getRenewIntervalHours());
      savedVaultConfig.setDefault(vaultConfig.isDefault());
      savedVaultConfig.setReadOnly(vaultConfig.isReadOnly());
      savedVaultConfig.setBasePath(vaultConfig.getBasePath());
      savedVaultConfig.setSecretEngineName(vaultConfig.getSecretEngineName());
      savedVaultConfig.setSecretEngineVersion(vaultConfig.getSecretEngineVersion());
      savedVaultConfig.setAppRoleId(vaultConfig.getAppRoleId());
      // PL-3237: Audit secret manager config changes.
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedVaultConfig);

      return secretManagerConfigService.save(savedVaultConfig);
    }

    String authToken = vaultConfig.getAuthToken();
    String secretId = vaultConfig.getSecretId();

    String vaultConfigId;
    try {
      vaultConfig.setAuthToken(null);
      vaultConfig.setSecretId(null);
      vaultConfigId = secretManagerConfigService.save(vaultConfig);
    } catch (DuplicateKeyException e) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Another vault configuration with the same name or URL exists", USER_SRE);
    }

    // Create a LOCAL encrypted record for Vault authToken
    if (isNotEmpty(authToken) && !SECRET_MASK.equals(authToken)) {
      String authTokenEncryptedDataId =
          saveSecretField(savedVaultConfig, vaultConfig, vaultConfigId, authToken, TOKEN_SECRET_NAME_SUFFIX);
      vaultConfig.setAuthToken(authTokenEncryptedDataId);
    }

    // Create a LOCAL encrypted record for Vault secretId
    if (isNotEmpty(secretId) && !SECRET_MASK.equals(secretId)) {
      String secretIdEncryptedDataId =
          saveSecretField(savedVaultConfig, vaultConfig, vaultConfigId, secretId, SECRET_ID_SECRET_NAME_SUFFIX);
      vaultConfig.setSecretId(secretIdEncryptedDataId);
    }

    // PL-3237: Audit secret manager config changes.
    generateAuditForSecretManager(accountId, oldConfigForAudit, vaultConfig);

    secretManagerConfigService.save(vaultConfig);
    return vaultConfigId;
  }

  private String saveSecretField(VaultConfig savedVaultConfig, VaultConfig vaultConfig, String vaultConfigId,
      String secretValue, String secretNameSuffix) {
    EncryptedData encryptedData = encryptLocal(secretValue.toCharArray());
    if (savedVaultConfig != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(ACCOUNT_ID_KEY)
          .equal(vaultConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(savedVaultConfig.getAuthToken()),
              query.criteria(SECRET_NAME_KEY).equal(savedVaultConfig.getName() + secretNameSuffix));
      EncryptedData savedEncryptedData = query.get();
      if (savedEncryptedData != null) {
        savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
        savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
        encryptedData = savedEncryptedData;
      }
    }

    encryptedData.setAccountId(vaultConfig.getAccountId());
    encryptedData.addParent(vaultConfigId);
    encryptedData.setType(SettingVariableTypes.VAULT);
    encryptedData.setName(vaultConfig.getName() + secretNameSuffix);
    return wingsPersistence.save(encryptedData);
  }

  @Override
  public boolean deleteVaultConfig(String accountId, String vaultConfigId) {
    final long count = wingsPersistence.createQuery(EncryptedData.class)
                           .filter(ACCOUNT_ID_KEY, accountId)
                           .filter(EncryptedDataKeys.kmsId, vaultConfigId)
                           .filter(EncryptedDataKeys.encryptionType, EncryptionType.VAULT)
                           .count(new CountOptions().limit(1));

    if (count > 0) {
      String message = "Can not delete the vault configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to another secret manager and try again.";
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
    }

    VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    checkNotNull(vaultConfig, "No vault config found with id " + vaultConfigId);

    if (isNotEmpty(vaultConfig.getAuthToken())) {
      wingsPersistence.delete(EncryptedData.class, vaultConfig.getAuthToken());
      logger.info("Deleted encrypted auth token record {} associated with vault secret manager '{}'",
          vaultConfig.getAuthToken(), vaultConfig.getName());
    }
    if (isNotEmpty(vaultConfig.getSecretId())) {
      wingsPersistence.delete(EncryptedData.class, vaultConfig.getSecretId());
      logger.info("Deleted encrypted secret id record {} associated with vault secret manager '{}'",
          vaultConfig.getSecretId(), vaultConfig.getName());
    }

    return deleteSecretManagerAndGenerateAudit(accountId, vaultConfig);
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

    // HAR-7605: Shorter timeout for decryption tasks, and it should retry on timeout or failure.
    int failedAttempts = 0;
    while (true) {
      try {
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(vaultConfig.getAccountId())
                                              .timeout(Duration.ofSeconds(5).toMillis())
                                              .appId(GLOBAL_APP_ID)
                                              .correlationId(vaultConfig.getUuid())
                                              .build();
        return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
            .listSecretEngines(vaultConfig);
      } catch (WingsException e) {
        failedAttempts++;
        logger.info("Vault Decryption failed for list secret engines for Vault serverer {}. trial num: {}",
            vaultConfig.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
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
        char[] decryptedToken = decryptVaultToken(tokenData);
        vaultConfig.setAuthToken(String.valueOf(decryptedToken));
      }
      if (secretIdData != null) {
        char[] decryptedSecretId = decryptVaultToken(secretIdData);
        vaultConfig.setSecretId(String.valueOf(decryptedSecretId));
      }
    }
  }

  @Override
  public EncryptedData encryptFile(
      String accountId, VaultConfig vaultConfig, String name, byte[] inputBytes, EncryptedData savedEncryptedData) {
    checkNotNull(vaultConfig, "Vault configuration can't be null");
    byte[] bytes = encodeBase64ToByteArray(inputBytes);
    EncryptedData fileData = encrypt(name, new String(CHARSET.decode(ByteBuffer.wrap(bytes)).array()), accountId,
        SettingVariableTypes.CONFIG_FILE, vaultConfig, savedEncryptedData);
    fileData.setAccountId(accountId);
    fileData.setName(name);
    fileData.setType(SettingVariableTypes.CONFIG_FILE);
    fileData.setBase64Encoded(true);
    fileData.setFileSize(inputBytes.length);
    return fileData;
  }

  @Override
  public File decryptFile(File file, String accountId, EncryptedData encryptedData) {
    try {
      VaultConfig vaultConfig = getVaultConfig(accountId, encryptedData.getKmsId());
      checkNotNull(vaultConfig, "Vault configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      char[] decrypt = decrypt(encryptedData, accountId, vaultConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      Files.write(fileData, file);
      return file;
    } catch (IOException ioe) {
      throw new SecretManagementException(
          VAULT_OPERATION_ERROR, "Failed to decrypt data into an output file", ioe, USER);
    }
  }

  @Override
  public void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      VaultConfig vaultConfig = getVaultConfig(accountId, encryptedData.getKmsId());
      checkNotNull(vaultConfig, "Vault configuration can't be null");
      checkNotNull(encryptedData, "Encrypted data record can't be null");
      char[] decrypt = decrypt(encryptedData, accountId, vaultConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new SecretManagementException(
          VAULT_OPERATION_ERROR, "Failed to decrypt data into an output stream", ioe, USER);
    }
  }

  @Override
  public void deleteSecret(String accountId, String path, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .deleteVaultSecret(path, vaultConfig);
  }

  @Override
  public List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedData encryptedData, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(vaultConfig.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .getVaultSecretChangeLogs(encryptedData, vaultConfig);
  }

  private void validateVaultConfig(String accountId, VaultConfig vaultConfig) {
    try {
      if (vaultConfig.getSecretEngineVersion() == 0) {
        SecretEngineSummary secretEngine = getVaultSecretEngine(vaultConfig);
        if (secretEngine == null) {
          String message = "Was not able to find the default or matching backend Vault secret engine.";
          throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, message, USER);
        } else {
          // Default to secret engine kv version 2
          int secreteEngineVersion = secretEngine.getVersion() != null ? secretEngine.getVersion() : 2;

          vaultConfig.setSecretEngineVersion(secreteEngineVersion);
          vaultConfig.setSecretEngineName(secretEngine.getName());
          logger.info("Backend secret engine name and version for Vault secret manager {} are: [{}, {}]",
              vaultConfig.getName(), secretEngine.getName(), secreteEngineVersion);
        }
      }

      // Need to try using Vault AppRole login to generate a client token if configured so
      if (isNotEmpty(vaultConfig.getAppRoleId())) {
        VaultAppRoleLoginResult loginResult = appRoleLogin(vaultConfig);
        if (loginResult == null) {
          String message =
              "Was not able to login Vault using the AppRole auth method. Please check your credentials and try again";
          throw new SecretManagementException(VAULT_OPERATION_ERROR, message, USER);
        } else {
          vaultConfig.setAuthToken(loginResult.getClientToken());
        }
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String message =
          "Was not able to determine the vault server's secret engine version using given credentials. Please check your credentials and try again";
      throw new SecretManagementException(VAULT_OPERATION_ERROR, message, e, USER);
    }

    if (!vaultConfig.isReadOnly()) {
      try {
        encrypt(
            VAULT_VAILDATION_URL, Boolean.TRUE.toString(), accountId, SettingVariableTypes.VAULT, vaultConfig, null);
      } catch (WingsException e) {
        String message =
            "Was not able to reach vault using given credentials. Please check your credentials and try again";
        throw new SecretManagementException(VAULT_OPERATION_ERROR, message, e, USER);
      }
    }
  }

  public VaultAppRoleLoginResult appRoleLogin(VaultConfig vaultConfig) {
    int failedAttempts = 0;
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
        logger.info(
            "Vault AppRole login failed Vault server {}. trial num: {}", vaultConfig.getName(), failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw e;
        }
        sleep(ofMillis(1000));
      }
    }
  }

  private SecretEngineSummary getVaultSecretEngine(VaultConfig vaultConfig) {
    List<SecretEngineSummary> secretEngineSummaries = listSecretEngines(vaultConfig);

    return getSecretEngineWithFallback(vaultConfig, secretEngineSummaries);
  }

  private SecretEngineSummary getSecretEngineWithFallback(
      VaultConfig vaultConfig, List<SecretEngineSummary> secretEngineSummaries) {
    String mountPointName = isEmpty(vaultConfig.getSecretEngineName()) ? DEFAULT_SECRET_ENGINE_NAME
                                                                       : vaultConfig.getSecretEngineName().trim();
    SecretEngineSummary secretEngine =
        secretEngineSummaries.stream()
            .filter(secretEngineSummary -> secretEngineSummary.getName().equals(mountPointName))
            .findAny()
            .get();

    logger.info("Matched Vault secret engine: {}", secretEngine);
    return secretEngine;
  }
}
