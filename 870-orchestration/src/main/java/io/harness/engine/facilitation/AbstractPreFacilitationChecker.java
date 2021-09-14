/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionCheck;
import io.harness.execution.NodeExecution;

@OwnedBy(HarnessTeam.PIPELINE)
public abstract class AbstractPreFacilitationChecker {
  protected AbstractPreFacilitationChecker nextChecker;

  public void setNextChecker(AbstractPreFacilitationChecker nextChecker) {
    this.nextChecker = nextChecker;
  }

  public ExecutionCheck check(NodeExecution nodeExecution) {
    ExecutionCheck preCheck = this.performCheck(nodeExecution);
    if (!preCheck.isProceed()) {
      return preCheck;
    }
    if (nextChecker != null) {
      return nextChecker.check(nodeExecution);
    }
    return ExecutionCheck.builder().proceed(true).reason(null).build();
  }

  protected abstract ExecutionCheck performCheck(NodeExecution nodeExecution);
}
