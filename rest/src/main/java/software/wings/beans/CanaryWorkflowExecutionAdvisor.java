package software.wings.beans;

import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionEventAdvice;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by rishi on 1/24/17.
 */
public class CanaryWorkflowExecutionAdvisor implements ExecutionEventAdvisor {
  private static final Logger logger = LoggerFactory.getLogger(CanaryWorkflowExecutionAdvisor.class);

  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;

  @Inject @Transient private transient WorkflowService workflowService;

  @Inject @Transient private transient WorkflowNotificationHelper workflowNotificationHelper;

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    ExecutionContext context = executionEvent.getContext();
    State state = executionEvent.getState();
    PhaseSubWorkflow phaseSubWorkflow = null;
    if (state.getStateType().equals(StateType.PHASE.name()) && state instanceof PhaseSubWorkflow) {
      phaseSubWorkflow = (PhaseSubWorkflow) state;
      workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
          context, executionEvent.getExecutionStatus(), phaseSubWorkflow);

      if (!phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() != ExecutionStatus.FAILED) {
        return null;
      }

      if (phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() != ExecutionStatus.FAILED
          && executionEvent.getExecutionStatus() != ExecutionStatus.SUCCESS) {
        return null;
      }
    } else if (!(executionEvent.getExecutionStatus() == ExecutionStatus.FAILED
                   || executionEvent.getExecutionStatus() == ExecutionStatus.ERROR)) {
      return null;
    }

    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    if (workflowExecution.getWorkflowType() != WorkflowType.ORCHESTRATION) {
      return null;
    }

    Workflow workflow = workflowService.readWorkflow(context.getAppId(), workflowExecution.getWorkflowId());
    if (workflow == null || workflow.getOrchestrationWorkflow() == null
        || !(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return null;
    }

    CanaryOrchestrationWorkflow orchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow == null || orchestrationWorkflow.getRollbackWorkflowPhaseIdMap() == null) {
      return null;
    }

    RepairActionCode repairActionCode = rollbackStrategy(orchestrationWorkflow);

    switch (repairActionCode) {
      case IGNORE:
        return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.MARK_SUCCESS).build();

      case MANUAL_INTERVENTION:
        return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.PAUSE).build();

      case ROLLBACK_PHASE: {
        if (phaseSubWorkflow == null) {
          return null;
        }
        if (phaseSubWorkflow.isRollback()) {
          return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.ROLLBACK_DONE).build();
        }

        return anExecutionEventAdvice()
            .withNextStateName(
                orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseSubWorkflow.getId()).getName())
            .withExecutionInterruptType(ExecutionInterruptType.ROLLBACK)
            .build();
      }

      case ROLLBACK_WORKFLOW: {
        if (phaseSubWorkflow == null) {
          return null;
        }

        if (!phaseSubWorkflow.isRollback()) {
          return anExecutionEventAdvice()
              .withNextStateName(
                  orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseSubWorkflow.getId()).getName())
              .withExecutionInterruptType(ExecutionInterruptType.ROLLBACK)
              .build();
        }

        // Rollback phase
        if (executionEvent.getExecutionStatus() != ExecutionStatus.SUCCESS) {
          return null;
        }

        List<String> phaseNames =
            orchestrationWorkflow.getWorkflowPhases().stream().map(WorkflowPhase::getName).collect(Collectors.toList());
        int index = phaseNames.indexOf(phaseSubWorkflow.getPhaseNameForRollback());
        if (index == 0) {
          // All Done
          return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.ROLLBACK_DONE).build();
        }

        String phaseId = orchestrationWorkflow.getWorkflowPhases().get(index - 1).getUuid();
        WorkflowPhase rollbackPhase = orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseId);
        if (rollbackPhase == null) {
          return null;
        }
        return anExecutionEventAdvice()
            .withExecutionInterruptType(ExecutionInterruptType.ROLLBACK)
            .withNextStateName(rollbackPhase.getName())
            .build();
      }
      default:
        return null;
    }
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

    rollbackStrategy = orchestrationWorkflow.getFailureStrategies()
                           .stream()
                           .filter(f -> f.getRepairActionCode() == RepairActionCode.MANUAL_INTERVENTION)
                           .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    rollbackStrategy = orchestrationWorkflow.getFailureStrategies()
                           .stream()
                           .filter(f -> f.getRepairActionCode() == RepairActionCode.IGNORE)
                           .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    return null;
  }
}
