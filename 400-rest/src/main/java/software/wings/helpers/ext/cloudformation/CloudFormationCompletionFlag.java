package software.wings.helpers.ext.cloudformation;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder()
@JsonTypeName("cloudFormationCompletionFlag")
public class CloudFormationCompletionFlag implements SweepingOutput {
  private boolean createStackCompleted;

  @Override
  public String getType() {
    return "cloudFormationCompletionFlag";
  }
}
