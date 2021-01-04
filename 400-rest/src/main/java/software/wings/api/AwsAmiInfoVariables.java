package software.wings.api;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("awsAmiInfoVariables")
public class AwsAmiInfoVariables implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "ami";
  private String newAsgName;
  private String oldAsgName;

  @Override
  public String getType() {
    return "awsAmiInfoVariables";
  }
}
