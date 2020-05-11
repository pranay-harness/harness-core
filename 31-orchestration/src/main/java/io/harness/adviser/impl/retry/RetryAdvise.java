package io.harness.adviser.impl.retry;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class RetryAdvise implements Advise {
  String retryNodeExecutionId;
  Integer waitInterval;

  @Override
  public AdviseType getType() {
    return AdviseType.RETRY;
  }
}
