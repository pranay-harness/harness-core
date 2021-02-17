package software.wings.testutils.encryptionsamples;

import io.harness.beans.EncryptedData;
import io.harness.persistence.UuidAware;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SampleEncryptableSettingField {
  private String accountId;
  private EncryptedData random;
  private UuidAware entity;
}
