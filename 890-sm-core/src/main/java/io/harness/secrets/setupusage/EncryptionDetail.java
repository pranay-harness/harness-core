package io.harness.secrets.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptionType;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class EncryptionDetail implements Comparable<EncryptionDetail> {
  private EncryptionType encryptionType;
  private String secretManagerName;

  @Override
  public int compareTo(EncryptionDetail o) {
    return encryptionType.compareTo(o.encryptionType);
  }
}
