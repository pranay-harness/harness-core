package io.harness.pms.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.PlanNodeProto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class StartNodeExecutionEventData implements NodeExecutionEventData {
  List<PlanNodeProto> nodes;
  FacilitatorResponseProto facilitatorResponse;
}
