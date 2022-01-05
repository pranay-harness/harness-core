package io.harness.beans.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.security.encryption.EncryptedRecordData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("terraformPlanParam")
@OwnedBy(CDP)
public class TerraformPlanParam implements SweepingOutput {
  private EncryptedRecordData encryptedRecordData;
  private String tfplan;
  private String tfPlanJsonFileId;

  @Override
  public String getType() {
    return "terraformPlanParam";
  }
}
