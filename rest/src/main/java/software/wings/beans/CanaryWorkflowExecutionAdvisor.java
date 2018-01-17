package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.ROLLBACK;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateType.FORK;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.REPEAT;
import static software.wings.sm.StateType.SUB_WORKFLOW;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.Graph.Node;
import software.wings.common.Constants;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionEventAdvice;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by rishi on 1/24/17.
 */
public class CanaryWorkflowExecutionAdvisor implements ExecutionEventAdvisor {
  private static final Logger logger = LoggerFactory.getLogger(CanaryWorkflowExecutionAdvisor.class);

  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;

  @Inject @Transient private transient WorkflowService workflowService;

  @Inject @Transient private transient WorkflowNotificationHelper workflowNotificationHelper;

  @Inject @Transient private transient ExecutionInterruptManager executionInterruptManager;

  @Inject @Transient private transient InstanceHelper instanceHelper;

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    ExecutionContext context = executionEvent.getContext();
    List<ExecutionInterrupt> executionInterrupts =
        executionInterruptManager.checkForExecutionInterrupt(context.getAppId(), context.getWorkflowExecutionId());
    if (executionInterrupts != null
        && executionInterrupts.stream().anyMatch(ex -> ex.getExecutionInterruptType() == ABORT_ALL)) {
      return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.END_EXECUTION).build();
    }

    State state = executionEvent.getState();
    PhaseSubWorkflow phaseSubWorkflow = null;
    WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    if (state.getStateType().equals(StateType.PHASE.name()) && state instanceof PhaseSubWorkflow) {
      phaseSubWorkflow = (PhaseSubWorkflow) state;

      workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
          context, executionEvent.getExecutionStatus(), phaseSubWorkflow, workflowExecution);

      if (executionEvent.getExecutionStatus().isFinalStatus()) {
        WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
        instanceHelper.extractInstanceOrContainerInfoBaseOnType(context.getStateExecutionInstanceId(),
            context.getStateExecutionData(), workflowStandardParams, context.getAppId(), workflowExecution);
      }

      // nothing to do for regular phase with non-error
      if (!phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() != FAILED
          && executionEvent.getExecutionStatus() != ERROR && executionEvent.getExecutionStatus() != ABORTED) {
        return null;
      }

      // nothing to do for rollback phase that got some error
      if (phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() != SUCCESS) {
        return null;
      }
    } else if (!(executionEvent.getExecutionStatus() == FAILED || executionEvent.getExecutionStatus() == ERROR)) {
      return null;
    }

    if (phaseSubWorkflow == null && executionInterrupts != null
        && executionInterrupts.stream().anyMatch(ex -> ex.getExecutionInterruptType() == ROLLBACK)) {
      return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.END_EXECUTION).build();
    }

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

    if (phaseSubWorkflow != null && executionInterrupts != null
        && executionInterrupts.stream().anyMatch(ex -> ex.getExecutionInterruptType() == ROLLBACK)) {
      return phaseSubWorkflowAdvice(orchestrationWorkflow, phaseSubWorkflow);
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
        FailureStrategy failureStrategy = rollbackStrategy(phaseStep.getFailureStrategies(), state);
        return getExecutionEventAdvice(orchestrationWorkflow, failureStrategy, executionEvent, null, state);
      }
    }
    FailureStrategy failureStrategy = rollbackStrategy(orchestrationWorkflow.getFailureStrategies(), state);

    return getExecutionEventAdvice(orchestrationWorkflow, failureStrategy, executionEvent, phaseSubWorkflow, state);
  }

  private ExecutionEventAdvice getExecutionEventAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      FailureStrategy failureStrategy, ExecutionEvent executionEvent, PhaseSubWorkflow phaseSubWorkflow, State state) {
    if (failureStrategy == null) {
      return null;
    }

    RepairActionCode repairActionCode = failureStrategy.getRepairActionCode();
    switch (repairActionCode) {
      case IGNORE: {
        return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.IGNORE).build();
      }
      case END_EXECUTION: {
        return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.END_EXECUTION).build();
      }

      case MANUAL_INTERVENTION: {
        if (REPEAT.name().equals(state.getStateType()) || FORK.name().equals(state.getStateType())
            || PHASE.name().equals(state.getStateType()) || PHASE_STEP.name().equals(state.getStateType())
            || SUB_WORKFLOW.name().equals(state.getStateType())) {
          return null;
        }

        Map<String, Object> stateParams = fetchStateParams(orchestrationWorkflow, state);
        return anExecutionEventAdvice()
            .withExecutionInterruptType(ExecutionInterruptType.PAUSE)
            .withStateParams(stateParams)
            .build();
      }

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
            .withExecutionInterruptType(ROLLBACK)
            .build();
      }

      case ROLLBACK_WORKFLOW: {
        if (phaseSubWorkflow == null) {
          return null;
        }

        return phaseSubWorkflowAdvice(orchestrationWorkflow, phaseSubWorkflow);
      }
      case RETRY: {
        String stateType = executionEvent.getState().getStateType();
        if (stateType.equals(StateType.PHASE.name()) || stateType.equals(StateType.PHASE_STEP.name())
            || stateType.equals(StateType.SUB_WORKFLOW.name()) || stateType.equals(StateType.FORK.name())
            || stateType.equals(REPEAT.name())) {
          // Retry is only at the leaf node
          FailureStrategy failureStrategyAfterRetry =
              aFailureStrategy().withRepairActionCode(failureStrategy.getRepairActionCodeAfterRetry()).build();
          return getExecutionEventAdvice(
              orchestrationWorkflow, failureStrategyAfterRetry, executionEvent, phaseSubWorkflow, state);
        }

        List<StateExecutionData> stateExecutionDataHistory = ((ExecutionContextImpl) executionEvent.getContext())
                                                                 .getStateExecutionInstance()
                                                                 .getStateExecutionDataHistory();
        if (stateExecutionDataHistory == null || stateExecutionDataHistory.size() < failureStrategy.getRetryCount()) {
          int waitInterval = 0;
          List<Integer> retryIntervals = failureStrategy.getRetryIntervals();
          if (isNotEmpty(retryIntervals)) {
            if (isEmpty(stateExecutionDataHistory)) {
              waitInterval = retryIntervals.get(0);
            } else if (stateExecutionDataHistory.size() > retryIntervals.size() - 1) {
              waitInterval = retryIntervals.get(retryIntervals.size() - 1);
            } else {
              waitInterval = retryIntervals.get(stateExecutionDataHistory.size());
            }
          }
          return anExecutionEventAdvice()
              .withExecutionInterruptType(ExecutionInterruptType.RETRY)
              .withWaitInterval(waitInterval)
              .build();
        } else {
          FailureStrategy failureStrategyAfterRetry =
              aFailureStrategy().withRepairActionCode(failureStrategy.getRepairActionCodeAfterRetry()).build();
          return getExecutionEventAdvice(
              orchestrationWorkflow, failureStrategyAfterRetry, executionEvent, phaseSubWorkflow, state);
        }
      }
      default:
        return null;
    }
  }

  private Map<String, Object> fetchStateParams(CanaryOrchestrationWorkflow orchestrationWorkflow, State state) {
    if (orchestrationWorkflow == null || orchestrationWorkflow.getGraph() == null || state == null
        || state.getId() == null) {
      return null;
    }
    if (state.getParentId() != null && orchestrationWorkflow.getGraph().getSubworkflows() == null
        || orchestrationWorkflow.getGraph().getSubworkflows().get(state.getParentId()) == null) {
      return null;
    }
    Graph graph = orchestrationWorkflow.getGraph().getSubworkflows().get(state.getParentId());
    Optional<Node> node1 = graph.getNodes().stream().filter(node -> node.getId().equals(state.getId())).findFirst();
    if (!node1.isPresent()) {
      return null;
    }

    return node1.get().getProperties();
  }

  private ExecutionEventAdvice phaseSubWorkflowAdvice(
      CanaryOrchestrationWorkflow orchestrationWorkflow, PhaseSubWorkflow phaseSubWorkflow) {
    if (!phaseSubWorkflow.isRollback()) {
      return anExecutionEventAdvice()
          .withNextStateName(
              orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseSubWorkflow.getId()).getName())
          .withExecutionInterruptType(ROLLBACK)
          .build();
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
        .withExecutionInterruptType(ROLLBACK)
        .withNextStateName(rollbackPhase.getName())
        .build();
  }

  private FailureStrategy rollbackStrategy(List<FailureStrategy> failureStrategies, State state) {
    if (isEmpty(failureStrategies)) {
      return null;
    }

    List<FailureStrategy> filteredFailureStrategies =
        failureStrategies.stream()
            .filter(f -> isNotEmpty(f.getSpecificSteps()) && f.getSpecificSteps().contains(state.getName()))
            .collect(Collectors.toList());

    if (filteredFailureStrategies.isEmpty()) {
      filteredFailureStrategies =
          failureStrategies.stream().filter(f -> isEmpty(f.getSpecificSteps())).collect(Collectors.toList());
    }

    Optional<FailureStrategy> rollbackStrategy =
        filteredFailureStrategies.stream()
            .filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_WORKFLOW)
            .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get();
    }

    rollbackStrategy = filteredFailureStrategies.stream()
                           .filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_PHASE)
                           .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get();
    }

    return filteredFailureStrategies.get(0);
  }
}
