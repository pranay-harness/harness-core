package software.wings.security.encryption;

import io.harness.security.encryption.EncryptionType;
import lombok.Builder;
import lombok.Data;

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
