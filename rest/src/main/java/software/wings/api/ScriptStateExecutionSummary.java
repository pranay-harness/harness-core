package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptStateExecutionSummary extends StepExecutionSummary {
  @Builder
  public ScriptStateExecutionSummary() {}
}
