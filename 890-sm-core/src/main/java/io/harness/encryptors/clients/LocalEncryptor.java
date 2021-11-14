package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.SecretKey;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.KmsEncryptor;
import io.harness.secretkey.SecretKeyConstants;
import io.harness.secretkey.SecretKeyService;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedMech;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.utils.featureflaghelper.FeatureFlagHelperService;

import software.wings.beans.LocalEncryptionConfig;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class LocalEncryptor implements KmsEncryptor {
  private static final AwsCrypto crypto = AwsCrypto.standard();
  private final SecretKeyService secretKeyService;
  private final FeatureFlagHelperService featureFlagService;

  @Inject
  public LocalEncryptor(@Named(SecretKeyConstants.AES_SECRET_KEY) SecretKeyService secretKeyService,
      FeatureFlagHelperService featureFlagService) {
    this.secretKeyService = secretKeyService;
    this.featureFlagService = featureFlagService;
  }

  @Override
  public EncryptedRecord encryptSecret(String accountId, String value, EncryptionConfig encryptionConfig) {
    if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)) {
      SecretKey secretKey = secretKeyService.createSecretKey();
      final byte[] awsEncryptedSecret = getAwsEncryptedSecret(accountId, value, secretKey);
      return EncryptedRecordData.builder()
          .encryptionKey(secretKey.getUuid())
          .encryptedValueBytes(awsEncryptedSecret)
          .encryptedMech(EncryptedMech.AWS_ENCRYPTION_SDK_CRYPTO)
          .build();
    }

    final char[] localJavaEncryptedSecret = getLocalJavaEncryptedSecret(accountId, value);
    if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_MULTI_CRYPTO_MODE)) {
      SecretKey secretKey = secretKeyService.createSecretKey();
      final byte[] awsEncryptedSecret = getAwsEncryptedSecret(accountId, value, secretKey);
      return EncryptedRecordData.builder()
          .encryptionKey(accountId)
          .encryptedValue(localJavaEncryptedSecret)
          .encryptedMech(EncryptedMech.MULTI_CRYPTO)
          .additionalMetadata(AdditionalMetadata.builder()
                                  .value(AdditionalMetadata.SECRET_KEY_KEY, secretKey.getUuid())
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

    String secretKeyUuid = null;
    byte[] encryptedSecret = null;
    if (featureFlagService.isEnabled(accountId, FeatureName.LOCAL_AWS_ENCRYPTION_SDK_MODE)) {
      secretKeyUuid = encryptedRecord.getEncryptionKey();
      encryptedSecret = encryptedRecord.getEncryptedValueBytes();
    } else {
      secretKeyUuid =
          (String) encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.SECRET_KEY_KEY);
      encryptedSecret =
          (byte[]) encryptedRecord.getAdditionalMetadata().getValues().get(AdditionalMetadata.AWS_ENCRYPTED_SECRET);
    }

    Optional<SecretKey> secretKey = secretKeyService.getSecretKey(secretKeyUuid);
    if (!secretKey.isPresent()) {
      // throw proper exception
    }
    return getAwsDecryptedSecret(accountId, encryptedSecret, secretKey.get()).toCharArray();
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

  private byte[] getAwsEncryptedSecret(String accountId, String value, SecretKey secretKey) {
    JceMasterKey escrowPub =
        JceMasterKey.getInstance(secretKey.getSecretKeySpec(), "Escrow", "Escrow", "AES/GCM/NOPADDING");
    Map<String, String> context = Collections.singletonMap("accountId", accountId);

    return crypto.encryptData(escrowPub, value.getBytes(StandardCharsets.UTF_8), context).getResult();
  }

  private String getAwsDecryptedSecret(String accountId, byte[] encryptedSecret, SecretKey secretKey) {
    JceMasterKey escrowPub =
        JceMasterKey.getInstance(secretKey.getSecretKeySpec(), "Escrow", "Escrow", "AES/GCM/NOPADDING");

    final CryptoResult<byte[], ?> decryptResult = crypto.decryptData(escrowPub, encryptedSecret);
    if (!decryptResult.getMasterKeyIds().get(0).equals(accountId)) {
      // throw exception
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
