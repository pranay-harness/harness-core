package software.wings.service.impl.security;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;
import static software.wings.service.intfc.security.SecretManager.ACCOUNT_ID_KEY;
import static software.wings.service.intfc.security.SecretManager.SECRET_NAME_KEY;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.expression.SecretString;
import io.harness.network.Http;
import io.harness.persistence.HIterator;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.Account;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.helpers.ext.vault.VaultSysAuthRestClient;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.impl.security.vault.VaultAppRoleLoginRequest;
import software.wings.service.impl.security.vault.VaultAppRoleLoginResponse;
import software.wings.service.impl.security.vault.VaultAppRoleLoginResult;
import software.wings.service.intfc.AccountService;
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
import java.util.ArrayList;
import java.util.Collection;
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
  @Inject private AccountService accountService;

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData) {
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
  public VaultConfig getSecretConfig(String accountId) {
    Query<VaultConfig> query =
        wingsPersistence.createQuery(VaultConfig.class).filter(ACCOUNT_ID_KEY, accountId).filter(IS_DEFAULT_KEY, true);
    return getVaultConfigInternal(query);
  }

  @Override
  public VaultConfig getVaultConfig(String accountId, String entityId) {
    Query<VaultConfig> query =
        wingsPersistence.createQuery(VaultConfig.class).filter(ACCOUNT_ID_KEY, accountId).filter(ID_KEY, entityId);
    return getVaultConfigInternal(query);
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
        throw new WingsException("Either auth token or secret Id field needs to be present for vault secret manager.");
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
    try (HIterator<VaultConfig> query = new HIterator<>(
             wingsPersistence.createQuery(VaultConfig.class).filter(ACCOUNT_ID_KEY, accountId).fetch())) {
      while (query.hasNext()) {
        VaultConfig vaultConfig = query.next();
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
  public void appRoleLogin(String accountId) {
    logger.info("Renewing Vault AppRole client token for {}", accountId);
    try (HIterator<VaultConfig> query = new HIterator<>(
             wingsPersistence.createQuery(VaultConfig.class).filter(ACCOUNT_ID_KEY, accountId).fetch())) {
      while (query.hasNext()) {
        VaultConfig vaultConfig = query.next();
        if (isNotEmpty(vaultConfig.getAppRoleId())) {
          VaultConfig decryptedVaultConfig = getVaultConfig(accountId, vaultConfig.getUuid());

          try {
            VaultAppRoleLoginResult loginResult = appRoleLogin(decryptedVaultConfig);
            if (loginResult != null) {
              vaultConfig.setRenewedAt(System.currentTimeMillis());
              vaultConfig.setAuthToken(loginResult.getClientToken());
              saveVaultConfig(accountId, vaultConfig);
            }
          } catch (Exception e) {
            logger.info("Error while renewing client token for Vault AppRole  " + vaultConfig.getAppRoleId()
                    + " in secret manager " + vaultConfig.getName(),
                e);
          }
        }
      }
    }
  }

  @Override
  public String saveVaultConfig(String accountId, VaultConfig vaultConfig) {
    checkIfVaultConfigCanBeCreatedOrUpdated(accountId, vaultConfig);

    // First normalize the base path value. Set default base path if it has not been specified from input.
    String basePath = isEmpty(vaultConfig.getBasePath()) ? DEFAULT_BASE_PATH : vaultConfig.getBasePath().trim();
    vaultConfig.setBasePath(basePath);

    VaultConfig savedVaultConfig = null;
    boolean shouldVerify = true;
    if (!isEmpty(vaultConfig.getUuid())) {
      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
      shouldVerify = !savedVaultConfig.getVaultUrl().equals(vaultConfig.getVaultUrl())
          || !Objects.equals(savedVaultConfig.getAppRoleId(), vaultConfig.getAppRoleId())
          || !SecretString.SECRET_MASK.equals(vaultConfig.getAuthToken())
          || !SecretString.SECRET_MASK.equals(vaultConfig.getSecretId());
    }
    if (shouldVerify) {
      // New vault configuration, need to validate it's parameters
      validateVaultConfig(accountId, vaultConfig);
    } else {
      // When setting this vault config as default, set current default secret manager to non-default first.
      if (vaultConfig.isDefault()) {
        updateCurrentEncryptionConfigsToNonDefault(accountId);
      }

      // update without token or url changes
      savedVaultConfig.setName(vaultConfig.getName());
      savedVaultConfig.setRenewIntervalHours(vaultConfig.getRenewIntervalHours());
      savedVaultConfig.setDefault(vaultConfig.isDefault());
      savedVaultConfig.setBasePath(vaultConfig.getBasePath());
      savedVaultConfig.setAppRoleId(vaultConfig.getAppRoleId());
      return wingsPersistence.save(savedVaultConfig);
    }

    vaultConfig.setAccountId(accountId);

    EncryptedData authTokenEncryptedData = getEncryptedDataForSecretField(
        savedVaultConfig, vaultConfig, vaultConfig.getAuthToken(), TOKEN_SECRET_NAME_SUFFIX);
    EncryptedData secretIdEncryptedData = getEncryptedDataForSecretField(
        savedVaultConfig, vaultConfig, vaultConfig.getSecretId(), SECRET_ID_SECRET_NAME_SUFFIX);

    vaultConfig.setAuthToken(null);
    vaultConfig.setSecretId(null);
    String vaultConfigId;
    try {
      // When setting this vault config as default, set current default secret manager to non-default first.
      if (vaultConfig.isDefault()) {
        updateCurrentEncryptionConfigsToNonDefault(accountId);
      }
      vaultConfigId = wingsPersistence.save(vaultConfig);
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY, "Another vault configuration with the same name or URL exists");
    }

    // Create a LOCAL encrypted record for Vault authToken
    String authTokenEncryptedDataId =
        saveSecretField(vaultConfig, vaultConfigId, authTokenEncryptedData, TOKEN_SECRET_NAME_SUFFIX);
    vaultConfig.setAuthToken(authTokenEncryptedDataId);

    // Create a LOCAL encrypted record for Vault secretId
    String secretIdEncryptedDataId =
        saveSecretField(vaultConfig, vaultConfigId, secretIdEncryptedData, SECRET_ID_SECRET_NAME_SUFFIX);
    vaultConfig.setSecretId(secretIdEncryptedDataId);

    wingsPersistence.save(vaultConfig);
    return vaultConfigId;
  }

  private EncryptedData getEncryptedDataForSecretField(
      VaultConfig savedVaultConfig, VaultConfig vaultConfig, String secretValue, String secretNameSuffix) {
    EncryptedData encryptedData = isNotEmpty(secretValue) ? encryptLocal(secretValue.toCharArray()) : null;
    if (savedVaultConfig != null && encryptedData != null) {
      // Get by auth token encrypted record by Id or name.
      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(ACCOUNT_ID_KEY)
          .equal(vaultConfig.getAccountId())
          .or(query.criteria(ID_KEY).equal(savedVaultConfig.getAuthToken()),
              query.criteria(SECRET_NAME_KEY).equal(vaultConfig.getName() + secretNameSuffix));
      EncryptedData savedEncryptedData = query.get();
      if (savedEncryptedData != null) {
        savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
        savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
        encryptedData = savedEncryptedData;
      }
    }
    return encryptedData;
  }

  private String saveSecretField(
      VaultConfig vaultConfig, String vaultConfigId, EncryptedData secretFieldEncryptedData, String secretNameSuffix) {
    String secretFieldEncryptedDataId = null;
    if (secretFieldEncryptedData != null) {
      secretFieldEncryptedData.setAccountId(vaultConfig.getAccountId());
      secretFieldEncryptedData.addParent(vaultConfigId);
      secretFieldEncryptedData.setType(SettingVariableTypes.VAULT);
      secretFieldEncryptedData.setName(vaultConfig.getName() + secretNameSuffix);
      secretFieldEncryptedDataId = wingsPersistence.save(secretFieldEncryptedData);
    }
    return secretFieldEncryptedDataId;
  }

  private void checkIfVaultConfigCanBeCreatedOrUpdated(String accountId, VaultConfig vaultConfig) {
    Account account = accountService.get(accountId);

    if (account.isLocalEncryptionEnabled()) {
      // Reject creation of new Vault secret manager if 'localEncryptionEnabled' account flag is set
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER_SRE)
          .addParam(REASON_KEY, "Can't create new Vault secret manager for a LOCAL encryption enabled account!");
    }

    String vaultConfigId = vaultConfig.getUuid();
    boolean isNewVaultConfig = isEmpty(vaultConfigId);
    boolean isCommunityAccount = accountService.isCommunityAccount(accountId);
    if (isCommunityAccount) {
      if (isNewVaultConfig) {
        throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER)
            .addParam(REASON_KEY, "Cannot add new HashiCorp Vault Secret Manager in Harness Community.");
      }

      VaultConfig savedVaultConfig = getSavedVaultConfig(vaultConfigId);
      if (vaultConfig.isDefault() && !savedVaultConfig.isDefault()) {
        throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER)
            .addParam(REASON_KEY, "Cannot change default Secret Manager in Harness Community.");
      }
    }
  }

  private VaultConfig getSavedVaultConfig(String id) {
    return wingsPersistence.get(VaultConfig.class, id);
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
          + "Please transition your secrets to a new kms and then try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER).addParam(REASON_KEY, message);
    }

    VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    Preconditions.checkNotNull(vaultConfig, "no vault config found with id " + vaultConfigId);

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
    return wingsPersistence.delete(vaultConfig);
  }

  @Override
  public Collection<VaultConfig> listVaultConfigs(String accountId, boolean maskSecret) {
    List<VaultConfig> rv = new ArrayList<>();
    try (HIterator<VaultConfig> query = new HIterator<>(wingsPersistence.createQuery(VaultConfig.class)
                                                            .filter(ACCOUNT_ID_KEY, accountId)
                                                            .order("-createdAt")
                                                            .fetch())) {
      while (query.hasNext()) {
        VaultConfig vaultConfig = query.next();
        Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                      .filter(EncryptedDataKeys.kmsId, vaultConfig.getUuid())
                                                      .filter(ACCOUNT_ID_KEY, accountId);
        vaultConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
        if (maskSecret) {
          vaultConfig.setAuthToken(SECRET_MASK);
          vaultConfig.setSecretId(SECRET_MASK);
        } else {
          EncryptedData tokenData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
          EncryptedData secretIdData = wingsPersistence.get(EncryptedData.class, vaultConfig.getSecretId());
          if (tokenData == null && secretIdData == null) {
            throw new WingsException(
                "Either auth token or secret Id field needs to be present for vault secret manager.");
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
        vaultConfig.setEncryptionType(EncryptionType.VAULT);
        rv.add(vaultConfig);
      }
    }
    return rv;
  }

  @Override
  public EncryptedData encryptFile(
      String accountId, VaultConfig vaultConfig, String name, byte[] inputBytes, EncryptedData savedEncryptedData) {
    Preconditions.checkNotNull(vaultConfig);
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
      Preconditions.checkNotNull(vaultConfig);
      Preconditions.checkNotNull(encryptedData);
      char[] decrypt = decrypt(encryptedData, accountId, vaultConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      Files.write(fileData, file);
      return file;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
  }

  @Override
  public void decryptToStream(String accountId, EncryptedData encryptedData, OutputStream output) {
    try {
      VaultConfig vaultConfig = getVaultConfig(accountId, encryptedData.getKmsId());
      Preconditions.checkNotNull(vaultConfig);
      Preconditions.checkNotNull(encryptedData);
      char[] decrypt = decrypt(encryptedData, accountId, vaultConfig);
      byte[] fileData =
          encryptedData.isBase64Encoded() ? decodeBase64(decrypt) : CHARSET.encode(CharBuffer.wrap(decrypt)).array();
      output.write(fileData, 0, fileData.length);
      output.flush();
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
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
        // Value 0 means the vault secret engine version has not been determined. Will need to check with
        // the Vault server to determine the actual secret engine version.
        int secreteEngineVersion = getVaultSecretEngineVersion(vaultConfig);
        vaultConfig.setSecretEngineVersion(secreteEngineVersion);
        logger.info(
            "Secret engine version for vault secret manager {} is {}", vaultConfig.getName(), secreteEngineVersion);
      }

      // Need to try using Vault AppRole login to generate a client token if configured so
      if (isNotEmpty(vaultConfig.getAppRoleId())) {
        VaultAppRoleLoginResult loginResult = appRoleLogin(vaultConfig);
        if (loginResult == null) {
          String message =
              "Was not able to login Vault using the AppRole auth method. Please check your credentials and try again";
          throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, message, USER).addParam(REASON_KEY, message);
        } else {
          vaultConfig.setAuthToken(loginResult.getClientToken());
          logger.info("Got client token {} from vault AppRole {} and secret Id {}", loginResult.getClientToken(),
              vaultConfig.getAppRoleId(), vaultConfig.getSecretId());
        }
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String message =
          "Was not able to determine the vault server's secret engine version using given credentials. Please check your credentials and try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, message, USER, e).addParam(REASON_KEY, message);
    }

    try {
      encrypt(VAULT_VAILDATION_URL, Boolean.TRUE.toString(), accountId, SettingVariableTypes.VAULT, vaultConfig, null);
    } catch (WingsException e) {
      String message =
          "Was not able to reach vault using given credentials. Please check your credentials and try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, message, USER, e).addParam(REASON_KEY, message);
    }
  }

  public VaultAppRoleLoginResult appRoleLogin(VaultConfig vaultConfig) throws IOException {
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    loggingInterceptor.setLevel(Level.NONE);
    OkHttpClient httpClient =
        Http.getUnsafeOkHttpClientBuilder(vaultConfig.getVaultUrl(), 10, 10).addInterceptor(loggingInterceptor).build();

    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(vaultConfig.getVaultUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(httpClient)
                                  .build();
    VaultSysAuthRestClient restClient = retrofit.create(VaultSysAuthRestClient.class);

    VaultAppRoleLoginRequest loginRequest = VaultAppRoleLoginRequest.builder()
                                                .roleId(vaultConfig.getAppRoleId())
                                                .secretId(vaultConfig.getSecretId())
                                                .build();
    Response<VaultAppRoleLoginResponse> response = restClient.appRoleLogin(loginRequest).execute();

    VaultAppRoleLoginResult result = null;
    if (response.isSuccessful()) {
      result = response.body().getAuth();
    }
    return result;
  }

  int getVaultSecretEngineVersion(VaultConfig vaultConfig) throws IOException {
    OkHttpClient httpClient = Http.getUnsafeOkHttpClient(vaultConfig.getVaultUrl(), 10, 10);

    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(vaultConfig.getVaultUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(httpClient)
                                  .build();
    VaultSysAuthRestClient restClient = retrofit.create(VaultSysAuthRestClient.class);

    Response<ResponseBody> response = restClient.getAll(vaultConfig.getAuthToken()).execute();

    int version = 2;
    if (response.isSuccessful()) {
      version = parseSecretEngineVersionFromSysMountsJson(response.body().string());
    }

    return version;
  }

  /**
   * Parsing the /secret/option/version integer value of the of full sys mounts output JSON from the
   * Vault /v1/secret/sys/mounts REST API call. Sample snippet of the output call is:
   * <p>
   * {
   * "secret/": {
   * "accessor": "kv_7fa3b4ad",
   * "config": {
   * "default_lease_ttl": 0,
   * "force_no_cache": false,
   * "max_lease_ttl": 0,
   * "plugin_name": ""
   * },
   * "description": "key\/value secret storage",
   * "local": false,
   * "options": {
   * "version": "2"
   * },
   * "seal_wrap": false,
   * "type": "kv"
   * }
   * }
   */
  static int parseSecretEngineVersionFromSysMountsJson(String jsonResponse) {
    int version = 1;

    JsonParser jsonParser = new JsonParser();
    JsonElement responseElement = jsonParser.parse(jsonResponse);
    JsonObject sysMountsObject = responseElement.getAsJsonObject();
    JsonObject secretObject = sysMountsObject.getAsJsonObject("secret/");
    if (secretObject != null) {
      JsonObject optionsObject = secretObject.getAsJsonObject("options");
      if (optionsObject != null) {
        JsonPrimitive versionObject = optionsObject.getAsJsonPrimitive("version");
        if (versionObject != null) {
          version = versionObject.getAsInt();
        }
      }
    }
    return version;
  }
}
