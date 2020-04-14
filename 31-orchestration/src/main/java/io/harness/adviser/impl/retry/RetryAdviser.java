package io.harness.adviser.impl.retry;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.annotations.Redesign;
import io.harness.state.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Redesign
public class RetryAdviser implements Adviser {
  RetryAdviserParameters parameters;
  AdviserType type = AdviserType.builder().type(AdviserType.RETRY).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    return null;
  }

  @Override
  public boolean canAdvise(NodeExecutionStatus status) {
    return false;
  }
}
