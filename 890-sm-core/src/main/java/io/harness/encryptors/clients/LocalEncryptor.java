package io.harness.encryptors.clients;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryptors.KmsEncryptor;
import io.harness.security.SimpleEncryption;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import software.wings.beans.LocalEncryptionConfig;

import com.google.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
@OwnedBy(PL)
public class LocalEncryptor implements KmsEncryptor {
  @Override
  public EncryptedRecord encryptSecret(String accountId, String value, EncryptionConfig encryptionConfig) {
    char[] encryptedChars = new SimpleEncryption(accountId).encryptChars(value.toCharArray());
    return EncryptedRecordData.builder().encryptionKey(accountId).encryptedValue(encryptedChars).build();
  }

  @Override
  public char[] fetchSecretValue(String accountId, EncryptedRecord encryptedRecord, EncryptionConfig encryptionConfig) {
    if (isEmpty(encryptedRecord.getEncryptionKey())) {
      return null;
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
}
