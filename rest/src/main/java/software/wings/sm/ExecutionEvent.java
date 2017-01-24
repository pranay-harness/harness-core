package software.wings.sm;

/**
 * Created by rishi on 1/24/17.
 */
public class ExecutionEvent {
  private final ExecutionContextImpl context;

  ExecutionEvent(ExecutionContextImpl context) {
    this.context = context;
  }
  public ExecutionStatus getExecutionStatus() {
    if (context == null) {
      return null;
    }

    return context.getStateExecutionInstance().getStatus();
  }

  public ExecutionContext getContext() {
    return context;
  }
}
