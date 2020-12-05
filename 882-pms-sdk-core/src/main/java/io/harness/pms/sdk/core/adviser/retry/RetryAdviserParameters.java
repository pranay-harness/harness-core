package io.harness.pms.sdk.core.adviser.retry;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.commons.RepairActionCode;
import io.harness.pms.execution.failure.FailureType;
import io.harness.pms.sdk.core.adviser.WithFailureTypes;

import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("retryAdviserParameters")
public class RetryAdviserParameters implements WithFailureTypes {
  List<Integer> waitIntervalList;
  int retryCount;
  RepairActionCode repairActionCodeAfterRetry;
  Set<FailureType> applicableFailureTypes;
  // Only applicable if the repair action code after retry is set to IGNORE
  String nextNodeId;
}
