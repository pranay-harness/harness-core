package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.OrchestrationWorkflowType.ROLLING;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.common.Constants.PHASE_NAME_PREFIX;
import static software.wings.common.Constants.ROLLBACK_PREFIX;
import static software.wings.common.Constants.ROLLING_PHASE_PREFIX;
import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.ROLLBACK;
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
import software.wings.common.Constants;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.StateExecutionService;
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
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient StateExecutionService stateExecutionService;

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    State state = executionEvent.getState();
    ExecutionContext context = executionEvent.getContext();
    WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();

    try {
      List<ExecutionInterrupt> executionInterrupts =
          executionInterruptManager.checkForExecutionInterrupt(context.getAppId(), context.getWorkflowExecutionId());
      if (executionInterrupts != null
          && executionInterrupts.stream().anyMatch(ex -> ex.getExecutionInterruptType() == ABORT_ALL)) {
        return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.END_EXECUTION).build();
      }

      Workflow workflow = workflowService.readWorkflow(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
      CanaryOrchestrationWorkflow orchestrationWorkflow =
          (CanaryOrchestrationWorkflow) findOrchestrationWorkflow(workflow, workflowExecution);

      boolean rolling = false;
      if (stateExecutionInstance != null && stateExecutionInstance.getOrchestrationWorkflowType() == ROLLING) {
        rolling = true;
      }

      if (rolling && state.getStateType().equals(StateType.PHASE_STEP.name())
          && state.getName().equals(Constants.PRE_DEPLOYMENT) && executionEvent.getExecutionStatus() == SUCCESS) {
        // ready for rolling deploy

        if (orchestrationWorkflow == null || isEmpty(orchestrationWorkflow.getWorkflowPhases())) {
          return null;
        }
        WorkflowPhase workflowPhase = orchestrationWorkflow.getWorkflowPhases().get(0);
        return anExecutionEventAdvice()
            .withExecutionInterruptType(ExecutionInterruptType.NEXT_STEP)
            .withNextStateName(workflowPhase.getName())
            .withNextChildStateMachineId(stateExecutionInstance.getChildStateMachineId())
            .withNextStateDisplayName(ROLLING_PHASE_PREFIX + 1)
            .build();
      }

      PhaseSubWorkflow phaseSubWorkflow = null;
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

      boolean rollbackProvisioners = false;
      if (state.getStateType().equals(StateType.PHASE.name()) && state instanceof PhaseSubWorkflow) {
        phaseSubWorkflow = (PhaseSubWorkflow) state;

        workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
            context, executionEvent.getExecutionStatus(), phaseSubWorkflow);

        if (!phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() == SUCCESS) {
          if (!rolling) {
            return null;
          }

          List<ServiceInstance> hostExclusionList = stateExecutionService.getHostExclusionList(
              ((ExecutionContextImpl) context).getStateExecutionInstance(), phaseElement);

          String infraMappingId;
          if (phaseElement == null || phaseElement.getInfraMappingId() == null) {
            List<InfrastructureMapping> resolvedInfraMappings =
                workflowExecutionService.getResolvedInfraMappings(workflow, workflowExecution);
            if (isEmpty(resolvedInfraMappings)) {
              return anExecutionEventAdvice()
                  .withExecutionInterruptType(ExecutionInterruptType.NEXT_STEP)
                  .withNextStateName(((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow())
                                         .getPostDeploymentSteps()
                                         .getName())
                  .withNextChildStateMachineId(stateExecutionInstance.getChildStateMachineId())
                  .build();
            }
            infraMappingId = resolvedInfraMappings.get(0).getUuid();
          } else {
            infraMappingId = phaseElement.getInfraMappingId();
          }
          ServiceInstanceSelectionParams.Builder selectionParams =
              aServiceInstanceSelectionParams().withExcludedServiceInstanceIds(
                  hostExclusionList.stream().map(ServiceInstance::getUuid).distinct().collect(toList()));
          List<ServiceInstance> serviceInstances =
              infrastructureMappingService.selectServiceInstances(context.getAppId(), infraMappingId,
                  context.getWorkflowExecutionId(), selectionParams.withCount(1).build());

          if (isEmpty(serviceInstances)) {
            return null;
          }
          return anExecutionEventAdvice()
              .withExecutionInterruptType(ExecutionInterruptType.NEXT_STEP)
              .withNextStateName(stateExecutionInstance.getStateName())
              .withNextChildStateMachineId(stateExecutionInstance.getChildStateMachineId())
              .withNextStateDisplayName(computeDisplayName(stateExecutionInstance))
              .build();
        }

        // nothing to do for regular phase with non-error
        if (!phaseSubWorkflow.isRollback() && !ExecutionStatus.isNegativeStatus(executionEvent.getExecutionStatus())) {
          return null;
        }

        // nothing to do for rollback phase that got some error
        if (phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() != SUCCESS) {
          return null;
        }
      } else if (state.getStateType().equals(StateType.PHASE_STEP.name()) && state instanceof PhaseStepSubWorkflow
          && ((PhaseStepSubWorkflow) state).getPhaseStepType().equals(PhaseStepType.ROLLBACK_PROVISIONERS)
          && executionEvent.getExecutionStatus() == SUCCESS) {
        rollbackProvisioners = true;

      } else if (state.getStateType().equals(StateType.PHASE_STEP.name()) && state instanceof PhaseStepSubWorkflow
          && ((PhaseStepSubWorkflow) state).getPhaseStepType().equals(PhaseStepType.PRE_DEPLOYMENT)
          && executionEvent.getExecutionStatus() == FAILED) {
        return getRollbackProvisionerAdviceIfNeeded(orchestrationWorkflow.getPreDeploymentSteps());
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

      if (orchestrationWorkflow == null || orchestrationWorkflow.getRollbackWorkflowPhaseIdMap() == null) {
        return null;
      }

      if (phaseSubWorkflow != null && executionInterrupts != null
          && executionInterrupts.stream().anyMatch(ex -> ex.getExecutionInterruptType() == ROLLBACK)) {
        return phaseSubWorkflowAdvice(orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
      } else if (rollbackProvisioners) {
        List<String> phaseNames =
            orchestrationWorkflow.getWorkflowPhases().stream().map(WorkflowPhase::getName).collect(toList());

        String lastExecutedPhaseName = null;
        for (String phaseName : phaseNames) {
          if (stateExecutionInstance.getStateExecutionMap().containsKey(phaseName)) {
            lastExecutedPhaseName = phaseName;
          } else {
            break;
          }
        }

        if (lastExecutedPhaseName == null) {
          return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.ROLLBACK_DONE).build();
        }

        String finalLastExecutedPhaseName = lastExecutedPhaseName;
        Optional<WorkflowPhase> lastExecutedPhase =
            orchestrationWorkflow.getWorkflowPhases()
                .stream()
                .filter(phase -> phase.getName().equals(finalLastExecutedPhaseName))
                .findFirst();

        if (!lastExecutedPhase.isPresent()
            || !orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().containsKey(lastExecutedPhase.get().getUuid())) {
          return null;
        }
        return anExecutionEventAdvice()
            .withNextStateName(
                orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(lastExecutedPhase.get().getUuid()).getName())
            .withExecutionInterruptType(ROLLBACK)
            .build();
      }

      if (state.getParentId() != null) {
        PhaseStep phaseStep = null;
        if (state.getParentId().equals(orchestrationWorkflow.getPreDeploymentSteps().getUuid())) {
          phaseStep = orchestrationWorkflow.getPreDeploymentSteps();
        } else if (state.getParentId().equals(orchestrationWorkflow.getRollbackProvisioners().getUuid())) {
          phaseStep = orchestrationWorkflow.getRollbackProvisioners();
        } else if (state.getParentId().equals(orchestrationWorkflow.getPostDeploymentSteps().getUuid())) {
          phaseStep = orchestrationWorkflow.getPostDeploymentSteps();
        } else {
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
        if (phaseStep != null && isNotEmpty(phaseStep.getFailureStrategies())) {
          FailureStrategy failureStrategy = rollbackStrategy(phaseStep.getFailureStrategies(), state);
          return getExecutionEventAdvice(
              orchestrationWorkflow, failureStrategy, executionEvent, null, state, stateExecutionInstance);
        }
      }
      FailureStrategy failureStrategy = rollbackStrategy(orchestrationWorkflow.getFailureStrategies(), state);

      return getExecutionEventAdvice(
          orchestrationWorkflow, failureStrategy, executionEvent, phaseSubWorkflow, state, stateExecutionInstance);
    } finally {
      try {
        if (state.getStateType().equals(StateType.PHASE_STEP.name()) && state instanceof PhaseStepSubWorkflow) {
          PhaseStepSubWorkflow phaseStepSubWorkflow = (PhaseStepSubWorkflow) state;
          instanceHelper.extractInstance(
              phaseStepSubWorkflow, executionEvent, workflowExecution, context, stateExecutionInstance);
        }
      } catch (Exception ex) {
        logger.warn("Error while getting workflow execution data for instance sync for execution: {}",
            workflowExecution.getUuid(), ex);
      }
    }
  }

  private ExecutionEventAdvice getExecutionEventAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      FailureStrategy failureStrategy, ExecutionEvent executionEvent, PhaseSubWorkflow phaseSubWorkflow, State state,
      StateExecutionInstance stateExecutionInstance) {
    if (failureStrategy == null) {
      return null;
    }

    RepairActionCode repairActionCode = failureStrategy.getRepairActionCode();
    if (repairActionCode == null) {
      return null;
    }

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

        return phaseSubWorkflowAdvice(orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
      }
      case RETRY: {
        String stateType = executionEvent.getState().getStateType();
        if (stateType.equals(StateType.PHASE.name()) || stateType.equals(StateType.PHASE_STEP.name())
            || stateType.equals(StateType.SUB_WORKFLOW.name()) || stateType.equals(StateType.FORK.name())
            || stateType.equals(REPEAT.name())) {
          // Retry is only at the leaf node
          FailureStrategy failureStrategyAfterRetry =
              FailureStrategy.builder().repairActionCode(failureStrategy.getRepairActionCodeAfterRetry()).build();
          return getExecutionEventAdvice(orchestrationWorkflow, failureStrategyAfterRetry, executionEvent,
              phaseSubWorkflow, state, stateExecutionInstance);
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
              FailureStrategy.builder().repairActionCode(failureStrategy.getRepairActionCodeAfterRetry()).build();
          return getExecutionEventAdvice(orchestrationWorkflow, failureStrategyAfterRetry, executionEvent,
              phaseSubWorkflow, state, stateExecutionInstance);
        }
      }
      default:
        return null;
    }
  }

  private OrchestrationWorkflow findOrchestrationWorkflow(Workflow workflow, WorkflowExecution workflowExecution) {
    if (workflow == null || workflow.getOrchestrationWorkflow() == null
        || !(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
      return null;
    }

    return workflow.getOrchestrationWorkflow();
  }

  private ExecutionEventAdvice computeRollingPhase(StateExecutionInstance stateExecutionInstance) {
    return anExecutionEventAdvice()
        .withExecutionInterruptType(ExecutionInterruptType.NEXT_STEP)
        .withNextStateName(stateExecutionInstance.getStateName())
        .withNextChildStateMachineId(stateExecutionInstance.getChildStateMachineId())
        .withNextStateDisplayName(computeDisplayName(stateExecutionInstance))
        .build();
  }

  public String computeDisplayName(StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getOrchestrationWorkflowType() == ROLLING
        && PHASE.name().equals(stateExecutionInstance.getStateType()) && !stateExecutionInstance.isRollback()) {
      List<String> phaseNamesAPI = stateExecutionService.phaseNames(
          stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid());

      final long count = phaseNamesAPI.stream().filter(key -> key.startsWith(ROLLING_PHASE_PREFIX)).count();
      return ROLLING_PHASE_PREFIX + (count + 1);
    }
    return null;
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
    Optional<GraphNode> node1 =
        graph.getNodes().stream().filter(node -> node.getId().equals(state.getId())).findFirst();
    if (!node1.isPresent()) {
      return null;
    }

    return node1.get().getProperties();
  }

  private ExecutionEventAdvice phaseSubWorkflowAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      PhaseSubWorkflow phaseSubWorkflow, StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getOrchestrationWorkflowType() == ROLLING) {
      return phaseSubWorkflowAdviceForRolling(orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
    } else {
      return phaseSubWorkflowAdviceForOthers(orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
    }
  }

  private ExecutionEventAdvice phaseSubWorkflowAdviceForRolling(CanaryOrchestrationWorkflow orchestrationWorkflow,
      PhaseSubWorkflow phaseSubWorkflow, StateExecutionInstance stateExecutionInstance) {
    int rollingIndex;
    if (!phaseSubWorkflow.isRollback()) {
      rollingIndex = Integer.parseInt(stateExecutionInstance.getDisplayName().substring(ROLLING_PHASE_PREFIX.length()));
    } else {
      rollingIndex = Integer.parseInt(
          stateExecutionInstance.getDisplayName().substring(ROLLBACK_PREFIX.length() + PHASE_NAME_PREFIX.length()));
      rollingIndex--;
    }
    if (rollingIndex < 1) {
      return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.ROLLBACK_DONE).build();
    }
    return anExecutionEventAdvice()
        .withNextStateName(orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().values().iterator().next().getName())
        .withNextStateDisplayName(ROLLBACK_PREFIX + PHASE_NAME_PREFIX + rollingIndex)
        .withRollbackPhaseName(ROLLING_PHASE_PREFIX + rollingIndex)
        .withExecutionInterruptType(ROLLBACK)
        .build();
  }

  private ExecutionEventAdvice getRollbackProvisionerAdviceIfNeeded(PhaseStep preDeploymentSteps) {
    if (preDeploymentSteps != null && preDeploymentSteps.getSteps() != null
        && preDeploymentSteps.getSteps().stream().anyMatch(step -> {
             return step.getType().equals(StateType.CLOUD_FORMATION_CREATE_STACK.name())
                 || step.getType().equals(StateType.TERRAFORM_PROVISION.getType());
           })) {
      return anExecutionEventAdvice()
          .withNextStateName(Constants.ROLLBACK_PROVISIONERS)
          .withExecutionInterruptType(ROLLBACK)
          .build();
    }
    return null;
  }
  private ExecutionEventAdvice phaseSubWorkflowAdviceForOthers(CanaryOrchestrationWorkflow orchestrationWorkflow,
      PhaseSubWorkflow phaseSubWorkflow, StateExecutionInstance stateExecutionInstance) {
    if (!phaseSubWorkflow.isRollback()) {
      ExecutionEventAdvice rollbackProvisionerAdvice =
          getRollbackProvisionerAdviceIfNeeded(orchestrationWorkflow.getPreDeploymentSteps());
      if (rollbackProvisionerAdvice != null) {
        return rollbackProvisionerAdvice;
      }

      return anExecutionEventAdvice()
          .withNextStateName(
              orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().get(phaseSubWorkflow.getId()).getName())
          .withExecutionInterruptType(ROLLBACK)
          .build();
    }

    List<String> phaseNames =
        orchestrationWorkflow.getWorkflowPhases().stream().map(WorkflowPhase::getName).collect(toList());
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
            .collect(toList());

    if (filteredFailureStrategies.isEmpty()) {
      filteredFailureStrategies =
          failureStrategies.stream().filter(f -> isEmpty(f.getSpecificSteps())).collect(toList());
      if (filteredFailureStrategies.isEmpty()) {
        return null;
      }
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
