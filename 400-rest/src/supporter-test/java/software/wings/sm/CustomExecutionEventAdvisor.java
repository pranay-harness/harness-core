/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm;

import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;

import io.harness.beans.ExecutionInterruptType;
import io.harness.beans.ExecutionStatus;

public class CustomExecutionEventAdvisor implements ExecutionEventAdvisor {
  private ExecutionInterruptType executionInterruptType;

  public CustomExecutionEventAdvisor() {}

  public CustomExecutionEventAdvisor(ExecutionInterruptType executionInterruptType) {
    this.executionInterruptType = executionInterruptType;
  }

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    if (executionEvent.getExecutionStatus() == ExecutionStatus.FAILED) {
      return anExecutionEventAdvice().withExecutionInterruptType(executionInterruptType).build();
    }
    return null;
  }

  public ExecutionInterruptType getExecutionInterruptType() {
    return executionInterruptType;
  }

  public void setExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
    this.executionInterruptType = executionInterruptType;
  }
}
