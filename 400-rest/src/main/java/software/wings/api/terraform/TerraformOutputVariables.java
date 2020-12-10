package software.wings.api.terraform;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;

@JsonTypeName("terraformOutputVariables")
public class TerraformOutputVariables extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "terraform";

  @Override
  public String getType() {
    return "terraformOutputVariables";
  }
}
