package io.harness.adviser.impl.retry;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;
import io.harness.interrupts.RepairActionCode;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class RetryAdviserParameters implements AdviserParameters {
  List<Integer> waitInterval;
  int retryCount;
  RepairActionCode repairActionCodeAfterRetry;
  Set<FailureType> applicableFailureTypes;
  // Only applicable if the repair action code after retry is set to IGNORE
  String nextNodeId;
}
