package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64ToByteArray;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.security.encryption.SimpleEncryption.CHARSET;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.persistence.HIterator;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.common.Constants;
import software.wings.helpers.ext.vault.VaultSysMountsRestClient;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 11/2/17.
 */
public class VaultServiceImpl extends AbstractSecretServiceImpl implements VaultService {
  public static final String VAULT_VAILDATION_URL = "harness_vault_validation";
  @Inject private KmsService kmsService;
  @Inject private AlertService alertService;

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData encryptedData) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .encrypt(name, value, accountId, settingType, vaultConfig, encryptedData);
  }

  @Override
  public char[] decrypt(EncryptedData data, String accountId, VaultConfig vaultConfig) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext).decrypt(data, vaultConfig);
  }

  @Override
  public VaultConfig getSecretConfig(String accountId) {
    VaultConfig vaultConfig =
        wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId).filter("isDefault", true).get();

    if (vaultConfig != null) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + vaultConfig);

      char[] decrypt =
          kmsService.decrypt(encryptedData, accountId, kmsService.getKmsConfig(accountId, encryptedData.getKmsId()));
      vaultConfig.setAuthToken(String.valueOf(decrypt));
    }

    return vaultConfig;
  }

  @Override
  public VaultConfig getVaultConfig(String accountId, String entityId) {
    VaultConfig vaultConfig =
        wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId).filter("_id", entityId).get();

    if (vaultConfig != null) {
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + vaultConfig);

      char[] decrypt =
          kmsService.decrypt(encryptedData, accountId, kmsService.getKmsConfig(accountId, encryptedData.getKmsId()));
      vaultConfig.setAuthToken(String.valueOf(decrypt));
    }

    return vaultConfig;
  }

  @Override
  public void renewTokens(String accountId) {
    long currentTime = System.currentTimeMillis();
    logger.info("renewing vault token for {}", accountId);
    try (HIterator<VaultConfig> query =
             new HIterator<>(wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId).fetch())) {
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
        SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
        KmsSetupAlert kmsSetupAlert =
            KmsSetupAlert.builder()
                .kmsId(vaultConfig.getUuid())
                .message(vaultConfig.getName()
                    + "(Hashicorp Vault) is not able to renew the token. Please check your setup and ensure that token is renewable")
                .build();
        try {
          delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
              .renewVaultToken(decryptedVaultConfig);
          alertService.closeAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
          vaultConfig.setRenewedAt(System.currentTimeMillis());
          wingsPersistence.save(vaultConfig);
        } catch (Exception e) {
          logger.info("Error while renewing token for : " + vaultConfig, e);
          alertService.openAlert(accountId, Base.GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
        }
      }
    }
  }

  @Override
  public String saveVaultConfig(String accountId, VaultConfig vaultConfig) {
    VaultConfig savedVaultConfig = null;
    boolean shouldVerify = true;
    if (!isEmpty(vaultConfig.getUuid())) {
      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
      if (savedVaultConfig.isDefault() && !vaultConfig.isDefault()) {
        throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER_SRE)
            .addParam("reason",
                "Can't set default vault secret manager to non-default explicitly. "
                    + "Please choose another secret manager instance as default instead.");
      }
      shouldVerify = !savedVaultConfig.getVaultUrl().equals(vaultConfig.getVaultUrl())
          || !Constants.SECRET_MASK.equals(vaultConfig.getAuthToken());
    }
    if (shouldVerify) {
      // New vault configuration, need to validate it's parameters
      validateVaultConfig(accountId, vaultConfig);
    } else {
      // When setting this vault config as default, set current default secret manager to non-default first.
      updateCurrentEncryptionConfigToNonDefaultIfNeeded(accountId, vaultConfig);

      // update without token or url changes
      savedVaultConfig.setName(vaultConfig.getName());
      savedVaultConfig.setRenewIntervalHours(vaultConfig.getRenewIntervalHours());
      savedVaultConfig.setDefault(vaultConfig.isDefault());
      return wingsPersistence.save(savedVaultConfig);
    }

    vaultConfig.setAccountId(accountId);

    EncryptedData encryptedData =
        kmsService.encrypt(vaultConfig.getAuthToken().toCharArray(), accountId, kmsService.getSecretConfig(accountId));
    if (isNotBlank(vaultConfig.getUuid())) {
      EncryptedData savedEncryptedData = wingsPersistence.get(
          EncryptedData.class, wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid()).getAuthToken());
      Preconditions.checkNotNull(savedEncryptedData, "reference is null for " + vaultConfig.getUuid());
      savedEncryptedData.setEncryptionKey(encryptedData.getEncryptionKey());
      savedEncryptedData.setEncryptedValue(encryptedData.getEncryptedValue());
      encryptedData = savedEncryptedData;
    }
    vaultConfig.setAuthToken(null);
    String vaultConfigId;
    try {
      vaultConfigId = wingsPersistence.save(vaultConfig);
    } catch (DuplicateKeyException e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER_SRE)
          .addParam("reason", "Another vault configuration with the same name or URL exists");
    }

    // When setting this vault config as default, set current default secret manager to non-default first.
    updateCurrentEncryptionConfigToNonDefaultIfNeeded(accountId, vaultConfig);

    encryptedData.setAccountId(accountId);
    encryptedData.addParent(vaultConfigId);
    encryptedData.setType(SettingVariableTypes.VAULT);
    encryptedData.setName(vaultConfig.getName() + "_token");
    String encryptedDataId = wingsPersistence.save(encryptedData);
    vaultConfig.setAuthToken(encryptedDataId);
    wingsPersistence.save(vaultConfig);

    return vaultConfigId;
  }

  @Override
  public boolean deleteVaultConfig(String accountId, String vaultConfigId) {
    final long count = wingsPersistence.createQuery(EncryptedData.class)
                           .filter("accountId", accountId)
                           .filter("kmsId", vaultConfigId)
                           .filter("encryptionType", EncryptionType.VAULT)
                           .count(new CountOptions().limit(1));

    if (count > 0) {
      String message = "Can not delete the vault configuration since there are secrets encrypted with this. "
          + "Please transition your secrets to a new kms and then try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR).addParam("reason", message);
    }

    VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    Preconditions.checkNotNull(vaultConfig, "no vault config found with id " + vaultConfigId);

    wingsPersistence.delete(EncryptedData.class, vaultConfig.getAuthToken());
    return wingsPersistence.delete(vaultConfig);
  }

  @Override
  public Collection<VaultConfig> listVaultConfigs(String accountId, boolean maskSecret) {
    List<VaultConfig> rv = new ArrayList<>();
    try (HIterator<VaultConfig> query = new HIterator<>(wingsPersistence.createQuery(VaultConfig.class)
                                                            .filter("accountId", accountId)
                                                            .order("-createdAt")
                                                            .fetch())) {
      while (query.hasNext()) {
        VaultConfig vaultConfig = query.next();
        Query<EncryptedData> encryptedDataQuery = wingsPersistence.createQuery(EncryptedData.class)
                                                      .filter("kmsId", vaultConfig.getUuid())
                                                      .filter("accountId", accountId);
        vaultConfig.setNumOfEncryptedValue(encryptedDataQuery.asKeyList().size());
        if (maskSecret) {
          vaultConfig.setAuthToken(SECRET_MASK);
        } else {
          EncryptedData tokenData = wingsPersistence.get(EncryptedData.class, vaultConfig.getAuthToken());
          Preconditions.checkNotNull(tokenData, "token data null for " + vaultConfig);
          char[] decryptedToken =
              kmsService.decrypt(tokenData, accountId, kmsService.getKmsConfig(accountId, tokenData.getKmsId()));
          vaultConfig.setAuthToken(String.valueOf(decryptedToken));
        }
        vaultConfig.setEncryptionType(EncryptionType.VAULT);
        rv.add(vaultConfig);
      }
    }
    return rv;
  }

  @Override
  public EncryptedData encryptFile(String accountId, VaultConfig vaultConfig, String name,
      BoundedInputStream inputStream, EncryptedData savedEncryptedData) {
    try {
      Preconditions.checkNotNull(vaultConfig);
      byte[] bytes = encodeBase64ToByteArray(ByteStreams.toByteArray(inputStream));
      EncryptedData fileData = encrypt(name, new String(CHARSET.decode(ByteBuffer.wrap(bytes)).array()), accountId,
          SettingVariableTypes.CONFIG_FILE, vaultConfig, savedEncryptedData);
      fileData.setAccountId(accountId);
      fileData.setName(name);
      fileData.setType(SettingVariableTypes.CONFIG_FILE);
      fileData.setBase64Encoded(true);
      fileData.setFileSize(inputStream.getTotalBytesRead());
      return fileData;
    } catch (IOException ioe) {
      throw new WingsException(DEFAULT_ERROR_CODE, ioe);
    }
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
    SyncTaskContext syncTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    delegateProxyFactory.get(SecretManagementDelegateService.class, syncTaskContext)
        .deleteVaultSecret(path, vaultConfig);
  }

  void validateVaultConfig(String accountId, VaultConfig vaultConfig) {
    try {
      if (vaultConfig.getSecretEngineVersion() == 0) {
        // Value 0 means the vault secret engine version has not been determined. Will need to check with
        // the Vault server to determine the actual secret engine version.
        int secreteEngineVersion = getVaultSecretEngineVersion(vaultConfig);
        vaultConfig.setSecretEngineVersion(secreteEngineVersion);
      }
    } catch (Exception e) {
      String message =
          "Was not able to determine the vault server's secret engine version using given credentials. Please check your credentials and try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, message, USER, e).addParam("reason", message);
    }

    try {
      encrypt(VAULT_VAILDATION_URL, Boolean.TRUE.toString(), accountId, SettingVariableTypes.VAULT, vaultConfig, null);
    } catch (WingsException e) {
      String message =
          "Was not able to reach vault using given credentials. Please check your credentials and try again";
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, message, USER, e).addParam("reason", message);
    }
  }

  private boolean updateCurrentEncryptionConfigToNonDefaultIfNeeded(String accountId, VaultConfig currentVaultConfig) {
    boolean updated = false;
    if (currentVaultConfig.isDefault()) {
      Query<KmsConfig> kmsConfigQuery = wingsPersistence.createQuery(KmsConfig.class).filter("accountId", accountId);
      List<KmsConfig> kmsConfigs = kmsConfigQuery.asList();
      for (KmsConfig kmsConfig : kmsConfigs) {
        if (kmsConfig.isDefault()) {
          kmsConfig.setDefault(false);
          wingsPersistence.save(kmsConfig);
          updated = true;
        }
      }
      Query<VaultConfig> vaultConfigQuery =
          wingsPersistence.createQuery(VaultConfig.class).filter("accountId", accountId);
      List<VaultConfig> vaultConfigs = vaultConfigQuery.asList();
      for (VaultConfig vaultConfig : vaultConfigs) {
        if (vaultConfig.isDefault()) {
          vaultConfig.setDefault(false);
          wingsPersistence.save(vaultConfig);
          updated = true;
        }
      }
    }

    return updated;
  }

  int getVaultSecretEngineVersion(VaultConfig vaultConfig) throws IOException {
    // http logging interceptor for dumping retrofit request/response content.
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    // set your desired log level, NONE by default. BODY while performing local testing.
    logging.setLevel(Level.NONE);

    OkHttpClient httpClient = Http.getOkHttpClientWithNoProxyValueSet(vaultConfig.getVaultUrl())
                                  .readTimeout(10, TimeUnit.SECONDS)
                                  .addInterceptor(logging)
                                  .build();

    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(vaultConfig.getVaultUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(httpClient)
                                  .build();
    VaultSysMountsRestClient restClient = retrofit.create(VaultSysMountsRestClient.class);

    Response<ResponseBody> response = restClient.getAll(vaultConfig.getAuthToken()).execute();

    int version = 1;
    if (response.isSuccessful()) {
      version = parseSecretEngineVersionFromSysMountsJson(response.body().string());
    }

    return version;
  }

  /**
   * Parsing the /secret/option/version integer value of the of full sys mounts output JSON from the
   * Vault /v1/secret/sys/mounts REST API call. Sample snippet of the output call is:
   *
   * {
   *   "secret/": {
   *     "accessor": "kv_7fa3b4ad",
   *     "config": {
   *       "default_lease_ttl": 0,
   *       "force_no_cache": false,
   *       "max_lease_ttl": 0,
   *       "plugin_name": ""
   *     },
   *     "description": "key\/value secret storage",
   *     "local": false,
   *     "options": {
   *       "version": "2"
   *     },
   *     "seal_wrap": false,
   *     "type": "kv"
   *   }
   * }
   *
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
