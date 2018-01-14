package software.wings.sm;

/**
 * Created by rishi on 1/24/17.
 */
public class ExecutionEvent {
  private final ExecutionContextImpl context;
  private final State state;

  ExecutionEvent(ExecutionContextImpl context, State state) {
    this.context = context;
    this.state = state;
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

  public State getState() {
    return state;
  }

  @Override
  public String toString() {
    return "ExecutionEvent{"
        + "context=" + context + ", state=" + state + '}';
  }

  public static final class ExecutionEventBuilder {
    private ExecutionContextImpl context;
    private State state;

    private ExecutionEventBuilder() {}

    public static ExecutionEventBuilder anExecutionEvent() {
      return new ExecutionEventBuilder();
    }

    public ExecutionEventBuilder withContext(ExecutionContextImpl context) {
      this.context = context;
      return this;
    }

    public ExecutionEventBuilder withState(State state) {
      this.state = state;
      return this;
    }

    public ExecutionEvent build() {
      return new ExecutionEvent(context, state);
    }
  }
}
