package software.wings.service.impl.security.kms;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.KMS_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static software.wings.service.impl.security.SecretManagementDelegateServiceImpl.isRetryable;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.KmsConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementDelegateException;
import software.wings.service.intfc.security.SecretManager;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author marklu on 9/24/19
 */
@Singleton
@Slf4j
public class KmsEncryptDecryptClient {
  private static final int DEFAULT_KMS_TIMEOUT = 30; // in seconds
  private TimeLimiter timeLimiter;

  // Caffeine cache is a high performance java cache just like Guava Cache. Below is the benchmark result
  // https://github.com/ben-manes/caffeine/wiki/Benchmarks
  private Cache<KmsEncryptionKeyCacheKey, byte[]> kmsEncryptionKeyCache =
      Caffeine.newBuilder().maximumSize(2000).expireAfterAccess(2, TimeUnit.HOURS).build();

  @Inject
  public KmsEncryptDecryptClient(TimeLimiter timeLimiter) {
    this.timeLimiter = timeLimiter;
  }

  public EncryptedRecord encrypt(String accountId, char[] value, KmsConfig kmsConfig) {
    if (kmsConfig == null) {
      throw new SecretManagementDelegateException(
          KMS_OPERATION_ERROR, "null secret manager for account " + accountId, USER);
    }

    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(
            () -> encryptInternal(accountId, value, kmsConfig), DEFAULT_KMS_TIMEOUT, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("Encryption failed. trial num: {}", failedAttempts, e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String reason = format("Encryption failed after %d retries", NUM_OF_RETRIES);
            throw new DelegateRetryableException(
                new SecretManagementDelegateException(KMS_OPERATION_ERROR, reason, e, USER));
          }
          sleep(ofMillis(1000));
        } else {
          throw new SecretManagementDelegateException(KMS_OPERATION_ERROR, e.getMessage(), e, USER);
        }
      }
    }
  }

  public EncryptedRecordData convertEncryptedRecordToLocallyEncrypted(
      EncryptedData encryptedRecord, KmsConfig kmsConfig) {
    try {
      char[] decryptedValue = decrypt(encryptedRecord, kmsConfig);
      String randomEncryptionKey = UUIDGenerator.generateUuid();
      char[] reEncryptedValue = new SimpleEncryption(randomEncryptionKey).encryptChars(decryptedValue);

      return EncryptedRecordData.builder()
          .uuid(encryptedRecord.getUuid())
          .name(encryptedRecord.getName())
          .encryptionType(LOCAL)
          .encryptionKey(randomEncryptionKey)
          .encryptedValue(reEncryptedValue)
          .build();
    } catch (DelegateRetryableException | SecretManagementDelegateException e) {
      logger.warn(
          "Failed to decrypt secret {} with secret manager {}. Falling back to decrypt this secret using delegate",
          encryptedRecord.getUuid(), kmsConfig.getUuid(), e);
      // This means we are falling back to use delegate to decrypt.
      return SecretManager.buildRecordData(encryptedRecord);
    }
  }

  public char[] decrypt(EncryptedRecord data, KmsConfig kmsConfig) {
    if (data.getEncryptedValue() == null) {
      return null;
    }
    if (kmsConfig == null) {
      throw new SecretManagementDelegateException(
          KMS_OPERATION_ERROR, "null secret manager for encrypted record " + data.getUuid(), USER);
    }

    int failedAttempts = 0;
    while (true) {
      try {
        byte[] cachedEncryptedKey = getCachedEncryptedKey(data);
        if (isNotEmpty(cachedEncryptedKey)) {
          return decryptInternalIfCached(data, cachedEncryptedKey, System.currentTimeMillis());
        } else {
          // Use TimeLimiter.callWithTimeout only if the KMS plain text key is not cached.
          return timeLimiter.callWithTimeout(
              () -> decryptInternal(data, kmsConfig), DEFAULT_KMS_TIMEOUT, TimeUnit.SECONDS, true);
        }
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("Decryption failed. trial num: {}", failedAttempts, e);
        if (isRetryable(e)) {
          if (failedAttempts == NUM_OF_RETRIES) {
            String reason =
                format("Decryption failed for encryptedData %s after %d retries", data.getName(), NUM_OF_RETRIES);
            throw new DelegateRetryableException(
                new SecretManagementDelegateException(KMS_OPERATION_ERROR, reason, e, USER));
          }
          sleep(ofMillis(1000));
        } else {
          throw new SecretManagementDelegateException(KMS_OPERATION_ERROR, e.getMessage(), e, USER);
        }
      }
    }
  }

  private GenerateDataKeyResult generateKmsKey(KmsConfig kmsConfig) {
    final AWSKMS kmsClient = getKmsClient(kmsConfig);

    try {
      GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
      dataKeyRequest.setKeyId(kmsConfig.getKmsArn());
      dataKeyRequest.setKeySpec("AES_128");
      return kmsClient.generateDataKey(dataKeyRequest);
    } finally {
      if (kmsClient != null) {
        // Shutdown the KMS client so as to prevent resource leaking,\
        kmsClient.shutdown();
      }
    }
  }

  public byte[] getPlainTextKeyFromKMS(KmsConfig kmsConfig, String encryptionKey) {
    AWSKMS kmsClient = null;
    try {
      kmsClient = getKmsClient(kmsConfig);

      DecryptRequest decryptRequest =
          new DecryptRequest().withCiphertextBlob(StandardCharsets.ISO_8859_1.encode(encryptionKey));
      ByteBuffer plainTextKey = kmsClient.decrypt(decryptRequest).getPlaintext();
      return getByteArray(plainTextKey);
    } finally {
      if (kmsClient != null) {
        // Shutdown the KMS client so as to prevent resource leaking,
        kmsClient.shutdown();
      }
    }
  }

  @VisibleForTesting
  AWSKMS getKmsClient(KmsConfig kmsConfig) {
    return AWSKMSClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(kmsConfig.getAccessKey(), kmsConfig.getSecretKey())))
        .withRegion(kmsConfig.getRegion() == null ? Regions.US_EAST_1 : Regions.fromName(kmsConfig.getRegion()))
        .build();
  }

  private EncryptedRecord encryptInternal(String accountId, char[] value, KmsConfig kmsConfig)
      throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
             NoSuchPaddingException {
    long startTime = System.currentTimeMillis();
    logger.info("Encrypting one secret in account {} with KMS secret manager '{}'", accountId, kmsConfig.getName());

    GenerateDataKeyResult dataKeyResult = generateKmsKey(kmsConfig);

    ByteBuffer plainTextKey = dataKeyResult.getPlaintext();

    char[] encryptedValue =
        value == null ? null : encrypt(new String(value), new SecretKeySpec(getByteArray(plainTextKey), "AES"));
    String encryptedKeyString = StandardCharsets.ISO_8859_1.decode(dataKeyResult.getCiphertextBlob()).toString();

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

  private char[] decryptInternal(EncryptedRecord data, KmsConfig kmsConfig)
      throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
             NoSuchPaddingException {
    long startTime = System.currentTimeMillis();
    logger.info("Decrypting secret {} with KMS secret manager '{}'", data.getUuid(), kmsConfig.getName());
    KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
    // HAR-9752: Caching KMS encryption key to plain text key mapping to reduce KMS decrypt call volume.
    byte[] encryptedPlainTextKey = kmsEncryptionKeyCache.get(cacheKey, key -> {
      byte[] plainTextKey = getPlainTextKeyFromKMS(kmsConfig, key.encryptionKey);
      // Encrypt plain text KMS key before caching it in memory.
      byte[] encryptedKey = encryptPlainTextKey(plainTextKey, key.uuid);

      logger.info("Decrypted encryption key from KMS secret manager '{}' in {} ms.", kmsConfig.getName(),
          System.currentTimeMillis() - startTime);
      return encryptedKey;
    });

    return decryptInternalIfCached(data, encryptedPlainTextKey, startTime);
  }

  private byte[] getCachedEncryptedKey(EncryptedRecord data) {
    KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
    return kmsEncryptionKeyCache.getIfPresent(cacheKey);
  }

  @VisibleForTesting
  char[] decryptInternalIfCached(EncryptedRecord data, byte[] encryptedPlainTextKey, long startTime)
      throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
             NoSuchPaddingException {
    KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
    byte[] plainTextKey = decryptPlainTextKey(encryptedPlainTextKey, cacheKey.uuid);
    String decrypted = decrypt(data.getEncryptedValue(), new SecretKeySpec(plainTextKey, "AES"));

    logger.info("Finished decrypting secret {} in {} ms.", data.getUuid(), System.currentTimeMillis() - startTime);
    return decrypted == null ? null : decrypted.toCharArray();
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

  private byte[] encryptPlainTextKey(byte[] plainTextKey, String localEncryptionKey) {
    SimpleEncryption simpleEncryption = new SimpleEncryption(localEncryptionKey);
    return simpleEncryption.encrypt(plainTextKey);
  }

  @VisibleForTesting
  byte[] decryptPlainTextKey(byte[] encryptedPlainTextKey, String localEncryptionKey) {
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
  public static class KmsEncryptionKeyCacheKey {
    String uuid;
    String encryptionKey;
  }
}
