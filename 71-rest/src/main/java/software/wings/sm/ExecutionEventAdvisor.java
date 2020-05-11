package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

/**
 * Created by rishi on 1/23/17.
 */
@OwnedBy(CDC)
public interface ExecutionEventAdvisor {
  ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent);
}
