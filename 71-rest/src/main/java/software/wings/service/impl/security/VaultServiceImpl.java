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
import static io.harness.persistence.HPersistence.upToOne;
import static io.harness.security.SimpleEncryption.CHARSET;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.settings.SettingValue.SettingVariableTypes.VAULT;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.expression.SecretString;
import io.harness.persistence.HPersistence;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.beans.VaultConfig.VaultConfigKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.EncryptedDataParent;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 11/2/17.
 */
@Singleton
@Slf4j
public class VaultServiceImpl extends AbstractSecretServiceImpl implements VaultService, RuntimeCredentialsInjector {
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
  public void renewTokens(VaultConfig vaultConfig) {
    long currentTime = System.currentTimeMillis();
    String accountId = vaultConfig.getAccountId();
    if (vaultConfig.getRenewIntervalHours() <= 0) {
      logger.info("renewing not configured for {}", vaultConfig.getUuid());
      return;
    }
    // don't renew if renewed within configured time
    if (TimeUnit.MILLISECONDS.toHours(currentTime - vaultConfig.getRenewedAt()) < vaultConfig.getRenewIntervalHours()) {
      logger.info("{} renewed at {} not renewing now", vaultConfig.getUuid(), new Date(vaultConfig.getRenewedAt()));
      return;
    }

    VaultConfig decryptedVaultConfig = getVaultConfig(accountId, vaultConfig.getUuid());
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
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
      wingsPersistence.updateField(
          SecretManagerConfig.class, vaultConfig.getUuid(), VaultConfigKeys.renewedAt, System.currentTimeMillis());
    } catch (Exception e) {
      logger.info("Error while renewing token for : " + vaultConfig, e);
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
    }
  }

  private String saveSecretField(
      String accountId, String vaultConfigId, String secretValue, String secretNameSuffix, String fieldName) {
    EncryptedData encryptedData = encryptLocal(secretValue.toCharArray());
    // Get by auth token encrypted record by Id or name.
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .field(ACCOUNT_ID_KEY)
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
                                     .field(ACCOUNT_ID_KEY)
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
    logger.info("Renewing Vault AppRole client token for vault id {}", vaultConfig.getUuid());
    Preconditions.checkNotNull(vaultConfig.getAuthToken());
    if (isNotEmpty(vaultConfig.getAppRoleId())) {
      VaultConfig decryptedVaultConfig = getVaultConfig(vaultConfig.getAccountId(), vaultConfig.getUuid());
      try {
        VaultAppRoleLoginResult loginResult = appRoleLogin(decryptedVaultConfig);
        if (loginResult != null && isNotEmpty(loginResult.getClientToken())) {
          logger.info("Login result is {} {}", loginResult.getLeaseDuration(), loginResult.getPolicies());
          updateSecretField(vaultConfig.getAuthToken(), vaultConfig.getAccountId(), vaultConfig.getUuid(),
              loginResult.getClientToken(), TOKEN_SECRET_NAME_SUFFIX, VaultConfigKeys.authToken);
          wingsPersistence.updateField(
              SecretManagerConfig.class, vaultConfig.getUuid(), VaultConfigKeys.renewedAt, System.currentTimeMillis());
        }
      } catch (Exception e) {
        logger.info("Error while renewing client token for Vault AppRole  " + vaultConfig.getAppRoleId()
                + " in secret manager " + vaultConfig.getName(),
            e);
      }
    }
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

  String updateVaultConfig(String accountId, VaultConfig vaultConfig, boolean auditChanges) {
    VaultConfig savedVaultConfigWithCredentials = getVaultConfig(accountId, vaultConfig.getUuid());
    VaultConfig oldConfigForAudit = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
    VaultConfig savedVaultConfig = KryoUtils.clone(oldConfigForAudit);
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
    validateVaultConfig(accountId, vaultConfig);

    if (credentialChanged) {
      updateVaultCredentials(savedVaultConfig, vaultConfig.getAuthToken(), vaultConfig.getSecretId());
    }

    savedVaultConfig.setName(vaultConfig.getName());
    savedVaultConfig.setRenewIntervalHours(vaultConfig.getRenewIntervalHours());
    savedVaultConfig.setDefault(vaultConfig.isDefault());
    savedVaultConfig.setReadOnly(vaultConfig.isReadOnly());
    savedVaultConfig.setBasePath(vaultConfig.getBasePath());
    savedVaultConfig.setSecretEngineName(vaultConfig.getSecretEngineName());
    savedVaultConfig.setSecretEngineVersion(vaultConfig.getSecretEngineVersion());
    savedVaultConfig.setAppRoleId(vaultConfig.getAppRoleId());
    savedVaultConfig.setVaultUrl(vaultConfig.getVaultUrl());
    savedVaultConfig.setTemplatizedFields(vaultConfig.getTemplatizedFields());
    // PL-3237: Audit secret manager config changes.
    if (auditChanges) {
      generateAuditForSecretManager(accountId, oldConfigForAudit, savedVaultConfig);
    }
    return secretManagerConfigService.save(savedVaultConfig);
  }

  private String updateVaultConfig(String accountId, VaultConfig vaultConfig) {
    return updateVaultConfig(accountId, vaultConfig, true);
  }

  private String saveVaultConfig(String accountId, VaultConfig vaultConfig) {
    // Validate every time when secret manager config change submitted
    validateVaultConfig(accountId, vaultConfig);

    String authToken = vaultConfig.getAuthToken();
    String secretId = vaultConfig.getSecretId();

    try {
      vaultConfig.setAuthToken(null);
      vaultConfig.setSecretId(null);
      String vaultConfigId = secretManagerConfigService.save(vaultConfig);
      vaultConfig.setUuid(vaultConfigId);
    } catch (DuplicateKeyException e) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "Another vault configuration with the same name or URL exists", USER_SRE);
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
  public String saveOrUpdateVaultConfig(String accountId, VaultConfig vaultConfig) {
    checkIfSecretsManagerConfigCanBeCreatedOrUpdated(accountId);
    // First normalize the base path value. Set default base path if it has not been specified from input.
    String basePath = isEmpty(vaultConfig.getBasePath()) ? DEFAULT_BASE_PATH : vaultConfig.getBasePath().trim();
    vaultConfig.setBasePath(basePath);
    vaultConfig.setAccountId(accountId);

    if (vaultConfig.isReadOnly() && vaultConfig.isDefault()) {
      throw new SecretManagementException(
          SECRET_MANAGEMENT_ERROR, "A read only vault cannot be the default secret manager", USER);
    }

    checkIfTemplatizedSecretManagerCanBeCreatedOrUpdated(vaultConfig);

    return isEmpty(vaultConfig.getUuid()) ? saveVaultConfig(accountId, vaultConfig)
                                          : updateVaultConfig(accountId, vaultConfig);
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
    if (SecretManagerConfig.isTemplatized(vaultConfig)) {
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
                     .filter(ACCOUNT_ID_KEY, accountId)
                     .filter(EncryptedDataKeys.kmsId, vaultConfigId)
                     .filter(EncryptedDataKeys.encryptionType, EncryptionType.VAULT)
                     .count(upToOne);

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

  private List<SecretEngineSummary> listSecretEnginesInternal(VaultConfig vaultConfig) {
    // HAR-7605: Shorter timeout for decryption tasks, and it should retry on timeout or failure.
    int failedAttempts = 0;
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
        if (loginResult != null && EmptyPredicate.isNotEmpty(loginResult.getClientToken())) {
          vaultConfig.setAuthToken(loginResult.getClientToken());
        } else {
          String message =
              "Was not able to login Vault using the AppRole auth method. Please check your credentials and try again";
          throw new SecretManagementException(VAULT_OPERATION_ERROR, message, USER);
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
        encrypt(VAULT_VAILDATION_URL, Boolean.TRUE.toString(), accountId, VAULT, vaultConfig, null);
      } catch (WingsException e) {
        String message =
            "Was not able to encrypt a secret in vault using given credentials. Please check your credentials and try again";
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
    List<SecretEngineSummary> secretEngineSummaries = listSecretEnginesInternal(vaultConfig);

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

  @Override
  public Optional<SecretManagerConfig> updateRuntimeCredentials(
      SecretManagerConfig secretManagerConfig, Map<String, String> runtimeParameters) {
    if (isEmpty(secretManagerConfig.getTemplatizedFields()) || isEmpty(runtimeParameters)) {
      return Optional.empty();
    }

    VaultConfig vaultConfig = (VaultConfig) KryoUtils.clone(secretManagerConfig);
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
    updateVaultConfig(vaultConfig.getAccountId(), vaultConfig, false);
    return Optional.of(vaultConfig);
  }
}
