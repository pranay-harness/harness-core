package io.harness.adviser.advise;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import lombok.Builder;

@Builder
public class EndPlanAdvise implements Advise {
  @Override
  public AdviseType getType() {
    return AdviseType.END_PLAN;
  }
}
