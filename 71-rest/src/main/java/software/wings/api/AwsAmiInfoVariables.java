package software.wings.api;

import io.harness.pms.sdk.core.data.SweepingOutput;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsAmiInfoVariables implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "ami";
  private String newAsgName;
  private String oldAsgName;
}
