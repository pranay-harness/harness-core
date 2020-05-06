package io.harness.adviser.impl.retry;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import lombok.Builder;
import lombok.Value;

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
