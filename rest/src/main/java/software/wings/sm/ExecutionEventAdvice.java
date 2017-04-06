package software.wings.sm;

/**
 * Created by rishi on 1/26/17.
 */
public class ExecutionEventAdvice {
  private ExecutionInterruptType executionInterruptType;
  private String nextStateName;
  private String nextChildStateMachineId;

  public ExecutionInterruptType getExecutionInterruptType() {
    return executionInterruptType;
  }

  public void setExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
    this.executionInterruptType = executionInterruptType;
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
    private ExecutionInterruptType executionInterruptType;
    private String nextStateName;
    private String nextChildStateMachineId;

    private ExecutionEventAdviceBuilder() {}

    public static ExecutionEventAdviceBuilder anExecutionEventAdvice() {
      return new ExecutionEventAdviceBuilder();
    }

    public ExecutionEventAdviceBuilder withExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
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
      executionEventAdvice.setExecutionInterruptType(executionInterruptType);
      executionEventAdvice.setNextStateName(nextStateName);
      executionEventAdvice.setNextChildStateMachineId(nextChildStateMachineId);
      return executionEventAdvice;
    }
  }
}
