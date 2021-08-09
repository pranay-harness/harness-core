package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(PL)
@Getter
@Setter
@Builder
public class NGAccessWithEncryptionConsumer {
  private NGAccess ngAccess;
  private DecryptableEntity decryptableEntity;
}
