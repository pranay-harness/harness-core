package io.harness.pms.plan.execution.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionMetadata;

import lombok.Builder;
import lombok.Data;

@OwnedBy(PIPELINE)
@Data
@Builder
public class ExecArgs {
  ExecutionMetadata metadata;
  PlanExecutionMetadata planExecutionMetadata;
}
