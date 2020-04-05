package io.harness.advise;

import io.harness.annotations.Redesign;
import io.harness.state.execution.status.NodeExecutionStatus;

@Redesign
public interface Adviser {
  AdviserType getType();

  Advise onAdviseEvent(AdvisingEvent advisingEvent);

  // TODO(prashant) : This doesn't belong here move it to separate interface. Also do we need this at all ?
  boolean canAdvise(NodeExecutionStatus status);
}
