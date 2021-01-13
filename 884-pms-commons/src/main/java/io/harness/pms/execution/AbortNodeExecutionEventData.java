package io.harness.pms.execution;

import io.harness.pms.contracts.execution.Status;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AbortNodeExecutionEventData implements NodeExecutionEventData {
  Status finalStatus;
}
