package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class InstanceFetchStateExecutionSummary extends StepExecutionSummary {
  private String activityId;
}
