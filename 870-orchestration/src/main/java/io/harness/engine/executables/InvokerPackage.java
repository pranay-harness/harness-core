package io.harness.engine.executables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class InvokerPackage {
  @NonNull NodeExecutionProto nodeExecution;
  @Singular List<PlanNodeProto> nodes;
  StepInputPackage inputPackage;
  PassThroughData passThroughData;
}
