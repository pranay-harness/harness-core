package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.sm.StepExecutionSummary;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesSteadyStateCheckExecutionSummary extends StepExecutionSummary {
  private Map<String, String> labels;

  @Builder
  public KubernetesSteadyStateCheckExecutionSummary(Map<String, String> labels) {
    this.labels = labels;
  }
}
