/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
