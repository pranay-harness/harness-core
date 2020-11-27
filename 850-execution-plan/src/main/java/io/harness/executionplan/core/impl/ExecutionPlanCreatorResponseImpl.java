package io.harness.executionplan.core.impl;

import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.plan.PlanNode;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ExecutionPlanCreatorResponseImpl implements ExecutionPlanCreatorResponse {
  @Singular List<PlanNode> planNodes;
  @NotNull String startingNodeId;
}
