package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.ExecutionInterruptType;

import java.util.Map;

/**
 * Created by rishi on 1/26/17.
 */
@OwnedBy(CDC)
public class ExecutionEventAdvice {
  private ExecutionInterruptType executionInterruptType;
  private String nextStateName;
  private String nextChildStateMachineId;
  private String nextStateDisplayName;
  private Integer waitInterval;
  private Map<String, Object> stateParams;
  private String rollbackPhaseName;
  private boolean skipState;
  private String skipExpression;
  private String skipError;

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

  public String getNextStateDisplayName() {
    return nextStateDisplayName;
  }

  public void setNextStateDisplayName(String nextStateDisplayName) {
    this.nextStateDisplayName = nextStateDisplayName;
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

  public String getRollbackPhaseName() {
    return rollbackPhaseName;
  }

  public void setRollbackPhaseName(String rollbackPhaseName) {
    this.rollbackPhaseName = rollbackPhaseName;
  }

  public boolean isSkipState() {
    return skipState;
  }

  public void setSkipState(boolean skipState) {
    this.skipState = skipState;
  }

  public String getSkipExpression() {
    return skipExpression;
  }

  public void setSkipExpression(String skipExpression) {
    this.skipExpression = skipExpression;
  }

  public String getSkipError() {
    return skipError;
  }

  public void setSkipError(String skipError) {
    this.skipError = skipError;
  }

  public static final class ExecutionEventAdviceBuilder {
    private ExecutionInterruptType executionInterruptType;
    private String nextStateDisplayName;
    private String nextStateName;
    private String nextChildStateMachineId;
    private Integer waitInterval;
    private Map<String, Object> stateParams;
    private String rollbackPhaseName;
    private boolean skipState;
    private String skipExpression;
    private String skipError;

    private ExecutionEventAdviceBuilder() {}

    public static ExecutionEventAdviceBuilder anExecutionEventAdvice() {
      return new ExecutionEventAdviceBuilder();
    }

    public ExecutionEventAdviceBuilder withExecutionInterruptType(ExecutionInterruptType executionInterruptType) {
      this.executionInterruptType = executionInterruptType;
      return this;
    }

    public ExecutionEventAdviceBuilder withNextStateDisplayName(String nextStateDisplayName) {
      this.nextStateDisplayName = nextStateDisplayName;
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

    public ExecutionEventAdviceBuilder withRollbackPhaseName(String rollbackPhaseName) {
      this.rollbackPhaseName = rollbackPhaseName;
      return this;
    }

    public ExecutionEventAdviceBuilder withSkipState(boolean skipState) {
      this.skipState = skipState;
      return this;
    }

    public ExecutionEventAdviceBuilder withSkipExpression(String skipExpression) {
      this.skipExpression = skipExpression;
      return this;
    }

    public ExecutionEventAdviceBuilder withSkipError(String skipError) {
      this.skipError = skipError;
      return this;
    }

    public ExecutionEventAdvice build() {
      ExecutionEventAdvice executionEventAdvice = new ExecutionEventAdvice();
      executionEventAdvice.setExecutionInterruptType(executionInterruptType);
      executionEventAdvice.setNextStateName(nextStateName);
      executionEventAdvice.setNextStateDisplayName(nextStateDisplayName);
      executionEventAdvice.setNextChildStateMachineId(nextChildStateMachineId);
      executionEventAdvice.setWaitInterval(waitInterval);
      executionEventAdvice.setStateParams(stateParams);
      executionEventAdvice.setRollbackPhaseName(rollbackPhaseName);
      executionEventAdvice.setSkipState(skipState);
      executionEventAdvice.setSkipExpression(skipExpression);
      executionEventAdvice.setSkipError(skipError);
      return executionEventAdvice;
    }
  }
}
