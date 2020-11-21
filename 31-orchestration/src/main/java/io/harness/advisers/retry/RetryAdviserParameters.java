package io.harness.advisers.retry;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;
import io.harness.interrupts.RepairActionCode;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class RetryAdviserParameters {
  List<Integer> waitIntervalList;
  int retryCount;
  RepairActionCode repairActionCodeAfterRetry;
  Set<FailureType> applicableFailureTypes;
  // Only applicable if the repair action code after retry is set to IGNORE
  String nextNodeId;
}
