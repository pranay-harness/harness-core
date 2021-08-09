package software.wings.api.arm;

import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;

@JsonTypeName("armOutputVariables")
public class ARMOutputVariables extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "arm";

  @Override
  public String getType() {
    return "armOutputVariables";
  }
}
