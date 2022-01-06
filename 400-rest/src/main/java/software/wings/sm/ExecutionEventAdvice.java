/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionInterruptType;
import io.harness.beans.RepairActionCode;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by rishi on 1/26/17.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Getter
@Setter
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
  private RepairActionCode actionOnTimeout;
  private Long timeout;
  private List<String> userGroupIdsToNotify;
  private ExecutionResponse executionResponse;
  private ExecutionInterruptType actionAfterManualInterventionTimeout;

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
    private Long timeout;
    private RepairActionCode actionOnTimeout;
    private List<String> userGroupIdsToNotify;
    private ExecutionResponse executionResponse;
    private ExecutionInterruptType actionAfterManualInterventionTimeout;

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

    public ExecutionEventAdviceBuilder withActionOnTimeout(RepairActionCode actionOnTimeout) {
      this.actionOnTimeout = actionOnTimeout;
      return this;
    }

    public ExecutionEventAdviceBuilder withTimeout(Long timeout) {
      this.timeout = timeout;
      return this;
    }

    public ExecutionEventAdviceBuilder withUserGroupIdsToNotify(List<String> userGroupIdsToNotify) {
      this.userGroupIdsToNotify = userGroupIdsToNotify;
      return this;
    }

    public ExecutionEventAdviceBuilder withExecutionResponse(ExecutionResponse executionResponse) {
      this.executionResponse = executionResponse;
      return this;
    }

    public ExecutionEventAdviceBuilder withActionAfterManualInterventionTimeout(
        ExecutionInterruptType actionAfterManualInterventionTimeout) {
      this.actionAfterManualInterventionTimeout = actionAfterManualInterventionTimeout;
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
      executionEventAdvice.setActionOnTimeout(actionOnTimeout);
      executionEventAdvice.setTimeout(timeout);
      executionEventAdvice.setExecutionResponse(executionResponse);
      executionEventAdvice.setUserGroupIdsToNotify(userGroupIdsToNotify);
      executionEventAdvice.setActionAfterManualInterventionTimeout(actionAfterManualInterventionTimeout);
      return executionEventAdvice;
    }
  }
}
