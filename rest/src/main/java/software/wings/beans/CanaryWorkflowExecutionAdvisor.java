package software.wings.beans;

import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterruptType;

/**
 * Created by rishi on 1/24/17.
 */
public class CanaryWorkflowExecutionAdvisor implements ExecutionEventAdvisor {
  @Override
  public ExecutionInterruptType onExecutionEvent(ExecutionEvent executionEvent) {
    return null;
  }
}
