package software.wings.beans;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger logger = LoggerFactory.getLogger(CanaryWorkflowExecutionAdvisor.class);

  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;

  @Inject @Transient private transient WorkflowService workflowService;

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    ExecutionContext context = executionEvent.getContext();
    State state = executionEvent.getState();
    ContextElement contextElement = context.getContextElement();
    if (!state.getStateType().equals(StateType.PHASE.name()) || !(state instanceof PhaseSubWorkflow)) {
      return null;
    }
    PhaseSubWorkflow phaseSubWorkflow = (PhaseSubWorkflow) state;
    if (!phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() != ExecutionStatus.FAILED) {
      return null;
    }

    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    if (workflowExecution.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      Workflow workflow = workflowService.readWorkflow(context.getAppId(), workflowExecution.getWorkflowId());
      if (workflow == null || workflow.getOrchestrationWorkflow() == null
          || !(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
        return null;
      }

      CanaryOrchestrationWorkflow orchestrationWorkflow =
          (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
      if (orchestrationWorkflow == null || orchestrationWorkflow.getRollbackWorkflowPhaseIdMap() == null
          || orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseSubWorkflow.getId()) == null) {
        return null;
      }

      if (phaseSubWorkflow.isRollback()) {
        // TODO:: rollback previously successful phases
        return null;
      }

      RepairActionCode repairActionCode = rollbackStrategy(orchestrationWorkflow);
      if (repairActionCode != repairActionCode.ROLLBACK_PHASE
          & repairActionCode != repairActionCode.ROLLBACK_WORKFLOW) {
        return null;
      }

      return ExecutionEventAdviceBuilder.anExecutionEventAdvice()
          .withNextStateName(
              orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseSubWorkflow.getId()).getName())
          .build();
    }
    return null;
  }

  private RepairActionCode rollbackStrategy(CanaryOrchestrationWorkflow orchestrationWorkflow) {
    if (orchestrationWorkflow.getFailureStrategies() == null) {
      return null;
    }
    Optional<FailureStrategy> rollbackStrategy =
        orchestrationWorkflow.getFailureStrategies()
            .stream()
            .filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_WORKFLOW)
            .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    rollbackStrategy = orchestrationWorkflow.getFailureStrategies()
                           .stream()
                           .filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_PHASE)
                           .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    return null;
  }
}
