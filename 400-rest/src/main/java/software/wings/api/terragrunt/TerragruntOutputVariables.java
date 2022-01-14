package software.wings.api.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;

@OwnedBy(CDP)
@JsonTypeName("terragruntOutputVariables")
@TargetModule(HarnessModule._957_CG_BEANS)
public class TerragruntOutputVariables extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "terragrunt";

  @Override
  public String getType() {
    return "terragruntOutputVariables";
  }
}
