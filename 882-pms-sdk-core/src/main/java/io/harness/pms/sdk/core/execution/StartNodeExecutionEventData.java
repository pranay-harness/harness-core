package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.NodeExecutionEventData;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class StartNodeExecutionEventData implements NodeExecutionEventData {
  // TODO: move to sdk-commons once FacilitatorResponseProto is there
  List<PlanNodeProto> nodes;
  FacilitatorResponse facilitatorResponse;
}
