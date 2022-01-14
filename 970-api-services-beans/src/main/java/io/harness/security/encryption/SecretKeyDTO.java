package io.harness.security.encryption;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.crypto.spec.SecretKeySpec;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
public class SecretKeyDTO {
  private String uuid;
  private byte[] key;
  private String algorithm;

  public SecretKeySpec getSecretKeySpec() {
    return new SecretKeySpec(key, algorithm);
  }
}
