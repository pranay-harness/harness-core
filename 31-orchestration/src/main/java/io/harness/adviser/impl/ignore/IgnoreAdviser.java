package io.harness.adviser.impl.ignore;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserType;
import io.harness.adviser.AdvisingEvent;
import io.harness.state.execution.status.NodeExecutionStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IgnoreAdviser implements Adviser {
  IgnoreAdviserParameters parameters;
  AdviserType type = AdviserType.builder().type(AdviserType.IGNORE).build();

  @Override
  public Advise onAdviseEvent(AdvisingEvent advisingEvent) {
    return null;
  }

  @Override
  public boolean canAdvise(NodeExecutionStatus status) {
    return false;
  }
}
