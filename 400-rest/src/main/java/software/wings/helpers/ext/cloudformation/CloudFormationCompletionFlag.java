package software.wings.helpers.ext.cloudformation;

import io.harness.pms.sdk.core.data.SweepingOutput;

import lombok.Builder;
import lombok.Data;

@Data
@Builder()
public class CloudFormationCompletionFlag implements SweepingOutput {
  private boolean createStackCompleted;
}
