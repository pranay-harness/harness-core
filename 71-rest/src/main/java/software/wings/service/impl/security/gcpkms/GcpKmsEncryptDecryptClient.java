package software.wings.service.impl.security.gcpkms;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GCP_KMS_OPERATION_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static software.wings.service.intfc.security.SecretManagementDelegateService.NUM_OF_RETRIES;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyManagementServiceSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.delegate.exception.DelegateRetryableException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.GcpKmsConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementDelegateException;
import software.wings.service.impl.security.kms.KmsEncryptDecryptClient.KmsEncryptionKeyCacheKey;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
public class GcpKmsEncryptDecryptClient {
  private static final int DEFAULT_GCP_KMS_TIMEOUT = 20;
  private TimeLimiter timeLimiter;

  private Cache<KmsEncryptionKeyCacheKey, byte[]> kmsEncryptionKeyCache =
      Caffeine.newBuilder().maximumSize(2000).expireAfterAccess(2, TimeUnit.HOURS).build();

  @Inject
  public GcpKmsEncryptDecryptClient(TimeLimiter timeLimiter) {
    this.timeLimiter = timeLimiter;
  }

  public EncryptedRecord encrypt(
      String value, String accountId, GcpKmsConfig gcpKmsConfig, EncryptedRecord savedEncryptedData) {
    if (gcpKmsConfig == null) {
      throw new SecretManagementDelegateException(
          GCP_KMS_OPERATION_ERROR, String.format("secret configuration is null for account %s", accountId), USER);
    }
    int failedAttempts = 0;
    while (true) {
      try {
        return timeLimiter.callWithTimeout(()
                                               -> encryptInternal(value, accountId, gcpKmsConfig, savedEncryptedData),
            DEFAULT_GCP_KMS_TIMEOUT, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("Encryption failed. Trial Number {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String reason = String.format("Encryption failed after %d retries", NUM_OF_RETRIES);
          throw new DelegateRetryableException(
              new SecretManagementDelegateException(GCP_KMS_OPERATION_ERROR, reason, USER));
        }
        sleep(Duration.ofMillis(1000));
      }
    }
  }

  private EncryptedRecord encryptInternal(String value, String accountId, GcpKmsConfig gcpKmsConfig,
      EncryptedRecord savedEncryptedData) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException,
                                                 NoSuchAlgorithmException, NoSuchPaddingException {
    long startTime = System.currentTimeMillis();
    logger.info("Encrypting one secret in account {} with KMS Secret Manager {}", accountId, gcpKmsConfig.getName());

    ByteString plainTextDek = generateDEK();
    String encryptedDek = encryptDekFromKms(gcpKmsConfig, plainTextDek);

    char[] encryptedValue =
        value == null ? null : encryptDataUsingDek(value, new SecretKeySpec(plainTextDek.toByteArray(), "AES"));
    logger.info("Finished encrypting one secret in account {} with KMS secret manager '{}' in {} ms.", accountId,
        gcpKmsConfig.getName(), System.currentTimeMillis() - startTime);

    EncryptedData encryptedData = savedEncryptedData != null ? (EncryptedData) savedEncryptedData
                                                             : EncryptedData.builder()
                                                                   .encryptionType(EncryptionType.GCP_KMS)
                                                                   .kmsId(gcpKmsConfig.getUuid())
                                                                   .enabled(true)
                                                                   .parentIds(new HashSet<>())
                                                                   .accountId(accountId)
                                                                   .build();
    encryptedData.setEncryptedValue(encryptedValue);
    encryptedData.setEncryptionKey(encryptedDek);
    return encryptedData;
  }

  private static char[] encryptDataUsingDek(String src, Key key) throws NoSuchAlgorithmException,
                                                                        NoSuchPaddingException, InvalidKeyException,
                                                                        IllegalBlockSizeException, BadPaddingException {
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    return encodeBase64(cipher.doFinal(src.getBytes(StandardCharsets.UTF_8))).toCharArray();
  }

  private ByteString generateDEK() {
    KeyGenerator keyGen;
    try {
      keyGen = KeyGenerator.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      String message = "Could not fetch DEK Key Generator";
      throw new SecretManagementDelegateException(GCP_KMS_OPERATION_ERROR, message, e, USER);
    }
    keyGen.init(128);
    SecretKey secretKey = keyGen.generateKey();
    return ByteString.copyFrom(secretKey.getEncoded());
  }

  public char[] decrypt(EncryptedRecord encryptedData, GcpKmsConfig gcpKmsConfig) {
    if (encryptedData.getEncryptedValue() == null) {
      return null;
    }
    if (gcpKmsConfig == null) {
      throw new SecretManagementDelegateException(
          GCP_KMS_OPERATION_ERROR, "null secret manager for encrypted record " + encryptedData.getUuid(), USER);
    }

    int failedAttempts = 0;
    while (true) {
      try {
        byte[] cachedEncryptedKey = getCachedEncryptedKey(encryptedData);
        if (isNotEmpty(cachedEncryptedKey)) {
          return decryptInternalIfCached(encryptedData, cachedEncryptedKey, System.currentTimeMillis());
        } else {
          // Use TimeLimiter.callWithTimeout only if the KMS plain text key is not cached.
          return timeLimiter.callWithTimeout(
              () -> decryptInternal(encryptedData, gcpKmsConfig), DEFAULT_GCP_KMS_TIMEOUT, TimeUnit.SECONDS, true);
        }
      } catch (Exception e) {
        failedAttempts++;
        logger.warn("Decryption failed. trial num: {}", failedAttempts, e);
        if (failedAttempts == NUM_OF_RETRIES) {
          String reason = format(
              "Decryption failed for encryptedData %s after %d retries", encryptedData.getName(), NUM_OF_RETRIES);
          throw new DelegateRetryableException(
              new SecretManagementDelegateException(GCP_KMS_OPERATION_ERROR, reason, USER));
        }
        sleep(ofMillis(1000));
      }
    }
  }

  private byte[] getCachedEncryptedKey(EncryptedRecord data) {
    KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
    return kmsEncryptionKeyCache.getIfPresent(cacheKey);
  }

  private char[] decryptInternal(EncryptedRecord data, GcpKmsConfig gcpKmsConfig)
      throws InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException,
             NoSuchPaddingException {
    long startTime = System.currentTimeMillis();
    logger.info("Decrypting secret {} with GCP KMS secret manager '{}'", data.getUuid(), gcpKmsConfig.getName());
    KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
    // HAR-9752: Caching KMS encryption key to plain text key mapping to reduce KMS decrypt call volume.
    byte[] encryptedPlainTextKey = kmsEncryptionKeyCache.get(cacheKey, key -> {
      byte[] plainTextKey = decryptDekFromKms(gcpKmsConfig, key.getEncryptionKey());
      // Encrypt plain text KMS key before caching it in memory.
      byte[] encryptedKey = simpleEncryptDek(plainTextKey, key.getUuid());

      logger.info("Decrypted encryption key from KMS secret manager '{}' in {} ms.", gcpKmsConfig.getName(),
          System.currentTimeMillis() - startTime);
      return encryptedKey;
    });

    return decryptInternalIfCached(data, encryptedPlainTextKey, startTime);
  }

  private char[] decryptInternalIfCached(EncryptedRecord data, byte[] encryptedPlainTextKey, long startTime)
      throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException,
             NoSuchPaddingException {
    KmsEncryptionKeyCacheKey cacheKey = new KmsEncryptionKeyCacheKey(data.getUuid(), data.getEncryptionKey());
    byte[] plainTextKey = simpleDecryptDek(encryptedPlainTextKey, cacheKey.getUuid());
    String decrypted = decryptDataUsingDek(data.getEncryptedValue(), new SecretKeySpec(plainTextKey, "AES"));

    logger.info("Finished decrypting secret {} in {} ms.", data.getUuid(), System.currentTimeMillis() - startTime);
    return decrypted == null ? null : decrypted.toCharArray();
  }

  private static String decryptDataUsingDek(char[] src, Key key) throws NoSuchAlgorithmException,
                                                                        NoSuchPaddingException, InvalidKeyException,
                                                                        IllegalBlockSizeException, BadPaddingException {
    if (src == null) {
      return null;
    }
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, key);
    return new String(cipher.doFinal(decodeBase64(src)), StandardCharsets.UTF_8);
  }

  private String encryptDekFromKms(GcpKmsConfig gcpKmsConfig, ByteString plainTextDek) {
    try (KeyManagementServiceClient gcpKmsClient = getClient(gcpKmsConfig)) {
      String resourceName = CryptoKeyName.format(
          gcpKmsConfig.getProjectId(), gcpKmsConfig.getRegion(), gcpKmsConfig.getKeyRing(), gcpKmsConfig.getKeyName());
      EncryptResponse encryptResponse = gcpKmsClient.encrypt(resourceName, plainTextDek);
      return encryptResponse.getCiphertext().toString(StandardCharsets.ISO_8859_1);
    }
  }

  private byte[] decryptDekFromKms(GcpKmsConfig gcpKmsConfig, String encryptionKey) {
    try (KeyManagementServiceClient client = getClient(gcpKmsConfig)) {
      String resourceName = CryptoKeyName.format(
          gcpKmsConfig.getProjectId(), gcpKmsConfig.getRegion(), gcpKmsConfig.getKeyRing(), gcpKmsConfig.getKeyName());
      ByteString key = ByteString.copyFrom(StandardCharsets.ISO_8859_1.encode(encryptionKey));
      DecryptResponse decryptResponse = client.decrypt(resourceName, key);
      return decryptResponse.getPlaintext().toByteArray();
    }
  }

  private byte[] simpleEncryptDek(byte[] plainTextKey, String localEncryptionKey) {
    SimpleEncryption simpleEncryption = new SimpleEncryption(localEncryptionKey);
    return simpleEncryption.encrypt(plainTextKey);
  }

  private byte[] simpleDecryptDek(byte[] encryptedPlainTextKey, String localEncryptionKey) {
    SimpleEncryption simpleEncryption = new SimpleEncryption(localEncryptionKey);
    return simpleEncryption.decrypt(encryptedPlainTextKey);
  }

  @VisibleForTesting
  KeyManagementServiceClient getClient(GcpKmsConfig gcpKmsConfig) {
    Preconditions.checkNotNull(gcpKmsConfig.getCredentials(), "credentials are not provided");
    Preconditions.checkNotNull(gcpKmsConfig.getRegion(), "region is not present");
    Preconditions.checkNotNull(gcpKmsConfig.getProjectId(), "projectId is not present");
    Preconditions.checkNotNull(gcpKmsConfig.getKeyRing(), "keyRing is not present");
    Preconditions.checkNotNull(gcpKmsConfig.getKeyName(), "keyName is not provided");
    InputStream credentialsStream =
        new ByteArrayInputStream(String.copyValueOf(gcpKmsConfig.getCredentials()).getBytes(StandardCharsets.UTF_8));
    try {
      GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
      FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
      KeyManagementServiceSettings keyManagementServiceSettings =
          KeyManagementServiceSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
      return KeyManagementServiceClient.create(keyManagementServiceSettings);
    } catch (IOException e) {
      throw new InvalidArgumentsException("gcpKmsConfig is invalid", USER_SRE, e);
    }
  }
}
