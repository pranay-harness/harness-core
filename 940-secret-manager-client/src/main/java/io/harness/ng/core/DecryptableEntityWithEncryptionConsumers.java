package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(PL)
@Getter
@Setter
@Builder
public class DecryptableEntityWithEncryptionConsumers {
  private List<EncryptedDataDetail> encryptedDataDetailList;
  private DecryptableEntity decryptableEntity;
}
