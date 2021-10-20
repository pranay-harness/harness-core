package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.KmsEncryptor;
import io.harness.ff.FeatureFlagService;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.AdditionalMetadata;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.LocalEncryptionConfig;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class LocalEncryptor implements KmsEncryptor {
  private static final String AWS_LOCAL_ENCRYPTION_ENABLED_WITH_BACKUP = "AWS_LOCAL_ENCRYPTION_ENABLED_WITH_BACKUP";
  private static final AwsCrypto crypto = AwsCrypto.standard();
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public EncryptedRecord encryptSecret(String accountId, String value, EncryptionConfig encryptionConfig) {
    final char[] awsEncryptedSecret = getAwsEncryptedSecret(accountId, value);
    if (featureFlagService.isEnabled(FeatureName.AWS_LOCAL_ENCRYPTION_WITHOUT_BACKWARD_COMP, accountId)) {
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

    if (featureFlagService.isEnabled(FeatureName.AWS_LOCAL_ENCRYPTION_WITHOUT_BACKWARD_COMP, accountId)) {
      return getAwsDecryptedSecret(accountId, new String(encryptedRecord.getEncryptedValue()).getBytes()).toCharArray();
    }

    if (encryptedRecord.getBackupEncryptedValue() != null) {
      // The record has both aws encrypted secret as well as locally encrypted secret, try for both
      try {
        return getAwsDecryptedSecret(accountId, new String(encryptedRecord.getEncryptedValue()).getBytes())
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

  private char[] getAwsEncryptedSecret(String accountId, String value) {
    final KmsMasterKeyProvider prov = KmsMasterKeyProvider.builder().buildStrict(accountId);
    final Map<String, String> context = Collections.singletonMap("accountId", accountId);
    final byte[] encryptedBytes = crypto.encryptData(prov, value.getBytes(), context).getResult();
    return new String(encryptedBytes).toCharArray();
  }

  private String getAwsDecryptedSecret(String accountId, byte[] encryptedSecret) {
    final KmsMasterKeyProvider prov = KmsMasterKeyProvider.builder().buildStrict(accountId);
    final CryptoResult<byte[], KmsMasterKey> decryptResult = crypto.decryptData(prov, encryptedSecret);
    if (!decryptResult.getMasterKeyIds().get(0).equals(accountId)) {
      // throw exception
    }
    return Arrays.toString(decryptResult.getResult());
  }
}
