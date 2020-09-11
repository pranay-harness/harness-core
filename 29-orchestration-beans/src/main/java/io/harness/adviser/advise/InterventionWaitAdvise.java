package io.harness.adviser.advise;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class InterventionWaitAdvise implements Advise {
  @Builder.Default Duration timeOut = Duration.ofDays(1);

  @Override
  public AdviseType getType() {
    return AdviseType.INTERVENTION_WAIT;
  }
}
