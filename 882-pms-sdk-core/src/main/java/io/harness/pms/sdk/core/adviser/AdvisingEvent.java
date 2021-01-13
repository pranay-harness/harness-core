package io.harness.pms.sdk.core.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class AdvisingEvent {
  @NonNull NodeExecutionProto nodeExecution;
  byte[] adviserParameters;
  Status toStatus;
  Status fromStatus;
}
