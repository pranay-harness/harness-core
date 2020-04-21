package software.wings.testutils.encryptionsamples;

import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedData;

@Data
@Builder
public class SampleEncryptableSettingField {
  private String accountId;
  private EncryptedData random;
  private UuidAware entity;
}
