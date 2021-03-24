package io.harness.encryptors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface KmsEncryptor {
  EncryptedRecord encryptSecret(
      @NotEmpty String accountId, @NotEmpty String value, @NotNull EncryptionConfig encryptionConfig);

  char[] fetchSecretValue(
      @NotEmpty String accountId, @NotNull EncryptedRecord encryptedRecord, @NotNull EncryptionConfig encryptionConfig);

  default boolean validateKmsConfiguration(String accountId, EncryptionConfig encryptionConfig) {
    throw new UnsupportedOperationException(
        "Validating SecretManager Configuration on Delegate in not available yet for:" + encryptionConfig);
  }
}
