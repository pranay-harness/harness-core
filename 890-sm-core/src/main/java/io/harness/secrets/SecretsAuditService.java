/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
