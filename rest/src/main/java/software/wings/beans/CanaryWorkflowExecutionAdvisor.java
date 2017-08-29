package software.wings.beans;

import static software.wings.beans.FailureStrategy.FailureStrategyBuilder.aFailureStrategy;
import static software.wings.sm.ExecutionEventAdvice.ExecutionEventAdviceBuilder.anExecutionEventAdvice;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.ROLLBACK;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.Lists;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.HostElement;
import software.wings.api.InstanceChangeEvent.Builder;
import software.wings.api.PhaseElement;
import software.wings.api.InstanceChangeEvent;
import software.wings.api.PhaseExecutionData;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.ContainerMetadata;
import software.wings.beans.infrastructure.Ec2InstanceMetadata;
import software.wings.beans.infrastructure.Instance;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.service.impl.WorkflowNotificationHelper;
import software.wings.service.impl.dashboardStats.InstanceChangeEventListener;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.dashboardStats.InstanceService;
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
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.List;
import java.util.Optional;
import software.wings.core.queue.Queue;
import software.wings.utils.Validator;

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

  @Inject @Transient private transient ExecutionInterruptManager executionInterruptManager;

  @Inject @Transient private transient AppService appService;

  // This queue is used to asynchronously process all the instance information that the workflow touched upon.
  @Inject @Transient private transient Queue<InstanceChangeEvent> instanceChangeEventQueue;

  @Inject @Transient private transient InstanceService instanceService;

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
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    if (state.getStateType().equals(StateType.PHASE.name()) && state instanceof PhaseSubWorkflow) {
      phaseSubWorkflow = (PhaseSubWorkflow) state;

      workflowNotificationHelper.sendWorkflowPhaseStatusChangeNotification(
          context, executionEvent.getExecutionStatus(), phaseSubWorkflow, workflowExecution);

      if (executionEvent.getExecutionStatus() == ExecutionStatus.SUCCESS) {
        updateInstanceInfoAsync(context, workflowExecution);
      }

      // nothing to do for regular phase with non-error
      if (!phaseSubWorkflow.isRollback() && executionEvent.getExecutionStatus() != FAILED
          && executionEvent.getExecutionStatus() != ERROR) {
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
        return getExecutionEventAdvice(orchestrationWorkflow, failureStrategy, executionEvent, null);
      }
    }
    FailureStrategy failureStrategy = rollbackStrategy(orchestrationWorkflow.getFailureStrategies(), state);

    return getExecutionEventAdvice(orchestrationWorkflow, failureStrategy, executionEvent, phaseSubWorkflow);
  }

  /**
   *   The phaseExecutionData is used to process the instance information that is used by the service and infra
   * dashboards. The instance processing happens asynchronously.
   *   @see InstanceChangeEventListener
   */
  private void updateInstanceInfoAsync(ExecutionContext executionContext, WorkflowExecution workflowExecution) {
    try {
      StateExecutionData stateExecutionData = executionContext.getStateExecutionData();
      if (!(stateExecutionData instanceof PhaseExecutionData)) {
        logger.error("stateExecutionData is not of type PhaseExecutionData");
        return;
      }

      PhaseExecutionData phaseExecutionData = (PhaseExecutionData) stateExecutionData;
      Validator.notNullCheck("PhaseExecutionData", phaseExecutionData);
      Validator.notNullCheck("ElementStatusSummary", phaseExecutionData.getElementStatusSummary());

      WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
      if (workflowStandardParams == null) {
        logger.error("workflowStandardParams can't be null");
        return;
      }

      Artifact artifact = workflowStandardParams.getArtifactForService(phaseExecutionData.getServiceId());
      if (artifact == null) {
        logger.error("artifact can't be null");
        return;
      }

      List<Instance> instanceList = Lists.newArrayList();

      for (ElementExecutionSummary summary : phaseExecutionData.getElementStatusSummary()) {
        List<InstanceStatusSummary> instanceStatusSummaries = summary.getInstanceStatusSummaries();
        if (instanceStatusSummaries == null) {
          logger.debug("No instances to process");
          return;
        }
        for (InstanceStatusSummary instanceStatusSummary : instanceStatusSummaries) {
          if (shouldCaptureInstance(instanceStatusSummary.getStatus())) {
            Instance instance = buildInstance(workflowExecution, artifact, instanceStatusSummary, phaseExecutionData);
            instanceList.add(instance);
          }
        }
      }

      InstanceChangeEvent instanceChangeEvent =
          Builder.anInstanceChangeEvent().withInstanceList(instanceList).withRetries(1).build();
      instanceChangeEventQueue.send(instanceChangeEvent);
    } catch (Exception ex) {
      // we deliberately don't throw back the exception since we don't want the workflow to be affected
      logger.error("Error while updating instance change information", ex);
    }
  }

  private Instance buildInstance(WorkflowExecution workflowExecution, Artifact artifact,
      InstanceStatusSummary instanceStatusSummary, PhaseExecutionData phaseExecutionData) {
    HostElement host = instanceStatusSummary.getInstanceElement().getHost();
    Validator.notNullCheck("Host", host);
    PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
    Application application = appService.get(workflowExecution.getAppId());
    Validator.notNullCheck("Application", application);
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    Validator.notNullCheck("triggeredBy", triggeredBy);

    Instance.Builder builder =
        Instance.Builder.anInstance()
            .withAccountId(application.getAccountId())
            .withAppId(workflowExecution.getAppId())
            .withAppName(workflowExecution.getAppName())
            .withLastArtifactId(artifact.getUuid())
            .withLastArtifactName(artifact.getDisplayName())
            .withLastArtifactStreamId(artifact.getArtifactStreamId())
            .withLastArtifactSourceName(artifact.getArtifactSourceName())
            .withLastArtifactBuildNum(artifact.getBuildNo())
            .withEnvName(workflowExecution.getEnvName())
            .withEnvId(workflowExecution.getEnvId())
            .withEnvType(workflowExecution.getEnvType())
            .withComputeProviderId(phaseExecutionData.getComputeProviderId())
            .withComputeProviderName(phaseExecutionData.getComputeProviderName())
            .withHostId(host.getUuid())
            .withHostName(host.getHostName())
            .withHostPublicDns(host.getPublicDns())
            .withInfraMappingId(phaseExecutionData.getInfraMappingId())
            .withInfraMappingType(phaseExecutionData.getInfraMappingName())
            .withLastPipelineId(pipelineSummary == null ? null : pipelineSummary.getPipelineId())
            .withLastPipelineName(pipelineSummary == null ? null : pipelineSummary.getPipelineName())
            .withLastDeployedAt(phaseExecutionData.getEndTs().longValue())
            .withLastDeployedById(triggeredBy.getUuid())
            .withLastDeployedByName(triggeredBy.getName())
            .withServiceId(phaseExecutionData.getServiceId())
            .withServiceName(phaseExecutionData.getServiceName())
            .withLastWorkflowId(workflowExecution.getUuid())
            .withLastWorkflowName(workflowExecution.getName());

    setInstanceMetadata(builder, phaseExecutionData, host);
    return builder.build();
  }

  private void setInstanceMetadata(Instance.Builder builder, PhaseExecutionData phaseExecutionData, HostElement host) {
    if (phaseExecutionData.getClusterName() != null) {
      ContainerMetadata containerMetadata =
          ContainerMetadata.Builder.aContainerMetadata().withClusterName(phaseExecutionData.getClusterName()).build();
      builder.withMetadata(containerMetadata);
    }

    com.amazonaws.services.ec2.model.Instance ec2Instance = host.getEc2Instance();
    if (ec2Instance != null) {
      Ec2InstanceMetadata ec2InstanceMetadata =
          Ec2InstanceMetadata.Builder.anEc2InstanceMetaInfo().withEc2Instance(ec2Instance).build();
      builder.withMetadata(ec2InstanceMetadata);
    }
  }

  /**
   * At the end of the phase, the instance can only be in one of the following states.
   */
  private boolean shouldCaptureInstance(ExecutionStatus instanceExecutionStatus) {
    // Instance would have a status but just in case.
    if (instanceExecutionStatus == null) {
      return false;
    }

    switch (instanceExecutionStatus) {
      case SUCCESS:
      case FAILED:
      case ERROR:
      case ABORTED:
        return true;
      default:
        return false;
    }
  }

  private ExecutionEventAdvice getExecutionEventAdvice(CanaryOrchestrationWorkflow orchestrationWorkflow,
      FailureStrategy failureStrategy, ExecutionEvent executionEvent, PhaseSubWorkflow phaseSubWorkflow) {
    if (failureStrategy == null) {
      return null;
    }

    RepairActionCode repairActionCode = failureStrategy.getRepairActionCode();
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
            || stateType.equals(StateType.REPEAT.name())) {
          // Retry is only at the leaf node
          FailureStrategy failureStrategyAfterRetry =
              aFailureStrategy().withRepairActionCode(failureStrategy.getRepairActionCodeAfterRetry()).build();
          return getExecutionEventAdvice(
              orchestrationWorkflow, failureStrategyAfterRetry, executionEvent, phaseSubWorkflow);
        }

        List<StateExecutionData> stateExecutionDataHistory = ((ExecutionContextImpl) executionEvent.getContext())
                                                                 .getStateExecutionInstance()
                                                                 .getStateExecutionDataHistory();
        if (stateExecutionDataHistory == null || stateExecutionDataHistory.size() < failureStrategy.getRetryCount()) {
          int waitInterval = 0;
          List<Integer> retryIntervals = failureStrategy.getRetryIntervals();
          if (retryIntervals != null && !retryIntervals.isEmpty()) {
            if (stateExecutionDataHistory == null || stateExecutionDataHistory.isEmpty()) {
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
              orchestrationWorkflow, failureStrategyAfterRetry, executionEvent, phaseSubWorkflow);
        }
      }
      default:
        return null;
    }
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
    if (failureStrategies == null || failureStrategies.isEmpty()) {
      return null;
    }
    Optional<FailureStrategy> rollbackStrategy =
        failureStrategies.stream()
            .filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_WORKFLOW)
            .findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get();
    }

    rollbackStrategy =
        failureStrategies.stream().filter(f -> f.getRepairActionCode() == RepairActionCode.ROLLBACK_PHASE).findFirst();

    if (rollbackStrategy.isPresent()) {
      return rollbackStrategy.get();
    }

    return failureStrategies.get(0);
  }
}
