package software.wings.helpers.ext.cloudformation;

import io.harness.data.SweepingOutput;

import lombok.Builder;
import lombok.Data;

@Data
@Builder()
public class CloudFormationCompletionFlag implements SweepingOutput {
  private boolean createStackCompleted;
}
