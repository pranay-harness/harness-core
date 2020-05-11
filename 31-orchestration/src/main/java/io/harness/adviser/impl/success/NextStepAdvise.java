package io.harness.adviser.impl.success;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.Advise;
import io.harness.adviser.AdviseType;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
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
