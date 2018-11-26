package software.wings.service.impl.security;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.DelegateRetryableException;
import io.harness.exception.KmsOperationException;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.helpers.ext.vault.VaultRestClientFactory;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
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
 * Created by rsingh on 10/2/17.
 */
public class SecretManagementDelegateServiceImpl implements SecretManagementDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(SecretManagementDelegateServiceImpl.class);

  public static final int NUM_OF_RETRIES = 3;

  @Inject private TimeLimiter timeLimiter;

  private boolean isRetryable(Exception e) {
    // TimeLimiter.callWithTimer will throw a new exception wrapping around the AwsKMS exceptions. Unwrap it.
    Throwable t = e.getCause() == null ? e : e.getCause();

    if (t instanceof AWSKMSException) {
      if (t instanceof KMSInternalException || t instanceof DependencyTimeoutException
          || t instanceof KeyUnavailableException) {
        return true;
      }
      return false;
    } else {
      // Else if not IllegalArgumentException, it should retry.
      return !(t instanceof IllegalArgumentException);
    }
  }

  @Override
  public EncryptedData encrypt(String accountId, char[] value, KmsConfig kmsConfig) {
    Preconditions.checkNotNull(kmsConfig, "null for " + accountId);
    for (int retry = 1; retry <= NUM_OF_RETRIES; retry++) {
      try {
        return timeLimiter.callWithTimeout(
            () -> encryptInternal(accountId, value, kmsConfig), 15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        if (isRetryable(e)) {
          if (retry < NUM_OF_RETRIES) {
            logger.warn(format("Encryption failed. trial num: %d", retry), e);
            sleep(ofMillis(100));
          } else {
            String reason = format("Encryption failed after %d retries", NUM_OF_RETRIES);
            throw new DelegateRetryableException(new KmsOperationException(reason, e, USER));
          }
        } else {
          throw new KmsOperationException(e.getMessage(), e, USER);
        }
      }
    }

    throw new IllegalStateException("Encryption failed. This state should never have been reached");
  }

  @SuppressFBWarnings("DM_STRING_CTOR")
  @Override
  public char[] decrypt(EncryptedData data, KmsConfig kmsConfig) {
    if (data.getEncryptedValue() == null) {
      return null;
    }

    Preconditions.checkNotNull(kmsConfig, "null for " + data);

    for (int retry = 1; retry <= NUM_OF_RETRIES; retry++) {
      try {
        return timeLimiter.callWithTimeout(() -> decryptInternal(data, kmsConfig), 15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        if (isRetryable(e)) {
          if (retry < NUM_OF_RETRIES) {
            logger.warn(format("Decryption failed. trial num: %d", retry), e);
            sleep(ofMillis(100));
          } else {
            String reason = format("Decryption failed after %d retries", NUM_OF_RETRIES);
            throw new DelegateRetryableException(new KmsOperationException(reason, e, USER));
          }
        } else {
          throw new KmsOperationException(e.getMessage(), e, USER);
        }
      }
    }
    throw new IllegalStateException("Decryption failed. This state should never have been reached");
  }

  @Override
  public EncryptedData encrypt(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData) {
    for (int retry = 1; retry <= NUM_OF_RETRIES; retry++) {
      try {
        return timeLimiter.callWithTimeout(
            ()
                -> encryptInternal(name, value, accountId, settingType, vaultConfig, savedEncryptedData),
            15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        if (retry < NUM_OF_RETRIES) {
          logger.warn(format("encryption failed. trial num: %d", retry), e);
          sleep(ofMillis(1000));
        } else {
          logger.error(format("encryption failed after %d retries ", retry), e);
          throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
              .addParam("reason", "encryption failed after " + NUM_OF_RETRIES + " retries");
        }
      }
    }
    throw new IllegalStateException("Encryption failed. This state should never have been reached");
  }

  @Override
  public char[] decrypt(EncryptedData data, VaultConfig vaultConfig) {
    if (data.getEncryptedValue() == null) {
      return null;
    }
    for (int retry = 1; retry <= NUM_OF_RETRIES; retry++) {
      try {
        return timeLimiter.callWithTimeout(() -> {
          logger.info("reading secret {} from vault {}", data.getEncryptionKey(), vaultConfig.getVaultUrl());
          String value = VaultRestClientFactory.create(vaultConfig)
                             .readSecret(String.valueOf(vaultConfig.getAuthToken()), data.getEncryptionKey());

          if (EmptyPredicate.isNotEmpty(value)) {
            return value.toCharArray();
          } else {
            String errorMsg = "Request not successful.";
            logger.error(errorMsg);
            throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER).addParam("reason", errorMsg);
          }
        }, 15, TimeUnit.SECONDS, true);
      } catch (Exception e) {
        if (retry < NUM_OF_RETRIES) {
          logger.warn(format("decryption failed. trial num: %d", retry), e);
          sleep(ofMillis(1000));
        } else {
          logger.error("decryption failed after {} retries for {}", retry, data, e);
          throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
              .addParam("reason", "Decryption failed after " + NUM_OF_RETRIES + " retries");
        }
      }
    }
    throw new IllegalStateException("Decryption failed. This state should never have been reached");
  }

  @Override
  public boolean deleteVaultSecret(String path, VaultConfig vaultConfig) {
    boolean success = false;
    try {
      success =
          VaultRestClientFactory.create(vaultConfig).deleteSecret(String.valueOf(vaultConfig.getAuthToken()), path);
    } catch (IOException e) {
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
          .addParam("reason", "Deletion of secret failed");
    }
    return success;
  }

  @Override
  public boolean renewVaultToken(VaultConfig vaultConfig) {
    for (int retry = 1; retry <= NUM_OF_RETRIES; retry++) {
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
        if (retry < NUM_OF_RETRIES) {
          logger.warn(format("renewal failed. trial num: %d", retry), e);
          sleep(ofMillis(1000));
        } else {
          logger.error("renewal failed after {} retries for {}", retry, vaultConfig, e);
          throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER, e)
              .addParam("reason", "renewal failed after " + NUM_OF_RETRIES + " retries");
        }
      }
    }
    return false;
  }

  private EncryptedData encryptInternal(String accountId, char[] value, KmsConfig kmsConfig) throws Exception {
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

  private char[] decryptInternal(EncryptedData data, KmsConfig kmsConfig) throws Exception {
    final AWSKMS kmsClient =
        AWSKMSClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(kmsConfig.getAccessKey(), kmsConfig.getSecretKey())))
            .withRegion(kmsConfig.getRegion() == null ? Regions.US_EAST_1 : Regions.fromName(kmsConfig.getRegion()))
            .build();

    DecryptRequest decryptRequest =
        new DecryptRequest().withCiphertextBlob(StandardCharsets.ISO_8859_1.encode(data.getEncryptionKey()));
    ByteBuffer plainTextKey = kmsClient.decrypt(decryptRequest).getPlaintext();

    // Shutdown the KMS client so as to prevent resource leaking,
    kmsClient.shutdown();

    return decrypt(data.getEncryptedValue(), new SecretKeySpec(getByteArray(plainTextKey), "AES")).toCharArray();
  }

  private EncryptedData encryptInternal(String name, String value, String accountId, SettingVariableTypes settingType,
      VaultConfig vaultConfig, EncryptedData savedEncryptedData) throws Exception {
    String keyUrl = settingType + "/" + name;
    if (value == null) {
      keyUrl = savedEncryptedData == null ? keyUrl : savedEncryptedData.getEncryptionKey();
      char[] encryptedValue = savedEncryptedData == null ? null : keyUrl.toCharArray();
      return EncryptedData.builder()
          .encryptionKey(keyUrl)
          .encryptedValue(encryptedValue)
          .encryptionType(EncryptionType.VAULT)
          .enabled(true)
          .accountId(accountId)
          .parentIds(new HashSet<>())
          .kmsId(vaultConfig.getUuid())
          .build();
    }
    if (savedEncryptedData != null && isNotBlank(value)) {
      logger.info("deleting vault secret {} for {}", savedEncryptedData.getEncryptionKey(), savedEncryptedData);
      VaultRestClientFactory.create(vaultConfig)
          .deleteSecret(String.valueOf(vaultConfig.getAuthToken()), savedEncryptedData.getEncryptionKey());
    }
    boolean isSuccessful = VaultRestClientFactory.create(vaultConfig)
                               .writeSecret(String.valueOf(vaultConfig.getAuthToken()), name, settingType, value);

    if (isSuccessful) {
      logger.info("saving vault secret {} for {}", keyUrl, savedEncryptedData);
      return EncryptedData.builder()
          .encryptionKey(keyUrl)
          .encryptedValue(keyUrl.toCharArray())
          .encryptionType(EncryptionType.VAULT)
          .enabled(true)
          .accountId(accountId)
          .parentIds(new HashSet<>())
          .kmsId(vaultConfig.getUuid())
          .build();
    } else {
      String errorMsg = "Request not successful.";
      logger.error(errorMsg);
      throw new WingsException(ErrorCode.VAULT_OPERATION_ERROR, USER).addParam("reason", errorMsg);
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

  private byte[] getByteArray(ByteBuffer b) {
    byte[] byteArray = new byte[b.remaining()];
    b.get(byteArray);
    return byteArray;
  }
}
