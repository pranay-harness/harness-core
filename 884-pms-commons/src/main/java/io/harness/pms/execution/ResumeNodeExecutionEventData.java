package io.harness.pms.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.PlanNodeProto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class ResumeNodeExecutionEventData implements NodeExecutionEventData {
  List<PlanNodeProto> nodes;
  Map<String, byte[]> response;
  boolean asyncError;
}
