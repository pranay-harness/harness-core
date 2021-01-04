package software.wings.sm;

import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;

import io.harness.beans.ExecutionStatus;
import io.harness.interrupts.ExecutionInterruptType;

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
