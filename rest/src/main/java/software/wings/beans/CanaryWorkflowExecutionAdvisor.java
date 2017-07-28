package software.wings.beans;

import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.common.Constants;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElementType;
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

    if (state.getParentId() != null) {
      PhaseStep phaseStep = null;
      if (state.getParentId().equals(orchestrationWorkflow.getPreDeploymentSteps().getUuid())) {
        phaseStep = orchestrationWorkflow.getPreDeploymentSteps();
      } else if (state.getParentId().equals(orchestrationWorkflow.getPostDeploymentSteps().getUuid())) {
        phaseStep = orchestrationWorkflow.getPostDeploymentSteps();
      } else {
        PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
        WorkflowPhase phase = orchestrationWorkflow.getWorkflowPhaseIdMap().get(phaseElement.getUuid());
        if (phase != null) {
          Optional<PhaseStep> phaseStep1 = phase.getPhaseSteps()
                                               .stream()
                                               .filter(ps -> ps != null && state.getParentId().equals(ps.getUuid()))
                                               .findFirst();
          if (phaseStep1.isPresent()) {
            phaseStep = phaseStep1.get();
          }
        }
      }
      if (phaseStep != null && phaseStep.getFailureStrategies() != null
          && !phaseStep.getFailureStrategies().isEmpty()) {
        RepairActionCode repairActionCode = rollbackStrategy(phaseStep.getFailureStrategies(), state);
        return getExecutionEventAdvice(orchestrationWorkflow, repairActionCode, executionEvent, null);
      }
    }
    RepairActionCode repairActionCode = rollbackStrategy(orchestrationWorkflow.getFailureStrategies(), state);

    return getExecutionEventAdvice(orchestrationWorkflow, repairActionCode, executionEvent, phaseSubWorkflow);
  }

  private ExecutionEventAdvice getExecutionEventAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      RepairActionCode repairActionCode, ExecutionEvent executionEvent, PhaseSubWorkflow phaseSubWorkflow) {
    switch (repairActionCode) {
      case IGNORE: {
        return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.IGNORE).build();
      }

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

  private RepairActionCode rollbackStrategy(List<FailureStrategy> failureStrategies, State state) {
    if (failureStrategies == null) {
      return null;
    }
    Optional<FailureStrategy> rollbackStrategy =
        failureStrategies.stream()
            .filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_WORKFLOW)
            .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    rollbackStrategy =
        failureStrategies.stream().filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_PHASE).findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    rollbackStrategy = failureStrategies.stream()
                           .filter(f -> f.getRepairActionCode() == RepairActionCode.MANUAL_INTERVENTION)
                           .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    rollbackStrategy =
        failureStrategies.stream().filter(f -> f.getRepairActionCode() == RepairActionCode.IGNORE).findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get().getRepairActionCode();
    }

    return null;
  }
}
