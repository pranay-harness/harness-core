package io.harness.secretkey;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PL)
public class AESSecretKeyServiceImpl extends AbstractSecretKeyServiceImpl {
  @Override
  public String getAlgorithm() {
    return SecretKeyConstants.AES_ENCRYPTION_ALGORITHM;
  }
}
