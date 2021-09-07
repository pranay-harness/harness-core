package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.container.Label;
import software.wings.sm.StepExecutionSummary;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class KubernetesSteadyStateCheckExecutionSummary extends StepExecutionSummary {
  private List<Label> labels;
  private String namespace;

  @Builder
  public KubernetesSteadyStateCheckExecutionSummary(List<Label> labels, String namespace) {
    this.labels = labels;
    this.namespace = namespace;
  }
}
