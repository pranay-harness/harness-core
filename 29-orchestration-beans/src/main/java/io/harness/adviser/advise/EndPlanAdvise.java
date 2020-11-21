package io.harness.adviser.advise;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;

@OwnedBy(CDC)
@Builder
public class EndPlanAdvise implements Advise {
  @Override
  public AdviseType getType() {
    return AdviseType.END_PLAN;
  }
}
