package io.harness.secrets.validation;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.HarnessSecret;
import io.harness.beans.SecretManagerConfig;

import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface SecretValidator {
  void validateSecret(
      @NotEmpty String accountId, @NotNull HarnessSecret secret, @NotNull SecretManagerConfig secretManagerConfig);
  void validateSecretUpdate(@NotNull HarnessSecret secret, @NotNull EncryptedData existingRecord,
      @NotNull SecretManagerConfig secretManagerConfig);
}
