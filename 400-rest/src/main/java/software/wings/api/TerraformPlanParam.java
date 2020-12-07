package software.wings.api;

import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.security.encryption.EncryptedRecordData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TerraformPlanParam implements SweepingOutput {
  private EncryptedRecordData encryptedRecordData;
  private String tfplan;
}
