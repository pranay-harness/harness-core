package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PL)
public final class SecretKeyConstants {
  public static final String AES_SECRET_KEY = "AES_SECRET_KEY";
  public static final String AES_ENCRYPTION_ALGORITHM = "AES";
}
