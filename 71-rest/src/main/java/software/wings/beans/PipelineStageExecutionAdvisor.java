package software.wings.beans;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.interrupts.ExecutionInterruptType.PAUSE_FOR_INPUTS;
import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;
import static software.wings.sm.StateType.ENV_LOOP_STATE;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.inject.Inject;

import io.harness.context.ContextElementType;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.EnvStateExecutionData;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionEventAdvice;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.WorkflowState;

import java.util.List;

@Slf4j
public class PipelineStageExecutionAdvisor implements ExecutionEventAdvisor {
  @Inject private transient WorkflowExecutionService workflowExecutionService;
  @Inject private transient WorkflowService workflowService;

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    State state = executionEvent.getState();
    ExecutionContextImpl context = executionEvent.getContext();
    WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();

    if (!ENV_STATE.name().equals(state.getStateType()) && !ENV_LOOP_STATE.name().equals(state.getStateType())) {
      return null;
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (workflowStandardParams != null && workflowStandardParams.isContinueWithDefaultValues()) {
      log.info(String.format(
          "Continue With defaults option is selection for execution: %s. Hence not pausing the stage for inputs",
          workflowExecution.getUuid()));
      return null;
    }

    WorkflowState workflowState = (WorkflowState) state;
    if (stateExecutionInstance.isContinued()) {
      return null;
    }
    ExecutionResponse executionResponse = workflowState.checkDisableAssertion(context, workflowService, log);
    if (executionResponse != null) {
      return anExecutionEventAdvice().withExecutionResponse(executionResponse).build();
    }

    List<String> runtimeInputsVariables = workflowState.getRuntimeInputVariables();
    if (isNotEmpty(runtimeInputsVariables)) {
      EnvStateExecutionData envStateExecutionData =
          anEnvStateExecutionData().withWorkflowId(workflowState.getWorkflowId()).build();
      executionResponse =
          ExecutionResponse.builder().executionStatus(PAUSED).stateExecutionData(envStateExecutionData).build();
      return anExecutionEventAdvice()
          .withExecutionInterruptType(PAUSE_FOR_INPUTS)
          .withExecutionResponse(executionResponse)
          .withTimeout(workflowState.getTimeout())
          .withActionOnTimeout(workflowState.getTimeoutAction())
          .withUserGroupIdsToNotify(workflowState.getUserGroupIds())
          .withNextChildStateMachineId(stateExecutionInstance.getChildStateMachineId())
          .build();
    }

    return null;
  }
}
