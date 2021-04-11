package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("awsAmiInfoVariables")
@OwnedBy(CDP)
public class AwsAmiInfoVariables implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "ami";
  private String newAsgName;
  private String oldAsgName;

  @Override
  public String getType() {
    return "awsAmiInfoVariables";
  }
}
