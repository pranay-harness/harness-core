package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static software.wings.helpers.ext.vault.VaultRestClientFactory.getFullPath;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.AWSKMSException;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.DependencyTimeoutException;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;
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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.beans.EmbeddedUser;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.DelegateRetryableException;
import io.harness.exception.KmsOperationException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.helpers.ext.vault.VaultRestClientFactory;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.service.impl.security.VaultSecretMetadata.VersionMetadata;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@Singleton
@Slf4j
public class SecretManagementDelegateServiceImpl implements SecretManagementDelegateService {
  private static final String REASON_KEY = "reason";

  private TimeLimiter timeLimiter;

  @Inject
  public SecretManagementDelegateServiceImpl(TimeLimiter timeLimiter) {
    this.timeLimiter = timeLimiter;
  }

  // Caffeine cache is a high performance java cache just like Guava Cache. Below is the benchmark result
  // https://github.com/ben-manes/caffeine/wiki/Benchmarks
  private Cache<KmsEncryptionKeyCacheKey, byte[]> kmsEncryptionKeyCache =
      Caffeine.newBuilder().maximumSize(2000).expireAfterAccess(2, TimeUnit.HOURS).build();

  long getKmsEncryptionKeyCacheSize() {
    return kmsEncryptionKeyCache.estimatedSize();
  }

  private boolean isRetryable(Exception e) {
    // TimeLimiter.callWithTimer will throw a new exception wrapping around the AwsKMS exceptions. Unwrap it.
    Throwable t = e.getCause() == null ? e : e.getCause();

    if (t instanceof AWSSecretsManagerException) {
      logger.info("Got AWSSecretsManagerException {}: {}", t.getClass().getName(), t.getMessage());
      return !(t instanceof ResourceNotFoundException);
    } else if (t instanceof AWSKMSException) {
      logger.info("Got AWSKMSEception {}: {}", t.getClass().getName(), t.getMessage());
      return t instanceof KMSInternalException || t instanceof DependencyTimeoutException
          || t instanceof KeyUnavailableException;
    } else {
      // Else if not IllegalArgumentException, it should retry.
      return !(t instanceof IllegalArgumentException);
    }
  }

  @Override
  public EncryptedRecord encrypt(String accountId, char[] value, KmsConfig kmsConfig) {
    Preconditions.checkNotNull(kmsConfig, "null for " + accountId);
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> encryptInternal(accountId, value, kmsConfig), 5, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn(format("Encryption failed. trial num: %d", failedAttempts), e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String reason = format("Encryption failed after %d retries", NUM_OF_RETRIES);
            throw new DelegateRetryableException(new KmsOperationException(reason, e, USER));
          }
          sleep(ofMillis(1000));
        } else {
          throw new KmsOperationException(e.getMessage(), e, USER);
        }
      }
    }
  }

  @Override
  public char[] decrypt(EncryptedRecord data, KmsConfig kmsConfig) {
    if (data.getEncryptedValue() == null) {
      return null;
    }

    Preconditions.checkNotNull(kmsConfig, "null for " + data);

    int failedAttempts = 0;
    while (true) {
      try {
        KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
        byte[] cachedEncryptedKey = kmsEncryptionKeyCache.getIfPresent(cacheKey);
        if (isNotEmpty(cachedEncryptedKey)) {
          return decryptInternalIfCached(cacheKey, data, cachedEncryptedKey, System.currentTimeMillis());
        } else {
          // Use TimeLimiter.callWithTimeout only if the KMS plain text key is not cached.
          return timeLimiter.callWithTimeout(() -> decryptInternal(data, kmsConfig), 5, TimeUnit.SECONDS, true);
        }
      } catch (Exception e) {
        failedAttempts++;
        logger.warn(format("Decryption failed. trial num: %d", failedAttempts), e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String reason =
                format("Decryption failed for encryptedData %s after %d retries", data.getName(), NUM_OF_RETRIES);
            throw new DelegateRetryableException(new KmsOperationException(reason, e, USER));
          }
          sleep(ofMillis(1000));
        } else {
          throw new KmsOperationException(e.getMessage(), e, USER);
        }
      }
    }
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
        logger.warn(format("encryption failed. trial num: %d", failedAttempts), e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
                .addParam(REASON_KEY, "encryption failed after " + NUM_OF_RETRIES + " retries");
          }
          sleep(ofMillis(1000));
        } else {
          throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, e.getMessage(), USER)
              .addParam(REASON_KEY, "Failed to encrypt Vault secret " + name);
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
        logger.warn(format("decryption failed. trial num: %d", failedAttempts), e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
                .addParam(REASON_KEY, "Decryption failed after " + NUM_OF_RETRIES + " retries");
          }
          sleep(ofMillis(1000));
        } else {
          throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, e.getMessage(), USER)
              .addParam(REASON_KEY, "Failed to decrypt Vault secret " + data.getName());
        }
      }
    }
  }

  @Override
  public boolean deleteVaultSecret(String path, VaultConfig vaultConfig) {
    try {
      String fullPath = getFullPath(vaultConfig.getBasePath(), path);
      return VaultRestClientFactory.create(vaultConfig)
          .deleteSecret(String.valueOf(vaultConfig.getAuthToken()), fullPath);
    } catch (IOException e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
          .addParam(REASON_KEY, "Deletion of secret failed");
    }
  }

  @Override
  public List<SecretChangeLog> getVaultSecretChangeLogs(EncryptedRecord encryptedData, VaultConfig vaultConfig) {
    List<SecretChangeLog> secretChangeLogs = new ArrayList<>();

    EmbeddedUser vaultUser = EmbeddedUser.builder().name("VaultUser").build();
    String encryptedDataId = encryptedData.getUuid();

    try {
      VaultSecretMetadata secretMetadata = VaultRestClientFactory.create(vaultConfig)
                                               .readSecretMetadata(vaultConfig.getAuthToken(), encryptedData.getPath());
      if (secretMetadata != null && isNotEmpty(secretMetadata.getVersions())) {
        for (Entry<Integer, VersionMetadata> entry : secretMetadata.getVersions().entrySet()) {
          int version = entry.getKey();
          VersionMetadata versionMetadata = entry.getValue();
          final String changeDescription;
          final String changeTime;
          if (versionMetadata.destroyed) {
            changeDescription = "Deleted at version " + version + " in Vault";
            changeTime = versionMetadata.deletionTime;
          } else {
            changeDescription = version == 1 ? "Created in Vault" : "Updated to version " + version + " in Vault";
            changeTime = versionMetadata.createdTime;
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
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
          .addParam(REASON_KEY, "Retrieval of vault secret version history failed");
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
        logger.warn(format("renewal failed. trial num: %d", failedAttempts), e);
        if (failedAttempts == NUM_OF_RETRIES) {
          throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
              .addParam(REASON_KEY, "renewal failed after " + NUM_OF_RETRIES + " retries");
        }
        sleep(ofMillis(1000));
      }
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
        logger.warn(format("encryption failed. trial num: %d", failedAttempts), e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            throw new WingsException(ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR, USER, e)
                .addParam(REASON_KEY, "encryption failed after " + NUM_OF_RETRIES + " retries");
          }
          sleep(ofMillis(1000));
        } else {
          throw new WingsException(ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR, USER, e)
              .addParam(REASON_KEY, "Failed to encrypt secret " + name);
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
        logger.warn(format("decryption failed. trial num: %d", failedAttempts), e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            throw new WingsException(ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR, USER, e)
                .addParam(REASON_KEY, "Decryption failed after " + NUM_OF_RETRIES + " retries");
          }
          sleep(ofMillis(1000));
        } else {
          throw new WingsException(ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR, USER, e)
              .addParam(REASON_KEY, "Failed to decrypt secret " + data.getName());
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
    boolean pathReference = false;
    if (isNotEmpty(savedEncryptedData.getPath())) {
      pathReference = true;
      fullSecretName = savedEncryptedData.getPath();
    } else {
      fullSecretName = getFullPath(secretsManagerConfig.getSecretNamePrefix(), name);
    }

    long startTime = System.currentTimeMillis();
    logger.info("Saving secret '{}' into AWS Secrets Manager: {}", fullSecretName, secretsManagerConfig.getName());

    EncryptedRecord encryptedData = savedEncryptedData;
    if (!pathReference && isEmpty(value)) {
      return encryptedData;
    }

    AWSSecretsManager client = getAwsSecretsManagerClient(secretsManagerConfig);

    // Get the secret value to see if it exists already.
    boolean secretExists = false;
    try {
      GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(fullSecretName);
      GetSecretValueResult result = client.getSecretValue(getSecretValueRequest);
      secretExists = isNotEmpty(result.getARN());
    } catch (ResourceNotFoundException e) {
      // If reaching here, it means the resource doesn't exist.
    }

    if (!secretExists) {
      // If it's a secret reference, the referred secret has to exist for this reference creation to succeed.
      if (pathReference) {
        throw new WingsException(ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR, USER)
            .addParam(REASON_KEY, "Secret name reference '" + savedEncryptedData.getPath() + "' is invalid");
      }

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

    AWSSecretsManager client = getAwsSecretsManagerClient(secretsManagerConfig);

    String secretName = data.getEncryptionKey();
    GetSecretValueRequest request = new GetSecretValueRequest().withSecretId(secretName);
    GetSecretValueResult result = client.getSecretValue(request);

    logger.info("Done decrypting AWS secret {} in {}ms", secretName, System.currentTimeMillis() - startTime);
    return result.getSecretString().toCharArray();
  }

  private AWSSecretsManager getAwsSecretsManagerClient(AwsSecretsManagerConfig secretsManagerConfig) {
    return AWSSecretsManagerClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(secretsManagerConfig.getAccessKey(), secretsManagerConfig.getSecretKey())))
        .withRegion(secretsManagerConfig.getRegion() == null ? Regions.US_EAST_1
                                                             : Regions.fromName(secretsManagerConfig.getRegion()))
        .build();
  }

  private EncryptedRecord encryptInternal(String accountId, char[] value, KmsConfig kmsConfig) throws Exception {
    long startTime = System.currentTimeMillis();
    logger.info("Encrypting one secret in account {} with KMS secret manager '{}'", accountId, kmsConfig.getName());

    final AWSKMS kmsClient =
        AWSKMSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(kmsConfig.getAccessKey(), kmsConfig.getSecretKey())))
            .withRegion(kmsConfig.getRegion() == null ? Regions.US_EAST_1 : Regions.fromName(kmsConfig.getRegion()))
            .build();
    GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
    dataKeyRequest.setKeyId(kmsConfig.getKmsArn());
    dataKeyRequest.setKeySpec("AES_128");
    GenerateDataKeyResult dataKeyResult = kmsClient.generateDataKey(dataKeyRequest);

    ByteBuffer plainTextKey = dataKeyResult.getPlaintext();

    char[] encryptedValue =
        value == null ? null : encrypt(new String(value), new SecretKeySpec(getByteArray(plainTextKey), "AES"));
    String encryptedKeyString = StandardCharsets.ISO_8859_1.decode(dataKeyResult.getCiphertextBlob()).toString();

    // Shutdown the KMS client so as to prevent resource leaking,
    kmsClient.shutdown();

    logger.info("Finished encrypting one secret in account {} with KMS secret manager '{}' in {} ms.", accountId,
        kmsConfig.getName(), System.currentTimeMillis() - startTime);
    return EncryptedData.builder()
        .encryptionKey(encryptedKeyString)
        .encryptedValue(encryptedValue)
        .encryptionType(EncryptionType.KMS)
        .kmsId(kmsConfig.getUuid())
        .enabled(true)
        .parentIds(new HashSet<>())
        .accountId(accountId)
        .build();
  }

  private char[] decryptInternal(EncryptedRecord data, KmsConfig kmsConfig) throws Exception {
    long startTime = System.currentTimeMillis();
    logger.info("Decrypting secret {} with KMS secret manager '{}'", data.getUuid(), kmsConfig.getName());

    KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
    // HAR-9752: Caching KMS encryption key to plain text key mapping to reduce KMS decrypt call volume.
    byte[] encryptedPlainTextKey = kmsEncryptionKeyCache.asMap().computeIfAbsent(cacheKey, key -> {
      ByteBuffer plainTextKey = getPlainTextKeyFromKMS(kmsConfig, key.encryptionKey);
      // Encrypt plain text KMS key before caching it in memory.
      byte[] encryptedKey = encryptPlainTextKey(plainTextKey, key.uuid);

      logger.info("Decrypted encryption key from KMS secret manager '{}' in {} ms.", kmsConfig.getName(),
          System.currentTimeMillis() - startTime);
      return encryptedKey;
    });

    return decryptInternalIfCached(cacheKey, data, encryptedPlainTextKey, startTime);
  }

  private char[] decryptInternalIfCached(KmsEncryptionKeyCacheKey cacheKey, EncryptedRecord data,
      byte[] encryptedPlainTextKey, long startTime) throws Exception {
    byte[] plainTextKey = decryptPlainTextKey(encryptedPlainTextKey, cacheKey.uuid);
    char[] decryptedSecret = decrypt(data.getEncryptedValue(), new SecretKeySpec(plainTextKey, "AES")).toCharArray();

    logger.info("Finished decrypting secret {} in {} ms.", data.getUuid(), System.currentTimeMillis() - startTime);
    return decryptedSecret;
  }

  private ByteBuffer getPlainTextKeyFromKMS(KmsConfig kmsConfig, String encryptionKey) {
    final AWSKMS kmsClient =
        AWSKMSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(kmsConfig.getAccessKey(), kmsConfig.getSecretKey())))
            .withRegion(kmsConfig.getRegion() == null ? Regions.US_EAST_1 : Regions.fromName(kmsConfig.getRegion()))
            .build();

    DecryptRequest decryptRequest =
        new DecryptRequest().withCiphertextBlob(StandardCharsets.ISO_8859_1.encode(encryptionKey));
    new DecryptRequest().withCiphertextBlob(StandardCharsets.ISO_8859_1.encode(encryptionKey));
    ByteBuffer plainTextKey = kmsClient.decrypt(decryptRequest).getPlaintext();

    // Shutdown the KMS client so as to prevent resource leaking,
    kmsClient.shutdown();

    return plainTextKey;
  }

  private EncryptedRecord encryptInternal(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedRecord savedEncryptedData) throws Exception {
    String keyUrl = settingType + "/" + name;
    char[] encryptedValue = keyUrl.toCharArray();

    long startTime = System.currentTimeMillis();
    logger.info("Saving secret {} into Vault {}", name, keyUrl);

    EncryptedRecord encryptedData = savedEncryptedData;
    if (savedEncryptedData == null) {
      encryptedData = EncryptedData.builder()
                          .encryptionKey(keyUrl)
                          .encryptedValue(encryptedValue)
                          .encryptionType(EncryptionType.VAULT)
                          .enabled(true)
                          .accountId(accountId)
                          .parentIds(new HashSet<>())
                          .kmsId(vaultConfig.getUuid())
                          .build();
    }

    // When secret path is specified, no need to write secret, since the secret is already created and is just being
    // referred by a secret text.
    if (isNotEmpty(encryptedData.getPath())) {
      // Try decrypting to make sure the 'path' specified is pointing to a valid Vault path.
      char[] referredSecretValue = decryptInternal(encryptedData, vaultConfig);
      if (isEmpty(referredSecretValue)) {
        throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER)
            .addParam(REASON_KEY, "Vault path '" + encryptedData.getPath() + "' is not referring to any valid secret.");
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
    VaultRestClientFactory.create(vaultConfig).deleteSecret(String.valueOf(vaultConfig.getAuthToken()), fullPath);

    boolean isSuccessful = VaultRestClientFactory.create(vaultConfig)
                               .writeSecret(String.valueOf(vaultConfig.getAuthToken()), fullPath, value);

    if (isSuccessful) {
      logger.info("Done saving vault secret {} for {}", keyUrl, encryptedData.getUuid());
      encryptedData.setEncryptionKey(keyUrl);
      encryptedData.setEncryptedValue(encryptedValue);
      return encryptedData;
    } else {
      String errorMsg = "Request not successful.";
      logger.error(errorMsg);
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER).addParam(REASON_KEY, errorMsg);
    }
  }

  private char[] decryptInternal(EncryptedRecord data, VaultConfig vaultConfig) throws Exception {
    String fullPath =
        isEmpty(data.getPath()) ? getFullPath(vaultConfig.getBasePath(), data.getEncryptionKey()) : data.getPath();
    long startTime = System.currentTimeMillis();
    logger.info("Reading secret {} from vault {}", fullPath, vaultConfig.getVaultUrl());

    String value =
        VaultRestClientFactory.create(vaultConfig).readSecret(String.valueOf(vaultConfig.getAuthToken()), fullPath);

    if (EmptyPredicate.isNotEmpty(value)) {
      logger.info("Done reading secret {} from vault {} in {} ms.", fullPath, vaultConfig.getVaultUrl(),
          System.currentTimeMillis() - startTime);
      return value.toCharArray();
    } else {
      String errorMsg = "Secret key path '" + fullPath + "' is invalid.";
      logger.error(errorMsg);
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER).addParam(REASON_KEY, errorMsg);
    }
  }

  public static char[] encrypt(String src, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                                           InvalidKeyException, IllegalBlockSizeException,
                                                           BadPaddingException {
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    return encodeBase64(cipher.doFinal(src.getBytes(Charsets.UTF_8))).toCharArray();
  }

  public static String decrypt(char[] src, Key key) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                                           InvalidKeyException, IllegalBlockSizeException,
                                                           BadPaddingException {
    if (src == null) {
      return null;
    }
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, key);
    return new String(cipher.doFinal(decodeBase64(src)), Charsets.UTF_8);
  }

  private byte[] encryptPlainTextKey(ByteBuffer plainTextKey, String localEncryptionKey) {
    SimpleEncryption simpleEncryption = new SimpleEncryption(localEncryptionKey);
    return simpleEncryption.encrypt(getByteArray(plainTextKey));
  }

  private byte[] decryptPlainTextKey(byte[] encryptedPlainTextKey, String localEncryptionKey) {
    SimpleEncryption simpleEncryption = new SimpleEncryption(localEncryptionKey);
    return simpleEncryption.decrypt(encryptedPlainTextKey);
  }

  private byte[] getByteArray(ByteBuffer b) {
    byte[] byteArray = new byte[b.remaining()];
    b.get(byteArray);
    return byteArray;
  }

  @Data
  @AllArgsConstructor
  @EqualsAndHashCode
  private class KmsEncryptionKeyCacheKey {
    String uuid;
    String encryptionKey;
  }
}
