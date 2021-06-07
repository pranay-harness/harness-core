package io.harness.pms.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class StartNodeExecutionEventData implements NodeExecutionEventData {
  FacilitatorResponseProto facilitatorResponse;
}
