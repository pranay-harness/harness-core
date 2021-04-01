package software.wings.helpers.ext.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder()
@JsonTypeName("cloudFormationCompletionFlag")
@OwnedBy(CDP)
public class CloudFormationCompletionFlag implements SweepingOutput {
  private boolean createStackCompleted;

  @Override
  public String getType() {
    return "cloudFormationCompletionFlag";
  }
}
