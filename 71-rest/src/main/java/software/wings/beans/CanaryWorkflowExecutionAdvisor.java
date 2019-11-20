package software.wings.beans;

import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.ROLLING;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.common.Constants.PHASE_NAME_PREFIX;
import static software.wings.service.impl.workflow.WorkflowServiceHelper.ROLLBACK_PREFIX;
import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.ROLLBACK;
import static software.wings.sm.StateType.FORK;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;
import static software.wings.sm.StateType.REPEAT;
import static software.wings.sm.StateType.SUB_WORKFLOW;
import static software.wings.sm.rollback.RollbackStateMachineGenerator.STAGING_PHASE_NAME;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.exception.FailureType;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.PhaseElement;
import software.wings.service.impl.instance.InstanceHelper;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionEvent;
import software.wings.sm.ExecutionEventAdvice;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by rishi on 1/24/17.
 */
@Slf4j
public class CanaryWorkflowExecutionAdvisor implements ExecutionEventAdvisor {
  public static final String ROLLBACK_PROVISIONERS = "Rollback Provisioners";
  private static final String ROLLING_PHASE_PREFIX = "Rolling Phase ";

  @Inject @Transient private transient WorkflowExecutionService workflowExecutionService;

  @Inject @Transient private transient WorkflowService workflowService;

  @Inject @Transient private transient WorkflowNotificationHelper workflowNotificationHelper;

  @Inject @Transient private transient ExecutionInterruptManager executionInterruptManager;

  @Inject @Transient private transient InstanceHelper instanceHelper;

  @Inject @Transient private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient StateExecutionService stateExecutionService;

  @Inject @Transient private transient WorkflowServiceHelper workflowServiceHelper;

  @Inject @Transient private transient FeatureFlagService featureFlagService;

  @Override
  public ExecutionEventAdvice onExecutionEvent(ExecutionEvent executionEvent) {
    State state = executionEvent.getState();
    ExecutionContextImpl context = executionEvent.getContext();
    WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();

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
      if (stateExecutionInstance != null && stateExecutionInstance.getOrchestrationWorkflowType() == ROLLING
          && !workflowServiceHelper.isExecutionForK8sV2Service(workflowExecution)) {
        rolling = true;
      }

      if (rolling && state.getStateType().equals(StateType.PHASE_STEP.name())
          && state.getName().equals(PRE_DEPLOYMENT.getDefaultName())
          && executionEvent.getExecutionStatus() == SUCCESS) {
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
      PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);

      boolean rollbackProvisioners = false;
      if (state.getStateType().equals(StateType.PHASE.name()) && state instanceof PhaseSubWorkflow) {
        phaseSubWorkflow = (PhaseSubWorkflow) state;

        workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
            context, executionEvent.getExecutionStatus(), phaseSubWorkflow);

        if (!phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() == SUCCESS) {
          if (phaseSubWorkflow.getName().startsWith(STAGING_PHASE_NAME)
              && executionEvent.getExecutionStatus() == SUCCESS) {
            return phaseSubWorkflowOnDemandRollbackAdvice(
                orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
          }

          if (!rolling) {
            return null;
          }

          if (isExecutionHostsPresent(context)) {
            return null;
          }

          List<ServiceInstance> hostExclusionList = stateExecutionService.getHostExclusionList(
              context.getStateExecutionInstance(), phaseElement, context.fetchInfraMappingId());

          String infraMappingId;
          if (context.fetchInfraMappingId() == null) {
            List<InfrastructureMapping> resolvedInfraMappings;
            if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, workflow.getAccountId())) {
              resolvedInfraMappings = infrastructureMappingService.getInfraStructureMappingsByUuids(
                  workflow.getAppId(), workflowExecution.getInfraMappingIds());
            } else {
              resolvedInfraMappings = workflowExecutionService.getResolvedInfraMappings(workflow, workflowExecution);
            }
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
            infraMappingId = context.fetchInfraMappingId();
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
          && ((PhaseStepSubWorkflow) state).getPhaseStepType().equals(PRE_DEPLOYMENT)
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
        } else if (orchestrationWorkflow.getRollbackProvisioners() != null
            && state.getParentId().equals(orchestrationWorkflow.getRollbackProvisioners().getUuid())) {
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
          FailureStrategy failureStrategy = selectTopMatchingStrategy(
              phaseStep.getFailureStrategies(), executionEvent.getFailureTypes(), state.getName());
          return computeExecutionEventAdvice(
              orchestrationWorkflow, failureStrategy, executionEvent, null, stateExecutionInstance);
        }
      }
      FailureStrategy failureStrategy = selectTopMatchingStrategy(
          orchestrationWorkflow.getFailureStrategies(), executionEvent.getFailureTypes(), state.getName());

      return computeExecutionEventAdvice(
          orchestrationWorkflow, failureStrategy, executionEvent, phaseSubWorkflow, stateExecutionInstance);
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

  boolean isExecutionHostsPresent(ExecutionContextImpl context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    if (workflowStandardParams != null && isNotEmpty(workflowStandardParams.getExecutionHosts())) {
      logger.info("Not generating rolling  phases when execution hosts are present");
      return true;
    }
    return false;
  }

  private ExecutionEventAdvice computeExecutionEventAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      FailureStrategy failureStrategy, ExecutionEvent executionEvent, PhaseSubWorkflow phaseSubWorkflow,
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

      case ABORT_WORKFLOW_EXECUTION: {
        ExecutionInterrupt executionInterrupt =
            anExecutionInterrupt()
                .withExecutionInterruptType(ExecutionInterruptType.ABORT_ALL)
                .withExecutionUuid(executionEvent.getContext().getWorkflowExecutionId())
                .withAppId(executionEvent.getContext().getAppId())
                .build();
        workflowExecutionService.triggerExecutionInterrupt(executionInterrupt);
        return anExecutionEventAdvice().withExecutionInterruptType(ExecutionInterruptType.END_EXECUTION).build();
      }

      case MANUAL_INTERVENTION: {
        State state = executionEvent.getState();
        if (REPEAT.name().equals(state.getStateType()) || FORK.name().equals(state.getStateType())
            || PHASE.name().equals(state.getStateType()) || PHASE_STEP.name().equals(state.getStateType())
            || SUB_WORKFLOW.name().equals(state.getStateType())) {
          return null;
        }

        Map<String, Object> stateParams = fetchStateParams(orchestrationWorkflow, state, executionEvent);
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

        if (!orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().containsKey(phaseSubWorkflow.getId())) {
          return null;
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
          return computeExecutionEventAdvice(orchestrationWorkflow, failureStrategyAfterRetry, executionEvent,
              phaseSubWorkflow, stateExecutionInstance);
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
          return computeExecutionEventAdvice(orchestrationWorkflow, failureStrategyAfterRetry, executionEvent,
              phaseSubWorkflow, stateExecutionInstance);
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

  private Map<String, Object> fetchStateParams(
      CanaryOrchestrationWorkflow orchestrationWorkflow, State state, ExecutionEvent executionEvent) {
    if (orchestrationWorkflow == null || orchestrationWorkflow.getGraph() == null || state == null
        || state.getId() == null) {
      return null;
    }
    if (state.getParentId() != null && orchestrationWorkflow.getGraph().getSubworkflows() == null
        || orchestrationWorkflow.getGraph().getSubworkflows().get(state.getParentId()) == null) {
      return null;
    }
    Graph graph = orchestrationWorkflow.getGraph().getSubworkflows().get(state.getParentId());
    GraphNode node1 =
        graph.getNodes().stream().filter(node -> node.getId().equals(state.getId())).findFirst().orElse(null);
    if (node1 == null) {
      return null;
    }

    Map<String, Object> properties = node1.getProperties();
    if (isNotEmpty(state.getTemplateVariables())) {
      properties.put("templateVariables", state.getTemplateVariables());
    }
    if (isNotEmpty(state.getTemplateUuid())) {
      properties.put("templateUuid", state.getTemplateUuid());
    }
    if (isNotEmpty(state.getTemplateVersion())) {
      properties.put("templateVersion", state.getTemplateVersion());
    }
    if (executionEvent != null && executionEvent.getContext() != null
        && isNotEmpty(executionEvent.getContext().getAccountId())) {
      properties.put("accountId", executionEvent.getContext().getAccountId());
    }
    return properties;
  }

  private ExecutionEventAdvice phaseSubWorkflowAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      PhaseSubWorkflow phaseSubWorkflow, StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getOrchestrationWorkflowType() == ROLLING
        && !workflowServiceHelper.isOrchestrationWorkflowForK8sV2Service(
               stateExecutionInstance.getAppId(), orchestrationWorkflow)) {
      return phaseSubWorkflowAdviceForRolling(orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
    } else {
      return phaseSubWorkflowAdviceForOthers(orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
    }
  }

  private ExecutionEventAdvice phaseSubWorkflowOnDemandRollbackAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      PhaseSubWorkflow phaseSubWorkflow, StateExecutionInstance stateExecutionInstance) {
    if (orchestrationWorkflow.checkLastPhase(phaseSubWorkflow.getName())) {
      return phaseSubWorkflowAdviceForOthers(orchestrationWorkflow, phaseSubWorkflow, stateExecutionInstance);
    }
    return null;
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
        && preDeploymentSteps.getSteps().stream().anyMatch(step
               -> step.getType().equals(StateType.CLOUD_FORMATION_CREATE_STACK.name())
                   || step.getType().equals(StateType.TERRAFORM_PROVISION.getType()))) {
      return anExecutionEventAdvice()
          .withNextStateName(ROLLBACK_PROVISIONERS)
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

      if (!orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().containsKey(phaseSubWorkflow.getId())) {
        return null;
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

  public static FailureStrategy selectTopMatchingStrategy(
      List<FailureStrategy> failureStrategies, EnumSet<FailureType> failureTypes, String stateName) {
    final FailureStrategy failureStrategy =
        selectTopMatchingStrategyInternal(failureStrategies, failureTypes, stateName);

    if (failureStrategy != null && isNotEmpty(failureStrategy.getFailureTypes()) && isEmpty(failureTypes)) {
      logger.error("Defaulting to accepting the action. "
              + "the propagated failure types for state {} are unknown. ",
          stateName);
    }

    return failureStrategy;
  }

  private static FailureStrategy selectTopMatchingStrategyInternal(
      List<FailureStrategy> failureStrategies, EnumSet<FailureType> failureTypes, String stateName) {
    if (isEmpty(failureStrategies)) {
      return null;
    }

    List<FailureStrategy> filteredFailureStrategies =
        failureStrategies.stream()
            .filter(failureStrategy -> {
              // we need at least one specific failure else we assume that we should apply in every case
              if (isEmpty(failureStrategy.getFailureTypes())) {
                return true;
              }
              // we need at least one failure type returned from the error to filter out
              if (isEmpty(failureTypes)) {
                return true;
              }
              return !disjoint(failureTypes, failureStrategy.getFailureTypes());
            })
            .collect(toList());

    filteredFailureStrategies = filteredFailureStrategies.stream()
                                    .filter(failureStrategy
                                        -> isEmpty(failureStrategy.getSpecificSteps())
                                            || failureStrategy.getSpecificSteps().contains(stateName))
                                    .collect(toList());

    if (filteredFailureStrategies.isEmpty()) {
      return null;
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
