package io.harness.adviser.advise;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;

public class InterventionWaitAdvise implements Advise {
  @Override
  public AdviseType getType() {
    return AdviseType.INTERVENTION_WAIT;
  }
}
