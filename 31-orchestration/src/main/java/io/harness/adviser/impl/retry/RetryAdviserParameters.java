package io.harness.adviser.impl.retry;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.Redesign;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Set;

@Value
@Builder
@Redesign
public class RetryAdviserParameters implements AdviserParameters {
  List<Integer> waitInterval;
  int retryCount;

  // TODO(prashant) => This should have been RepairActionCode enum value. Change this when we move that to the
  // orchestration layer
  String repairActionCodeAfterRetry;

  Set<FailureType> applicableFailureTypes;

  // Only applicable if the repair action code after retry is set to IGNORE
  String nextNodeId;
}
