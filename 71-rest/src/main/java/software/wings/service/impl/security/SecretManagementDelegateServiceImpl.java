package software.wings.service.impl.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.AZURE_KEY_VAULT_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.CYBERARK_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.VAULT_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static software.wings.helpers.ext.vault.VaultRestClientFactory.getFullPath;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.model.AWSKMSException;
import com.amazonaws.services.kms.model.DependencyTimeoutException;
import com.amazonaws.services.kms.model.KMSInternalException;
import com.amazonaws.services.kms.model.KeyUnavailableException;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.AWSSecretsManagerException;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import com.amazonaws.services.secretsmanager.model.DeleteSecretRequest;
import com.amazonaws.services.secretsmanager.model.DeleteSecretResult;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.amazonaws.services.secretsmanager.model.Tag;
import com.amazonaws.services.secretsmanager.model.UpdateSecretRequest;
import com.amazonaws.services.secretsmanager.model.UpdateSecretResult;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import com.microsoft.azure.keyvault.requests.SetSecretRequest;
import io.harness.beans.EmbeddedUser;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.helpers.ext.cyberark.CyberArkRestClient;
import software.wings.helpers.ext.cyberark.CyberArkRestClientFactory;
import software.wings.helpers.ext.vault.VaultRestClientFactory;
import software.wings.helpers.ext.vault.VaultSysAuthRestClient;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.service.impl.security.cyberark.CyberArkReadResponse;
import software.wings.service.impl.security.gcpkms.GcpKmsEncryptDecryptClient;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient;
import software.wings.service.impl.security.vault.SecretEngineSummary;
import software.wings.service.impl.security.vault.SysMount;
import software.wings.service.impl.security.vault.SysMountsResponse;
import software.wings.service.impl.security.vault.VaultAppRoleLoginRequest;
import software.wings.service.impl.security.vault.VaultAppRoleLoginResponse;
import software.wings.service.impl.security.vault.VaultAppRoleLoginResult;
import software.wings.service.impl.security.vault.VaultSecretMetadata;
import software.wings.service.impl.security.vault.VaultSecretMetadata.VersionMetadata;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class SecretManagementDelegateServiceImpl implements SecretManagementDelegateService {
  private static final String KEY_SEPARATOR = "#";
  private static final JsonParser JSON_PARSER = new JsonParser();

  private TimeLimiter timeLimiter;
  private KmsEncryptDecryptClient kmsEncryptDecryptClient;
  private GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient;

  @Inject
  public SecretManagementDelegateServiceImpl(TimeLimiter timeLimiter, KmsEncryptDecryptClient kmsEncryptDecryptClient,
      GcpKmsEncryptDecryptClient gcpKmsEncryptDecryptClient) {
    this.timeLimiter = timeLimiter;
    this.kmsEncryptDecryptClient = kmsEncryptDecryptClient;
    this.gcpKmsEncryptDecryptClient = gcpKmsEncryptDecryptClient;
  }

  public static boolean isRetryable(Exception e) {
    // TimeLimiter.callWithTimer will throw a new exception wrapping around the AwsKMS exceptions. Unwrap it.
    Throwable t = e.getCause() == null ? e : e.getCause();

    if (t instanceof AWSSecretsManagerException) {
      logger.info("Got AWSSecretsManagerException {}: {}", t.getClass().getName(), t.getMessage());
      return !(t instanceof ResourceNotFoundException);
    } else if (t instanceof AWSKMSException) {
      logger.info("Got AWSKMSException {}: {}", t.getClass().getName(), t.getMessage());
      return t instanceof KMSInternalException || t instanceof DependencyTimeoutException
          || t instanceof KeyUnavailableException;
    } else {
      // Else if not IllegalArgumentException, it should retry.
      return !(t instanceof IllegalArgumentException);
    }
  }

  @Override
  public EncryptedRecord encrypt(String accountId, char[] value, KmsConfig kmsConfig) {
    return kmsEncryptDecryptClient.encrypt(accountId, value, kmsConfig);
  }

  @Override
  public char[] decrypt(EncryptedRecord data, KmsConfig kmsConfig) {
    return kmsEncryptDecryptClient.decrypt(data, kmsConfig);
  }

  @Override
  public EncryptedRecord encrypt(
      String value, String accountId, GcpKmsConfig gcpKmsConfig, EncryptedRecord savedEncryptedData) {
    return gcpKmsEncryptDecryptClient.encrypt(value, accountId, gcpKmsConfig, savedEncryptedData);
  }

  @Override
  public char[] decrypt(EncryptedRecord encryptedRecord, GcpKmsConfig gcpKmsConfig) {
    return gcpKmsEncryptDecryptClient.decrypt(encryptedRecord, gcpKmsConfig);
  }

  @Override
  public EncryptedRecord encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedRecord savedEncryptedData) {
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            ()
                -> encryptInternal(name, value, accountId, settingType, vaultConfig, savedEncryptedData),
            5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "encryption failed after " + NUM_OF_RETRIES + " retries";
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        } else {
          String message = "Failed to encrypt Vault secret " + name;
          throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
        }
      }
    }
  }

  @Override
  public char[] decrypt(EncryptedRecord data, VaultConfig vaultConfig) {
    if (data.getEncryptedValue() == null) {
      return null;
    }

    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(() -> decryptInternal(data, vaultConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("decryption failed. trial num: {}", failedAttempts, e);
        if (e instanceof SecretManagementDelegateException) {
          throw(SecretManagementDelegateException) e;
        } else if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Decryption failed after " + NUM_OF_RETRIES + " retries";
            throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        } else {
          String message = "Failed to decrypt Vault secret " + data.getName();
          throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
        }
      }
    }
  }

  @Override
  public boolean deleteVaultSecret(String path, VaultConfig vaultConfig) {
    try {
      String fullPath = getFullPath(vaultConfig.getBasePath(), path);
      return VaultRestClientFactory.create(vaultConfig)
          .deleteSecret(String.valueOf(vaultConfig.getAuthToken()), vaultConfig.getSecretEngineName(), fullPath);
    } catch (IOException e) {
      String message = "Deletion of Vault secret at " + path + " failed";
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }
  }

  @Override
  public List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedRecord encryptedData, VaultConfig vaultConfig) {
    List<SecretChangeLog> secretChangeLogs = new ArrayList<>();

    EmbeddedUser vaultUser = EmbeddedUser.builder().name("VaultUser").build();
    String encryptedDataId = encryptedData.getUuid();

    try {
      VaultSecretMetadata secretMetadata = VaultRestClientFactory.create(vaultConfig)
                                               .readSecretMetadata(vaultConfig.getAuthToken(),
                                                   vaultConfig.getSecretEngineName(), encryptedData.getPath());
      if (secretMetadata != null && isNotEmpty(secretMetadata.getVersions())) {
        for (Entry<Integer, VersionMetadata> entry : secretMetadata.getVersions().entrySet()) {
          int version = entry.getKey();
          VersionMetadata versionMetadata = entry.getValue();
          final String changeDescription;
          final String changeTime;
          if (versionMetadata.isDestroyed()) {
            changeDescription = "Deleted at version " + version + " in Vault";
            changeTime = versionMetadata.getDeletionTime();
          } else {
            changeDescription = version == 1 ? "Created in Vault" : "Updated to version " + version + " in Vault";
            changeTime = versionMetadata.getCreatedTime();
          }
          SecretChangeLog changeLog = SecretChangeLog.builder()
                                          .accountId(vaultConfig.getAccountId())
                                          .encryptedDataId(encryptedDataId)
                                          .description(changeDescription)
                                          .external(true)
                                          .user(vaultUser)
                                          .build();
          long changeTimeInMillis = Instant.parse(changeTime).toEpochMilli();
          changeLog.setCreatedBy(vaultUser);
          changeLog.setLastUpdatedBy(vaultUser);
          changeLog.setCreatedAt(changeTimeInMillis);
          changeLog.setLastUpdatedAt(changeTimeInMillis);

          // need to set the change time to the corresponding field in SecretChangeLog.
          // changeLog.setCreatedAt();
          secretChangeLogs.add(changeLog);
        }
      }
    } catch (Exception e) {
      String message = "Retrieval of vault secret version history failed";
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }

    return secretChangeLogs;
  }

  @Override
  public boolean renewVaultToken(VaultConfig vaultConfig) {
    int failedAttempts = 0;
    while (true) {
      try {
        logger.info("renewing token for vault {}", vaultConfig);
        boolean isSuccessful = VaultRestClientFactory.create(vaultConfig).renewToken(vaultConfig.getAuthToken());
        if (isSuccessful) {
          return true;
        } else {
          String errorMsg = "Request not successful.";
          logger.error(errorMsg);
          throw new IOException(errorMsg);
        }
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("renewal failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String message = "renewal failed after " + NUM_OF_RETRIES + " retries";
          throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
        }
        sleep(ofMillis(1000));
      }
    }
  }

  @Override
  public List<SecretEngineSummary> listSecretEngines(VaultConfig vaultConfig) {
    List<SecretEngineSummary> secretEngineSummaries = new ArrayList<>();
    try {
      String vaultToken = vaultConfig.getAuthToken();
      if (isEmpty(vaultToken)) {
        VaultAppRoleLoginResult loginResult = appRoleLogin(vaultConfig);
        if (loginResult != null) {
          vaultToken = loginResult.getClientToken();
          vaultConfig.setAuthToken(vaultToken);
        }
      }

      VaultSysAuthRestClient restClient =
          VaultRestClientFactory.getVaultRetrofit(vaultConfig.getVaultUrl()).create(VaultSysAuthRestClient.class);
      Response<SysMountsResponse> response = restClient.getAllMounts(vaultConfig.getAuthToken()).execute();
      if (response.isSuccessful()) {
        Map<String, SysMount> sysMountMap = response.body().getData();
        logger.info("Found Vault sys mount points: {}", sysMountMap.keySet());

        for (Entry<String, SysMount> entry : sysMountMap.entrySet()) {
          String secretEngineName = StringUtils.removeEnd(entry.getKey(), "/");
          SysMount sysMount = entry.getValue();
          Integer version = sysMount.getOptions() == null ? null : sysMount.getOptions().getVersion();
          SecretEngineSummary secretEngineSummary = SecretEngineSummary.builder()
                                                        .name(secretEngineName)
                                                        .description(sysMount.getDescription())
                                                        .type(sysMount.getType())
                                                        .version(version)
                                                        .build();
          secretEngineSummaries.add(secretEngineSummary);
        }
      } else {
        // Log down why the sys mount failed.
        if (response.errorBody() != null) {
          logger.error("Failed to list secret engines for secret manager {} due to the following error {}",
              vaultConfig.getVaultUrl(), response.errorBody().string());
        }
        // To be consistent with the old Vault secret management behavior we will take a default secret engine
        SecretEngineSummary secretEngineSummary = SecretEngineSummary.builder()
                                                      .name(VaultService.DEFAULT_SECRET_ENGINE_NAME)
                                                      .type(VaultService.KEY_VALUE_SECRET_ENGINE_TYPE)
                                                      .version(2)
                                                      .build();
        secretEngineSummaries.add(secretEngineSummary);
      }
    } catch (IOException e) {
      String message = "Failed to list secret engines for secret manager " + vaultConfig.getName() + " at "
          + vaultConfig.getVaultUrl();
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, USER);
    }

    return secretEngineSummaries;
  }

  @Override
  public VaultAppRoleLoginResult appRoleLogin(VaultConfig vaultConfig) {
    try {
      VaultSysAuthRestClient restClient =
          VaultRestClientFactory.getVaultRetrofit(vaultConfig.getVaultUrl()).create(VaultSysAuthRestClient.class);

      VaultAppRoleLoginRequest loginRequest = VaultAppRoleLoginRequest.builder()
                                                  .roleId(vaultConfig.getAppRoleId())
                                                  .secretId(vaultConfig.getSecretId())
                                                  .build();
      Response<VaultAppRoleLoginResponse> response = restClient.appRoleLogin(loginRequest).execute();

      VaultAppRoleLoginResult result = null;
      if (response.isSuccessful()) {
        result = response.body().getAuth();
      } else {
        String message = "Failed to perform AppRole based login for secret manager " + vaultConfig.getName() + " at "
            + vaultConfig.getVaultUrl();
        throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, USER);
      }
      return result;
    } catch (IOException e) {
      String message = "Failed to perform AppRole based login for secret manager " + vaultConfig.getName() + " at "
          + vaultConfig.getVaultUrl();
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, e, USER);
    }
  }

  @Override
  public EncryptedRecord encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AwsSecretsManagerConfig secretsManagerConfig, EncryptedRecord savedEncryptedData) {
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            ()
                -> encryptInternal(name, value, accountId, settingType, secretsManagerConfig, savedEncryptedData),
            5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Encryption failed after " + NUM_OF_RETRIES + " retries";
            throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        } else {
          String message = "Failed to encrypt secret " + name;
          throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
        }
      }
    }
  }

  @Override
  public char[] decrypt(EncryptedRecord data, AwsSecretsManagerConfig secretsManagerConfig) {
    if (data.getEncryptedValue() == null) {
      return null;
    }

    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> decryptInternal(data, secretsManagerConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("decryption failed. trial num: {}", failedAttempts, e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Decryption failed after " + NUM_OF_RETRIES + " retries";
            throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        } else {
          String message = "Failed to decrypt secret " + data.getName();
          throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
        }
      }
    }
  }

  @Override
  public boolean deleteSecret(String secretName, AwsSecretsManagerConfig secretsManagerConfig) {
    long startTime = System.currentTimeMillis();

    AWSSecretsManager client = getAwsSecretsManagerClient(secretsManagerConfig);
    DeleteSecretRequest request =
        new DeleteSecretRequest().withSecretId(secretName).withForceDeleteWithoutRecovery(true);
    DeleteSecretResult result = client.deleteSecret(request);

    logger.info("Done deleting AWS secret {} in {}ms", secretName, System.currentTimeMillis() - startTime);
    return result != null;
  }

  private EncryptedRecord encryptInternal(String name, String value, String accountId, SettingVariableTypes settingType,
      AwsSecretsManagerConfig secretsManagerConfig, EncryptedRecord savedEncryptedData) {
    final String fullSecretName;
    String refKeyName = null;
    boolean pathReference = false;
    if (savedEncryptedData != null && isNotEmpty(savedEncryptedData.getPath())) {
      pathReference = true;
      String path = savedEncryptedData.getPath();
      ParsedSecretRef secretRef = parsedSecretRef(path);
      fullSecretName = secretRef.secretPath;
      refKeyName = secretRef.keyName;
    } else {
      fullSecretName = getFullPath(secretsManagerConfig.getSecretNamePrefix(), name);
    }

    long startTime = System.currentTimeMillis();
    logger.info("Saving secret '{}' into AWS Secrets Manager: {}", fullSecretName, secretsManagerConfig.getName());

    EncryptedData encryptedData = savedEncryptedData != null ? (EncryptedData) savedEncryptedData
                                                             : EncryptedData.builder()
                                                                   .encryptionKey(fullSecretName)
                                                                   .encryptionType(EncryptionType.AWS_SECRETS_MANAGER)
                                                                   .enabled(true)
                                                                   .accountId(accountId)
                                                                   .kmsId(secretsManagerConfig.getUuid())
                                                                   .build();

    if (!pathReference && isEmpty(value)) {
      return encryptedData;
    }

    AWSSecretsManager client = getAwsSecretsManagerClient(secretsManagerConfig);

    // Get the secret value to see if it exists already.
    boolean secretExists = false;
    String secretValue = null;
    try {
      GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(fullSecretName);
      GetSecretValueResult result = client.getSecretValue(getSecretValueRequest);
      secretExists = isNotEmpty(result.getARN());
      secretValue = result.getSecretString();

      // Make sure the encrypted key/value of encrypted data record is set when secret exists.
      encryptedData.setEncryptionKey(savedEncryptedData.getPath());
      encryptedData.setEncryptedValue(result.getARN().toCharArray());
    } catch (ResourceNotFoundException e) {
      // If reaching here, it means the resource doesn't exist.
    }

    // If it's a secret reference, the referred secret has to exist for this reference creation to succeed.
    if (pathReference) {
      if (secretValue == null) {
        String message = "Secret name reference '" + savedEncryptedData.getPath() + "' is invalid";
        throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, USER);
      } else {
        // Secret name exist, check to make sure the referenced Key name also exist as part of the secret value JSON.
        if (refKeyName != null) {
          try {
            JsonElement element = JSON_PARSER.parse(secretValue);
            if (!element.getAsJsonObject().has(refKeyName)) {
              String message = "Secret reference key name " + refKeyName + " is invalid";
              throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, USER);
            }
          } catch (JsonParseException e) {
            String message = "Secret value for " + fullSecretName + " is not a valid JSON document";
            throw new SecretManagementDelegateException(AWS_SECRETS_MANAGER_OPERATION_ERROR, message, e, USER);
          }
        }
      }

      // For path reference secret. No need to actually encrypted and save the secret value in the AWS Secrets Manager.
      return encryptedData;
    }

    if (!secretExists) {
      // Create the secret with proper tags.
      CreateSecretRequest request = new CreateSecretRequest()
                                        .withName(fullSecretName)
                                        .withSecretString(value)
                                        .withTags(new Tag().withKey("createdBy").withValue("Harness"),
                                            new Tag().withKey("type").withValue(settingType.name()));
      CreateSecretResult createSecretResult = client.createSecret(request);
      encryptedData.setEncryptionKey(fullSecretName);
      encryptedData.setEncryptedValue(createSecretResult.getARN().toCharArray());
    } else {
      // Update the existing secret with new secret value.
      UpdateSecretRequest request = new UpdateSecretRequest().withSecretId(fullSecretName).withSecretString(value);
      UpdateSecretResult updateSecretResult = client.updateSecret(request);
      encryptedData.setEncryptionKey(fullSecretName);
      encryptedData.setEncryptedValue(updateSecretResult.getARN().toCharArray());
    }

    logger.info("Done saving secret {} into AWS Secrets Manager for {} in {}ms", fullSecretName,
        encryptedData.getUuid(), System.currentTimeMillis() - startTime);
    return encryptedData;
  }

  private char[] decryptInternal(EncryptedRecord data, AwsSecretsManagerConfig secretsManagerConfig) {
    long startTime = System.currentTimeMillis();

    final String secretName;
    String refKeyName = null;
    if (isNotEmpty(data.getPath())) {
      String path = data.getPath();
      ParsedSecretRef secretRef = parsedSecretRef(path);
      secretName = secretRef.secretPath;
      refKeyName = secretRef.keyName;
    } else {
      secretName = data.getEncryptionKey();
    }

    AWSSecretsManager client = getAwsSecretsManagerClient(secretsManagerConfig);
    GetSecretValueRequest request = new GetSecretValueRequest().withSecretId(secretName);
    GetSecretValueResult result = client.getSecretValue(request);
    String secretValue = result.getSecretString();

    char[] decryptedValue = null;
    if (isNotEmpty(refKeyName)) {
      JsonElement element = JSON_PARSER.parse(secretValue);
      if (element.getAsJsonObject().has(refKeyName)) {
        JsonElement refKeyedElement = element.getAsJsonObject().get(refKeyName);
        decryptedValue = refKeyedElement.toString().toCharArray();
      }
    } else {
      decryptedValue = secretValue.toCharArray();
    }

    logger.info("Done decrypting AWS secret {} in {}ms", secretName, System.currentTimeMillis() - startTime);
    return decryptedValue;
  }

  private AWSSecretsManager getAwsSecretsManagerClient(AwsSecretsManagerConfig secretsManagerConfig) {
    return AWSSecretsManagerClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(secretsManagerConfig.getAccessKey(), secretsManagerConfig.getSecretKey())))
        .withRegion(secretsManagerConfig.getRegion() == null ? Regions.US_EAST_1
                                                             : Regions.fromName(secretsManagerConfig.getRegion()))
        .build();
  }

  private EncryptedRecord encryptInternal(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedRecord savedEncryptedData) throws Exception {
    String keyUrl = settingType + "/" + name;
    char[] encryptedValue = keyUrl.toCharArray();

    long startTime = System.currentTimeMillis();
    logger.info("Saving secret {} into Vault {}", name, keyUrl);

    EncryptedData encryptedData = (EncryptedData) savedEncryptedData;
    if (savedEncryptedData == null) {
      encryptedData = EncryptedData.builder()
                          .encryptionKey(keyUrl)
                          .encryptedValue(encryptedValue)
                          .encryptionType(EncryptionType.VAULT)
                          .enabled(true)
                          .accountId(accountId)
                          .kmsId(vaultConfig.getUuid())
                          .build();
    }

    // When secret path is specified, no need to write secret, since the secret is already created and is just being
    // referred by a secret text.
    if (isNotEmpty(encryptedData.getPath())) {
      // Try decrypting to make sure the 'path' specified is pointing to a valid Vault path.
      char[] referredSecretValue = decryptInternal(encryptedData, vaultConfig);
      if (isEmpty(referredSecretValue)) {
        String message = "Vault path '" + encryptedData.getPath() + "' is not referring to any valid secret.";
        throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, message, USER);
      }

      encryptedData.setEncryptionKey(keyUrl);
      encryptedData.setEncryptedValue(encryptedValue);
      return encryptedData;
    }

    if (isEmpty(value)) {
      return encryptedData;
    }

    // With existing encrypted value. Need to delete it first and rewrite with new value.
    logger.info("Deleting vault secret {} for {} in {} ms.", keyUrl, encryptedData.getUuid(),
        System.currentTimeMillis() - startTime);
    String fullPath = getFullPath(vaultConfig.getBasePath(), keyUrl);
    VaultRestClientFactory.create(vaultConfig)
        .deleteSecret(String.valueOf(vaultConfig.getAuthToken()), vaultConfig.getSecretEngineName(), fullPath);

    boolean isSuccessful = VaultRestClientFactory.create(vaultConfig)
                               .writeSecret(String.valueOf(vaultConfig.getAuthToken()),
                                   vaultConfig.getSecretEngineName(), fullPath, value);

    if (isSuccessful) {
      logger.info("Done saving vault secret {} for {}", keyUrl, encryptedData.getUuid());
      encryptedData.setEncryptionKey(keyUrl);
      encryptedData.setEncryptedValue(encryptedValue);
      return encryptedData;
    } else {
      String errorMsg = "Encryption request for " + name + " was not successful.";
      logger.error(errorMsg);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
    }
  }

  private char[] decryptInternal(EncryptedRecord data, VaultConfig vaultConfig) throws Exception {
    String fullPath =
        isEmpty(data.getPath()) ? getFullPath(vaultConfig.getBasePath(), data.getEncryptionKey()) : data.getPath();
    long startTime = System.currentTimeMillis();
    logger.info("Reading secret {} from vault {}", fullPath, vaultConfig.getVaultUrl());

    String value =
        VaultRestClientFactory.create(vaultConfig)
            .readSecret(String.valueOf(vaultConfig.getAuthToken()), vaultConfig.getSecretEngineName(), fullPath);

    if (isNotEmpty(value)) {
      logger.info("Done reading secret {} from vault {} in {} ms.", fullPath, vaultConfig.getVaultUrl(),
          System.currentTimeMillis() - startTime);
      return value.toCharArray();
    } else {
      String errorMsg = "Secret key path '" + fullPath + "' is invalid.";
      logger.error(errorMsg);
      throw new SecretManagementDelegateException(VAULT_OPERATION_ERROR, errorMsg, USER);
    }
  }

  @Override
  public EncryptedRecord encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      AzureVaultConfig azureConfig, EncryptedRecord savedEncryptedData) {
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            ()
                -> encryptInternal(name, value, accountId, settingType, azureConfig, savedEncryptedData),
            15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("encryption failed. trial num: {}", failedAttempts, e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Encryption failed after " + NUM_OF_RETRIES + " retries";
            throw new SecretManagementDelegateException(AZURE_KEY_VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        } else {
          String message = "Failed to encrypt Vault secret " + name;
          throw new SecretManagementDelegateException(AZURE_KEY_VAULT_OPERATION_ERROR, message, e, USER);
        }
      }
    }
  }

  @Override
  public char[] decrypt(EncryptedRecord data, AzureVaultConfig azureConfig) {
    if (data.getEncryptedValue() == null) {
      return null;
    }

    int failedAttempts = 0;
    while (true) {
      try {
        logger.info("Trying to decrypt record {} by {}", data.getEncryptionKey(), azureConfig.getVaultName());
        return timeLimiter.callWithTimeout(() -> decryptInternal(data, azureConfig), 15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("decryption failed. trial num: {}", failedAttempts, e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Decryption failed after " + NUM_OF_RETRIES + " retries";
            throw new SecretManagementDelegateException(AZURE_KEY_VAULT_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        } else {
          String message = "Failed to decrypt secret " + data.getName();
          throw new SecretManagementDelegateException(AZURE_KEY_VAULT_OPERATION_ERROR, message, e, USER);
        }
      }
    }
  }

  private EncryptedRecord encryptInternal(String fullSecretName, String value, String accountId,
      SettingVariableTypes type, AzureVaultConfig secretsManagerConfig, EncryptedRecord savedEncryptedData) {
    logger.info("Saving secret '{}' into Azure Secrets Manager: {}", fullSecretName, secretsManagerConfig.getName());
    long startTime = System.currentTimeMillis();

    boolean pathReference = false;
    String path = null;
    if (savedEncryptedData != null && isNotEmpty(savedEncryptedData.getPath())) {
      pathReference = true;
      path = savedEncryptedData.getPath();
    }

    EncryptedData encryptedData = savedEncryptedData != null ? (EncryptedData) savedEncryptedData
                                                             : EncryptedData.builder()
                                                                   .encryptionKey(fullSecretName)
                                                                   .encryptionType(EncryptionType.AZURE_VAULT)
                                                                   .enabled(true)
                                                                   .accountId(accountId)
                                                                   .kmsId(secretsManagerConfig.getUuid())
                                                                   .build();

    if (!pathReference && isEmpty(value)) {
      return encryptedData;
    }

    KeyVaultClient azureVaultClient = getAzureVaultClient(secretsManagerConfig);

    if (pathReference) {
      SecretBundle secret = null;
      try {
        AzureParsedSecretReference parsedSecretReference = new AzureParsedSecretReference(path);

        secret = azureVaultClient.getSecret(secretsManagerConfig.getEncryptionServiceUrl(),
            parsedSecretReference.getSecretName(), parsedSecretReference.getSecretVersion());
      } catch (Exception ex) {
        logger.error("Couldn't retrieve referenced secret", ex);
      }

      if (secret == null) {
        String message = "Secret name reference '" + savedEncryptedData.getPath() + "' is invalid";
        throw new SecretManagementDelegateException(AZURE_KEY_VAULT_OPERATION_ERROR, message, USER);
      } else {
        encryptedData.setEncryptionKey(savedEncryptedData.getPath());
        encryptedData.setEncryptedValue(secret.id().toCharArray());

        return encryptedData;
      }
    }

    // Create and updates are done in Azure using the same API. A set call will update the secret
    SetSecretRequest setSecretRequest =
        new SetSecretRequest.Builder(secretsManagerConfig.getEncryptionServiceUrl(), fullSecretName, value)
            .withTags(getMetadata(type))
            .build();

    SecretBundle secretBundle;
    try {
      secretBundle = azureVaultClient.setSecret(setSecretRequest);
    } catch (Exception ex) {
      String message = format("Failed to save secret in Azure Vault. Secret name: %s, Vault name: %s, accountId: %s",
          fullSecretName, secretsManagerConfig.getVaultName(), accountId);
      throw new SecretManagementDelegateException(AZURE_KEY_VAULT_OPERATION_ERROR, message, ex, USER);
    }
    encryptedData.setEncryptedValue(secretBundle.id().toCharArray());
    encryptedData.setEncryptionKey(fullSecretName);

    logger.info("Done saving secret {} into Azure Secrets Manager for {} in {} ms", fullSecretName,
        encryptedData.getUuid(), System.currentTimeMillis() - startTime);
    return encryptedData;
  }

  private char[] decryptInternal(EncryptedRecord data, AzureVaultConfig azureConfig) {
    long startTime = System.currentTimeMillis();

    AzureParsedSecretReference parsedSecretReference = isNotEmpty(data.getPath())
        ? new AzureParsedSecretReference(data.getPath())
        : new AzureParsedSecretReference(data.getEncryptionKey());

    KeyVaultClient azureVaultClient = getAzureVaultClient(azureConfig);
    try {
      SecretBundle secret = azureVaultClient.getSecret(azureConfig.getEncryptionServiceUrl(),
          parsedSecretReference.getSecretName(), parsedSecretReference.getSecretVersion());

      logger.info("Done decrypting Azure secret {} in {} ms", parsedSecretReference.getSecretName(),
          System.currentTimeMillis() - startTime);
      return secret.value().toCharArray();
    } catch (Exception ex) {
      String message = format("Failed to decrypt Azure secret %s in vault %s in account %s",
          parsedSecretReference.getSecretName(), azureConfig.getName(), azureConfig.getAccountId());
      throw new SecretManagementDelegateException(AZURE_KEY_VAULT_OPERATION_ERROR, message, ex, USER);
    }
  }

  @Override
  public boolean delete(AzureVaultConfig config, String key) {
    KeyVaultClient azureVaultClient = getAzureVaultClient(config);
    try {
      azureVaultClient.deleteSecret(config.getEncryptionServiceUrl(), key);
      logger.info("deletion of key {} in azure vault {} was successful.", key, config.getVaultName());
      return true;
    } catch (Exception ex) {
      logger.error("Failed to delete key {} from azure vault: {}", key, config.getVaultName(), ex);
      return false;
    }
  }

  @Override
  public char[] decrypt(EncryptedRecord data, CyberArkConfig cyberArkConfig) {
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(() -> decryptInternal(data, cyberArkConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("decryption failed. trial num: {}", failedAttempts, e);
        if (e instanceof SecretManagementDelegateException) {
          throw(SecretManagementDelegateException) e;
        } else if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String message = "Decryption failed after " + NUM_OF_RETRIES + " retries";
            throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, message, e, USER);
          }
          sleep(ofMillis(1000));
        } else {
          String message = "Failed to decrypt CyberArk secret " + data.getName();
          throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, message, e, USER);
        }
      }
    }
  }

  @Override
  public boolean validateCyberArkConfig(CyberArkConfig cyberArkConfig) {
    String errorMessage;
    // Basic connectivity and certificate validity checks
    if (isNotEmpty(cyberArkConfig.getClientCertificate())
        && !CyberArkRestClientFactory.validateClientCertificate(cyberArkConfig.getClientCertificate())) {
      errorMessage = "Client certificate provided is not valid. Please check your configurations and try again";
      throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMessage, USER);
    }

    try {
      CyberArkRestClient restClient = CyberArkRestClientFactory.create(cyberArkConfig);
      String testQuery = "Username=svc_account_harness_validate_config";
      Response<CyberArkReadResponse> response = restClient.readSecret(cyberArkConfig.getAppId(), testQuery).execute();
      // Expecting a 404 response (or 200 by accident) as the test query of a non-existent account in normal cases.
      int status = response.code();
      if (status != 404 && status != 200) {
        errorMessage = "Failed to query the CyberArk REST endpoint. Please check your configurations and try again";
        throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMessage, USER);
      }
    } catch (IOException e) {
      errorMessage = "Failed to test a sample CyberArk query. Please check your configurations and try again";
      throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMessage, e, USER);
    }

    return false;
  }

  private char[] decryptInternal(EncryptedRecord data, CyberArkConfig cyberArkConfig) {
    String appId = cyberArkConfig.getAppId();
    // Sample query params: "Address=components;Username=svc_account" or
    // "Safe=Test;Folder=root\OS\Windows;Object=windows1"
    String query = data.getPath();

    if (isEmpty(query)) {
      String errorMessage =
          "Query parameter is mandatory but it's not present in the encrypted record for CyberArk secret manager "
          + cyberArkConfig.getUuid();
      throw new IllegalArgumentException(errorMessage);
    }

    long startTime = System.currentTimeMillis();
    logger.info(
        "Reading secret CyberArk {} using AppID '{}' and query '{}'", cyberArkConfig.getCyberArkUrl(), appId, query);

    String secretValue = null;
    try {
      Response<CyberArkReadResponse> response =
          CyberArkRestClientFactory.create(cyberArkConfig).readSecret(appId, query).execute();
      if (response != null && response.isSuccessful()) {
        secretValue = response.body().getContent();
      }
    } catch (IOException e) {
      logger.error("Failed to read secret from CyberArk", e);
    }

    if (isNotEmpty(secretValue)) {
      logger.info("Done reading secret {} from CyberArk {} in {} ms.", query, cyberArkConfig.getCyberArkUrl(),
          System.currentTimeMillis() - startTime);
      return secretValue.toCharArray();
    } else {
      String errorMsg = "CyberArk query '" + query + "' is invalid in application '" + appId + "'";
      logger.error(errorMsg);
      throw new SecretManagementDelegateException(CYBERARK_OPERATION_ERROR, errorMsg, USER);
    }
  }

  private HashMap<String, String> getMetadata(SettingVariableTypes type) {
    return new HashMap<String, String>() {
      {
        put("createdBy", "Harness");
        put("type", type.name());
      }
    };
  }

  private KeyVaultClient getAzureVaultClient(AzureVaultConfig azureVaultConfig) {
    return KeyVaultADALAuthenticator.getClient(azureVaultConfig.getClientId(), azureVaultConfig.getSecretKey());
  }

  private ParsedSecretRef parsedSecretRef(String path) {
    String[] parts = path.split(KEY_SEPARATOR);
    ParsedSecretRef secretRef = new ParsedSecretRef();
    secretRef.secretPath = parts[0];
    if (parts.length > 1) {
      secretRef.keyName = parts[1];
    }
    return secretRef;
  }

  @ToString
  private class ParsedSecretRef {
    String secretPath;
    String keyName;
  }
}
