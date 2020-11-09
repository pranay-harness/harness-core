package io.harness.encryptors;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptedRecord;
import io.harness.security.encryption.EncryptionConfig;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface CustomEncryptor {
  boolean validateReference(
      @NotEmpty String accountId, @NotNull Set<EncryptedDataParams> params, @NotNull EncryptionConfig encryptionConfig);

  char[] fetchSecretValue(
      @NotEmpty String accountId, @NotNull EncryptedRecord encryptedRecord, @NotNull EncryptionConfig encryptionConfig);
}
