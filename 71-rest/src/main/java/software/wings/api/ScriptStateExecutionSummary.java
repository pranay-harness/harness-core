package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ScriptStateExecutionSummary extends StepExecutionSummary {
  private String activityId;
  private Map<String, String> sweepingOutputEnvVariables;
}
