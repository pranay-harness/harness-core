package software.wings.sm;

/**
 * Created by rishi on 1/26/17.
 */
public class ExecutionEventAdvice {
  private ExecutionInterrupt executionInterrupt;
  private boolean roollback;
  private String nextStateName;
  private String nextChildStateMachineId;

  public ExecutionInterrupt getExecutionInterrupt() {
    return executionInterrupt;
  }

  public void setExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    this.executionInterrupt = executionInterrupt;
  }

  public boolean isRoollback() {
    return roollback;
  }

  public void setRoollback(boolean roollback) {
    this.roollback = roollback;
  }

  public String getNextStateName() {
    return nextStateName;
  }

  public void setNextStateName(String nextStateName) {
    this.nextStateName = nextStateName;
  }

  public String getNextChildStateMachineId() {
    return nextChildStateMachineId;
  }

  public void setNextChildStateMachineId(String nextChildStateMachineId) {
    this.nextChildStateMachineId = nextChildStateMachineId;
  }

  public static final class ExecutionEventAdviceBuilder {
    private ExecutionInterrupt executionInterrupt;
    private boolean roollback;
    private String nextStateName;
    private String nextChildStateMachineId;

    private ExecutionEventAdviceBuilder() {}

    public static ExecutionEventAdviceBuilder anExecutionEventAdvice() {
      return new ExecutionEventAdviceBuilder();
    }

    public ExecutionEventAdviceBuilder withExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
      this.executionInterrupt = executionInterrupt;
      return this;
    }

    public ExecutionEventAdviceBuilder withRoollback(boolean roollback) {
      this.roollback = roollback;
      return this;
    }

    public ExecutionEventAdviceBuilder withNextStateName(String nextStateName) {
      this.nextStateName = nextStateName;
      return this;
    }

    public ExecutionEventAdviceBuilder withNextChildStateMachineId(String nextChildStateMachineId) {
      this.nextChildStateMachineId = nextChildStateMachineId;
      return this;
    }

    public ExecutionEventAdvice build() {
      ExecutionEventAdvice executionEventAdvice = new ExecutionEventAdvice();
      executionEventAdvice.setExecutionInterrupt(executionInterrupt);
      executionEventAdvice.setRoollback(roollback);
      executionEventAdvice.setNextStateName(nextStateName);
      executionEventAdvice.setNextChildStateMachineId(nextChildStateMachineId);
      return executionEventAdvice;
    }
  }
}
