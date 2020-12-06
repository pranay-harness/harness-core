package software.wings.api.terraform;

import io.harness.pms.sdk.core.data.SweepingOutput;

import java.util.HashMap;

public class TerraformOutputVariables extends HashMap<String, Object> implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "terraform";
}
