package software.wings.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploymentTriggerExecutionArgs {
  private String triggerUuid;
  private String triggerName;
}
