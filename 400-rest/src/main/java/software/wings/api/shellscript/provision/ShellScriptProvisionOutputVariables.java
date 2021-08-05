package software.wings.api.shellscript.provision;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;

@JsonTypeName("shellScriptProvisionOutputVariables")
@OwnedBy(CDP)
public class ShellScriptProvisionOutputVariables extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "shellScriptProvisioner";

  @Override
  public String getType() {
    return "shellScriptProvisionOutputVariables";
  }
}