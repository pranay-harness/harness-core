package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;

@JsonTypeName("shellScriptProvisionerOutput")
@OwnedBy(CDP)
public class ShellScriptProvisionerOutput extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = ShellScriptProvisionerOutputElement.KEY;

  @Override
  public String getType() {
    return "shellScriptProvisionerOutput";
  }
}
