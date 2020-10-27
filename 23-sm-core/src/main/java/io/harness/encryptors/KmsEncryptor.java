package io.harness.encryptors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface KmsEncryptor {
  EncryptedRecord encryptSecret(
      @NotEmpty String accountId, @NotEmpty String value, @Valid @NotNull EncryptionConfig encryptionConfig);

  char[] fetchSecretValue(@NotEmpty String accountId, @Valid @NotNull EncryptedRecord encryptedRecord,
      @Valid @NotNull EncryptionConfig encryptionConfig);
}
