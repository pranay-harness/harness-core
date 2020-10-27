package io.harness.encryptors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface VaultEncryptor {
  EncryptedRecord createSecret(@NotEmpty String accountId, @NotEmpty String name, @NotEmpty String plaintext,
      @Valid @NotNull EncryptionConfig encryptionConfig);

  EncryptedRecord updateSecret(@NotEmpty String accountId, @NotEmpty String name, @NotEmpty String plaintext,
      @NotNull EncryptedRecord existingRecord, @Valid @NotNull EncryptionConfig encryptionConfig);

  EncryptedRecord renameSecret(@NotEmpty String accountId, @NotEmpty String name,
      @NotNull EncryptedRecord existingRecord, @Valid @NotNull EncryptionConfig encryptionConfig);

  boolean deleteSecret(@NotEmpty String accountId, @Valid @NotNull EncryptedRecord existingRecord,
      @Valid @NotNull EncryptionConfig encryptionConfig);

  boolean validateReference(
      @NotEmpty String accountId, @NotEmpty String path, @Valid @NotNull EncryptionConfig encryptionConfig);

  char[] fetchSecretValue(@NotEmpty String accountId, @Valid @NotNull EncryptedRecord encryptedRecord,
      @Valid @NotNull EncryptionConfig encryptionConfig);
}
