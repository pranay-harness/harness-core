package io.harness.secrets.validation;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.HarnessSecret;
import io.harness.beans.SecretManagerConfig;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface SecretValidator {
  void validateSecret(@NotEmpty String accountId, @Valid @NotNull HarnessSecret secret,
      @Valid @NotNull SecretManagerConfig secretManagerConfig);
  void validateSecretUpdate(@Valid @NotNull HarnessSecret secret, @Valid @NotNull EncryptedData existingRecord,
      @Valid @NotNull SecretManagerConfig secretManagerConfig);
}
