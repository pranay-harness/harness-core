package software.wings.api;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.Label;
import software.wings.sm.StepExecutionSummary;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesSteadyStateCheckExecutionSummary extends StepExecutionSummary {
  private List<Label> labels;

  @Builder
  public KubernetesSteadyStateCheckExecutionSummary(List<Label> labels) {
    this.labels = labels;
  }
}
