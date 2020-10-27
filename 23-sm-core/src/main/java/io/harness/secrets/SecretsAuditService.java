package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretUpdateData;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface SecretsAuditService {
  void logSecretCreateEvent(@Valid @NotNull EncryptedData newRecord);
  void logSecretUpdateEvent(@Valid @NotNull EncryptedData oldRecord, @Valid @NotNull EncryptedData updatedRecord,
      @Valid @NotNull SecretUpdateData secretUpdateData);
  void logSecretDeleteEvent(@Valid @NotNull EncryptedData deletedRecord);
}