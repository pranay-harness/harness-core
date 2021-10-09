package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;
import io.harness.security.encryption.EncryptedRecordData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("terraformPlanParam")
@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(CDP)
public class TerraformPlanParam implements SweepingOutput {
  private EncryptedRecordData encryptedRecordData;
  private String tfplan;

  @Override
  public String getType() {
    return "terraformPlanParam";
  }
}
