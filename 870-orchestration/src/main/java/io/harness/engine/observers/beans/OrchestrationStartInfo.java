package io.harness.engine.observers.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.ambiance.Ambiance;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class OrchestrationStartInfo {
  @NotNull Ambiance ambiance;
  @NotNull PlanExecutionMetadata planExecutionMetadata;

  public String getPlanExecutionId() {
    return ambiance.getPlanExecutionId();
  }
}
