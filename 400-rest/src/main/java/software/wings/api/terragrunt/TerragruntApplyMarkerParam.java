package software.wings.api.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
@JsonTypeName("terragruntApplyMarkerParam")
public class TerragruntApplyMarkerParam implements SweepingOutput {
  private String provisionerId;
  private boolean applyCompleted;
  private String pathToModule;

  @Override
  public String getType() {
    return "terragruntApplyMarkerParam";
  }
}
