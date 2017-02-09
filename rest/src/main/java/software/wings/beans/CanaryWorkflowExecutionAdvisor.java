package software.wings.beans;

import org.mongodb.morphia.annotations.Transient;
import software.wings.common.Constants;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionEventAdvice;
import software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.Optional;
import javax.inject.Inject;

/**
 * Created by rishi on 1/24/17.
 */
public class CanaryWorkflowExecutionAdvisor implements ExecutionEventAdvisor {
  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;

  @Inject @Transient private transient WorkflowService workflowService;

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    if (executionEvent.getExecutionStatus() != ExecutionStatus.FAILED) {
      return null;
    }
    ExecutionContext context = executionEvent.getContext();
    State state = executionEvent.getState();
    ContextElement contextElement = context.getContextElement();
    if (!state.getStateType().equals(StateType.PHASE.name()) || !(state instanceof PhaseSubWorkflow)) {
      return null;
    }

    PhaseSubWorkflow phaseSubWorkflow = (PhaseSubWorkflow) state;
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    if (workflowExecution.getWorkflowType() == WorkflowType.ORCHESTRATION_WORKFLOW) {
      OrchestrationWorkflow orchestrationWorkflow =
          workflowService.readOrchestrationWorkflow(context.getAppId(), workflowExecution.getWorkflowId());
      if (orchestrationWorkflow == null || orchestrationWorkflow.getRollbackWorkflowPhaseIdMap() == null
          || orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseSubWorkflow.getId()) == null) {
        return null;
      }

      if (isRollbackStrategy(orchestrationWorkflow)) {
        return ExecutionEventAdviceBuilder.anExecutionEventAdvice()
            .withNextStateName(Constants.ROLLBACK_PREFIX + phaseSubWorkflow.getName())
            .build();
      }
    }
    return null;
  }

  private boolean isRollbackStrategy(OrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow.getFailureStrategies() == null) {
      return false;
    }
    Optional<FailureStrategy> rollbackStrategy =
        orchestrationWorkflow.getFailureStrategies()
            .stream()
            .filter(f
                -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_PHASE
                    || f.getRepairActionCode() == RepairActionCode.ROLLBACK_PHASE)
            .findFirst();

    if (rollbackStrategy.isPresent()) {
      return true;
    }

    return false;
  }
}
