package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.KmsEncryptor;
import io.harness.exception.UnexpectedException;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedMech;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.SecretKeyDTO;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import software.wings.beans.LocalEncryptionConfig;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class LocalEncryptor implements KmsEncryptor {
  private static final AwsCrypto crypto = AwsCrypto.standard();
  @Inject private FeatureFlagHelperService featureFlagService;

  @Override
  public EncryptedRecord encryptSecret(String accountId, String value, EncryptionConfig encryptionConfig) {
    if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)) {
      final byte[] awsEncryptedSecret = getAwsEncryptedSecret(accountId, value, encryptionConfig.getSecretKeySpec());
      return EncryptedRecordData.builder()
          .encryptionKey(encryptionConfig.getSecretKeySpec().getUuid())
          .encryptedValueBytes(awsEncryptedSecret)
          .encryptedMech(EncryptedMech.AWS_ENCRYPTION_SDK_CRYPTO)
          .build();
    }
    final char[] localJavaEncryptedSecret = getLocalJavaEncryptedSecret(accountId, value);
    if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_MULTI_CRYPTO_MODE)) {
      final byte[] awsEncryptedSecret = getAwsEncryptedSecret(accountId, value, encryptionConfig.getSecretKeySpec());
      return EncryptedRecordData.builder()
          .encryptionKey(accountId)
          .encryptedValue(localJavaEncryptedSecret)
          .encryptedMech(EncryptedMech.MULTI_CRYPTO)
          .additionalMetadata(
              AdditionalMetadata.builder()
                  .value(AdditionalMetadata.SECRET_KEY_UUID_KEY, encryptionConfig.getSecretKeySpec().getUuid())
                  .value(AdditionalMetadata.AWS_ENCRYPTED_SECRET, awsEncryptedSecret)
                  .build())
          .build();
    }
    return EncryptedRecordData.builder().encryptionKey(accountId).encryptedValue(localJavaEncryptedSecret).build();
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    if (isEmpty(encryptedRecord.getEncryptionKey())) {
      return null;
    }

    // This means this record hasn't been migrated yet
    if (encryptedRecord.getEncryptedMech() == null) {
      return getLocalJavaDecryptedSecret(encryptedRecord);
    }

    byte[] encryptedSecret = null;
    if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)) {
      encryptedSecret = encryptedRecord.getEncryptedValueBytes();
    } else if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_MULTI_CRYPTO_MODE)) {
      encryptedSecret = encryptedRecord.getAdditionalMetadata().getAwsEncryptedSecret();
    } else {
      return getLocalJavaDecryptedSecret(encryptedRecord);
    }

    return getAwsDecryptedSecret(accountId, encryptedSecret, encryptionConfig.getSecretKeySpec()).toCharArray();
  }

  @Override
  public boolean validateKmsConfiguration(String accountId, EncryptionConfig encryptionConfig) {
    log.info("Validating Local KMS configuration Start {}", encryptionConfig.getName());
    String randomString = UUIDGenerator.generateUuid();
    LocalEncryptionConfig localEncryptionConfig = (LocalEncryptionConfig) encryptionConfig;
    try {
      encryptSecret(localEncryptionConfig.getAccountId(), randomString, localEncryptionConfig);
    } catch (Exception e) {
      log.error("Was not able to encrypt using given credentials. Please check your credentials and try again", e);
      return false;
    }
    log.info("Validating Local KMS configuration End {}", encryptionConfig.getName());
    return true;
  }

  // ------------------------------ PRIVATE METHODS -----------------------------

  private byte[] getAwsEncryptedSecret(String accountId, String value, SecretKeyDTO secretKey) {
    JceMasterKey escrowPub =
        JceMasterKey.getInstance(secretKey.getSecretKeySpec(), "Escrow", "Escrow", "AES/GCM/NOPADDING");
    Map<String, String> context = Collections.singletonMap("accountId", accountId);

    return crypto.encryptData(escrowPub, value.getBytes(StandardCharsets.UTF_8), context).getResult();
  }

  private String getAwsDecryptedSecret(String accountId, byte[] encryptedSecret, SecretKeyDTO secretKey) {
    JceMasterKey escrowPub =
        JceMasterKey.getInstance(secretKey.getSecretKeySpec(), "Escrow", "Escrow", "AES/GCM/NOPADDING");

    final CryptoResult<byte[], ?> decryptResult = crypto.decryptData(escrowPub, encryptedSecret);
    if (!decryptResult.getEncryptionContext().get("accountId").equals(accountId)) {
      throw new UnexpectedException(String.format("Corrupted secret found for secret key : %s", secretKey.getUuid()));
    }

    return new String(decryptResult.getResult(), StandardCharsets.UTF_8);
  }

  private char[] getLocalJavaEncryptedSecret(String accountId, String value) {
    return new SimpleEncryption(accountId).encryptChars(value.toCharArray());
  }

  private char[] getLocalJavaDecryptedSecret(EncryptedRecord encryptedRecord) {
    final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptedRecord.getEncryptionKey());
    return simpleEncryption.decryptChars(encryptedRecord.getEncryptedValue());
  }
}
