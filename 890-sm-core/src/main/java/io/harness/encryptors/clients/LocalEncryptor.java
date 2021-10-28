package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.SecretKey;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.KmsEncryptor;
import io.harness.ff.FeatureFlagService;
import io.harness.secretkey.SecretKeyService;
import io.harness.secretkey.SecretKeyType;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.LocalEncryptionConfig;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.jce.JceMasterKey;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class LocalEncryptor implements KmsEncryptor {
  private static final String AWS_LOCAL_ENCRYPTION_ENABLED_WITH_BACKUP = "AWS_LOCAL_ENCRYPTION_ENABLED_WITH_BACKUP";
  private static final AwsCrypto crypto = AwsCrypto.standard();
  private SecretKeyService secretKeyService;
  private FeatureFlagService featureFlagService;

  public LocalEncryptor(
      @Named(SecretKeyType.AES_SECRET_KEY) SecretKeyService secretKeyService, FeatureFlagService featureFlagService) {
    this.secretKeyService = secretKeyService;
    this.featureFlagService = featureFlagService;
  }

  @Override
  public EncryptedRecord encryptSecret(String accountId, String value, EncryptionConfig encryptionConfig) {
    SecretKey secretKey = secretKeyService.createSecretKey();
    final char[] awsEncryptedSecret = getAwsEncryptedSecret(accountId, value, secretKey);
    if (featureFlagService.isEnabled(FeatureName.LOCAL_MULTI_CRYPTO_MODE, accountId)) {
      return EncryptedRecordData.builder().encryptionKey(accountId).encryptedValue(awsEncryptedSecret).build();
    }

    char[] encryptedChars = new SimpleEncryption(accountId).encryptChars(value.toCharArray());
    return EncryptedRecordData.builder()
        .encryptionKey(accountId)
        .encryptedValue(awsEncryptedSecret)
        .backupEncryptedValue(encryptedChars)
        .additionalMetadata(AdditionalMetadata.builder().value(AWS_LOCAL_ENCRYPTION_ENABLED_WITH_BACKUP, true).build())
        .build();
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    if (isEmpty(encryptedRecord.getEncryptionKey())) {
      return null;
    }

    Optional<SecretKey> secretKey = secretKeyService.getSecretKey("");
    if (featureFlagService.isEnabled(FeatureName.LOCAL_MULTI_CRYPTO_MODE, accountId)) {
      return getAwsDecryptedSecret(
          accountId, new String(encryptedRecord.getEncryptedValue()).getBytes(), secretKey.get())
          .toCharArray();
    }

    if (encryptedRecord.getBackupEncryptedValue() != null) {
      // The record has both aws encrypted secret as well as locally encrypted secret, try for both
      try {
        return getAwsDecryptedSecret(
            accountId, new String(encryptedRecord.getEncryptedValue()).getBytes(), secretKey.get())
            .toCharArray();
      } catch (Exception exception) {
        // unable to decrypt via aws encryption sdk
      }
    }

    final SimpleEncryption simpleEncryption = new SimpleEncryption(encryptedRecord.getEncryptionKey());
    return simpleEncryption.decryptChars(encryptedRecord.getEncryptedValue());
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

  private char[] getAwsEncryptedSecret(String accountId, String value, SecretKey secretKey) {
    JceMasterKey escrowPub =
        JceMasterKey.getInstance(secretKey.getSecretKeySpec(), "Escrow", "Escrow", "AES/GCM/NOPADDING");
    Map<String, String> context = Collections.singletonMap("accountId", accountId);

    final byte[] encryptedBytes =
        crypto.encryptData(escrowPub, value.getBytes(StandardCharsets.UTF_8), context).getResult();
    return new String(encryptedBytes).toCharArray();
  }

  private String getAwsDecryptedSecret(String accountId, byte[] encryptedSecret, SecretKey secretKey) {
    JceMasterKey escrowPub =
        JceMasterKey.getInstance(secretKey.getSecretKeySpec(), "Escrow", "Escrow", "AES/GCM/NOPADDING");

    final CryptoResult<byte[], ?> decryptResult = crypto.decryptData(escrowPub, encryptedSecret);
    if (!decryptResult.getMasterKeyIds().get(0).equals(accountId)) {
      // throw exception
    }
    return Arrays.toString(decryptResult.getResult());
  }
}
