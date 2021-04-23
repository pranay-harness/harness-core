package io.harness.engine.facilitation;

import io.harness.engine.interrupts.PreFacilitationCheck;
import io.harness.execution.NodeExecution;

public abstract class AbstractPreFacilitationChecker {
  protected AbstractPreFacilitationChecker nextChecker;

  public void setNextChecker(AbstractPreFacilitationChecker nextChecker) {
    this.nextChecker = nextChecker;
  }

  public PreFacilitationCheck check(NodeExecution nodeExecution) {
    PreFacilitationCheck preCheck = this.performCheck(nodeExecution);
    if (!preCheck.isProceed()) {
      return preCheck;
    }
    if (nextChecker != null) {
      return nextChecker.check(nodeExecution);
    }
    return PreFacilitationCheck.builder().proceed(true).reason(null).build();
  }

  protected abstract PreFacilitationCheck performCheck(NodeExecution nodeExecution);
}
