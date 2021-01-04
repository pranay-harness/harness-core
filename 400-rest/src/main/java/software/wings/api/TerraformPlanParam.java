package software.wings.api;

import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.security.encryption.EncryptedRecordData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("terraformPlanParam")
public class TerraformPlanParam implements SweepingOutput {
  private EncryptedRecordData encryptedRecordData;
  private String tfplan;

  @Override
  public String getType() {
    return "terraformPlanParam";
  }
}
