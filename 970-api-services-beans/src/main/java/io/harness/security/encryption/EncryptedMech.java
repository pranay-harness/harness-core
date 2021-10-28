package io.harness.security.encryption;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public enum EncryptedMech {
  LOCAL_CRYPTO,
  MULTI_CRYPTO,
  AWS_ENCRYPTION_SDK_CRYPTO;
}
