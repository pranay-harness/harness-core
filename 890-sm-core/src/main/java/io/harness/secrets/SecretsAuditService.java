package io.harness.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretUpdateData;

import javax.validation.constraints.NotNull;

@OwnedBy(PL)
public interface SecretsAuditService {
  void logSecretCreateEvent(@NotNull EncryptedData newRecord);
  void logSecretUpdateEvent(@NotNull EncryptedData oldRecord, @NotNull EncryptedData updatedRecord,
      @NotNull SecretUpdateData secretUpdateData);
  void logSecretDeleteEvent(@NotNull EncryptedData deletedRecord);
}
