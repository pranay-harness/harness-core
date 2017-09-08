package software.wings.sm;

import java.util.Map;

/**
 * Created by rishi on 1/26/17.
 */
public class ExecutionEventAdvice {
  private ExecutionInterruptType executionInterruptType;
  private String nextStateName;
  private String nextChildStateMachineId;
  private Integer waitInterval;
  private Map<String, Object> stateParams;

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

  public Integer getWaitInterval() {
    return waitInterval;
  }

  public void setWaitInterval(Integer waitInterval) {
    this.waitInterval = waitInterval;
  }

  public Map<String, Object> getStateParams() {
    return stateParams;
  }

  public void setStateParams(Map<String, Object> stateParams) {
    this.stateParams = stateParams;
  }

  public static final class ExecutionEventAdviceBuilder {
    private ExecutionInterruptType executionInterruptType;
    private String nextStateName;
    private String nextChildStateMachineId;
    private Integer waitInterval;
    private Map<String, Object> stateParams;

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

    public ExecutionEventAdviceBuilder withWaitInterval(Integer waitInterval) {
      this.waitInterval = waitInterval;
      return this;
    }

    public ExecutionEventAdviceBuilder withStateParams(Map<String, Object> stateParams) {
      this.stateParams = stateParams;
      return this;
    }

    public ExecutionEventAdvice build() {
      ExecutionEventAdvice executionEventAdvice = new ExecutionEventAdvice();
      executionEventAdvice.setExecutionInterruptType(executionInterruptType);
      executionEventAdvice.setNextStateName(nextStateName);
      executionEventAdvice.setNextChildStateMachineId(nextChildStateMachineId);
      executionEventAdvice.setWaitInterval(waitInterval);
      executionEventAdvice.setStateParams(stateParams);
      return executionEventAdvice;
    }
  }
}
