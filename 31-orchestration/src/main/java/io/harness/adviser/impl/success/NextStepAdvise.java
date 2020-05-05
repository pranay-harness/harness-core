package io.harness.adviser.impl.success;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Value;

@Value
@Redesign
@Builder
public class NextStepAdvise implements Advise {
  String nextNodeId;

  public NextStepAdvise(String nextNodeId) {
    this.nextNodeId = nextNodeId;
  }

  @Override
  public AdviseType getType() {
    return AdviseType.NEXT_STEP;
  }
}
