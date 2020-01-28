package software.wings.service.impl;

import static io.fabric8.utils.Lists.isNullOrEmpty;
import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.NEW;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.PAUSING;
import static io.harness.beans.ExecutionStatus.QUEUED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.STARTING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.GE;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.beans.SearchFilter.Operator.LT_EQ;
import static io.harness.beans.SearchFilter.Operator.NOT_EXISTS;
import static io.harness.beans.SortOrder.Builder.aSortOrder;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.time.Duration.ofDays;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ApprovalDetails.Action.APPROVE;
import static software.wings.beans.ApprovalDetails.Action.REJECT;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.EntityType.DEPLOYMENT;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;
import static software.wings.beans.FeatureName.NODE_AGGREGATION;
import static software.wings.beans.FeatureName.SSH_WINRM_SO;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY;
import static software.wings.beans.config.ArtifactSourceable.ARTIFACT_SOURCE_DOCKER_CONFIG_PLACEHOLDER;
import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.PAUSE_ALL;
import static software.wings.sm.ExecutionInterruptType.RESUME_ALL;
import static software.wings.sm.InfraMappingSummary.Builder.anInfraMappingSummary;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ARTIFACT_COLLECTION;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.beans.WorkflowType;
import io.harness.cache.Distributable;
import io.harness.cache.Ordinal;
import io.harness.context.ContextElementType;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.limits.InstanceUsageExceededLimitException;
import io.harness.limits.checker.LimitApproachingException;
import io.harness.limits.checker.UsageLimitExceededException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.HIterator;
import io.harness.queue.QueuePublisher;
import io.harness.serializer.KryoUtils;
import io.harness.serializer.MapperUtils;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.CriteriaContainerImpl;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.DeploymentType;
import software.wings.api.EnvStateExecutionData;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.KubernetesSteadyStateCheckExecutionData;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.PipelineElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.api.WorkflowElement;
import software.wings.api.WorkflowElement.WorkflowElementBuilder;
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.ApprovalAuthorization;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.AwsLambdaExecutionSummary;
import software.wings.beans.Base;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CanaryWorkflowExecutionAdvisor;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.EnvSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.FeatureName;
import software.wings.beans.GraphGroup;
import software.wings.beans.GraphNode;
import software.wings.beans.HelmExecutionSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.StateExecutionElement;
import software.wings.beans.StateExecutionInterrupt;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DeploymentRateApproachingLimitAlert;
import software.wings.beans.alert.UsageLimitExceededAlert;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.beans.baseline.WorkflowExecutionBaseline.WorkflowExecutionBaselineKeys;
import software.wings.beans.concurrency.ConcurrentExecutionResponse;
import software.wings.beans.concurrency.ConcurrentExecutionResponse.ConcurrentExecutionResponseBuilder;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.deployment.WorkflowVariablesMetadata;
import software.wings.beans.execution.RollbackType;
import software.wings.beans.execution.RollbackWorkflowExecutionInfo;
import software.wings.beans.execution.WorkflowExecutionInfo;
import software.wings.beans.execution.WorkflowExecutionInfo.WorkflowExecutionInfoBuilder;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.trigger.Trigger;
import software.wings.common.cache.MongoStore;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidBaselineConfigurationException;
import software.wings.infra.InfrastructureDefinition;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.WorkflowExecutionServiceImpl.Tree.TreeBuilder;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.impl.deployment.checks.DeploymentCtx;
import software.wings.service.impl.deployment.checks.DeploymentFreezeChecker;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.workflow.queuing.WorkflowConcurrencyHelper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.BarrierService.OrchestrationWorkflowInfo;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.service.intfc.deployment.AccountExpiryCheck;
import software.wings.service.intfc.deployment.PreDeploymentChecker;
import software.wings.service.intfc.deployment.RateLimitCheck;
import software.wings.service.intfc.deployment.ServiceInstanceUsage;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterrupt.ExecutionInterruptKeys;
import software.wings.sm.ExecutionInterruptEffect;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.InfraDefinitionSummary;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.RollbackConfirmation;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.rollback.RollbackStateMachineGenerator;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.sm.states.HoldingScope;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;

import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class WorkflowExecutionServiceImpl.
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {
  @Inject private MainConfiguration mainConfiguration;
  @Inject private BarrierService barrierService;
  @Inject private StateMachineExecutor stateMachineExecutor;
  @Inject private EnvironmentService environmentService;
  @Inject private StateExecutionService stateExecutionService;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ArtifactService artifactService;
  @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Inject private GraphRenderer graphRenderer;
  @Inject private AppService appService;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private PipelineService pipelineService;
  @Inject private ExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private QueuePublisher<ExecutionEvent> executionEventQueue;
  @Inject private WorkflowExecutionBaselineService workflowExecutionBaselineService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private MongoStore mongoStore;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserGroupService userGroupService;
  @Inject private AuthHandler authHandler;
  @Inject private SweepingOutputService sweepingOutputService;
  @Inject private PreDeploymentChecks preDeploymentChecks;
  @Inject private AlertService alertService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WorkflowExecutionServiceHelper workflowExecutionServiceHelper;
  @Inject private MultiArtifactWorkflowExecutionServiceHelper multiArtifactWorkflowExecutionServiceHelper;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
  @Inject private GovernanceConfigService governanceConfigService;
  @Inject private HostService hostService;
  @Inject private WorkflowConcurrencyHelper workflowConcurrencyHelper;
  @Inject private ResourceConstraintService resourceConstraintService;
  @Inject private AuthService authService;
  @Inject private RollbackStateMachineGenerator rollbackStateMachineGenerator;

  @Inject @RateLimitCheck private PreDeploymentChecker deployLimitChecker;
  @Inject @ServiceInstanceUsage private PreDeploymentChecker siUsageChecker;
  @Inject @AccountExpiryCheck private PreDeploymentChecker accountExpirationChecker;

  @Override
  public HIterator<WorkflowExecution> executions(
      String appId, long startedFrom, long statedTo, Set<String> includeOnlyFields) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecutionKeys.appId, appId)
                                         .field(WorkflowExecutionKeys.startTs)
                                         .greaterThanOrEq(startedFrom)
                                         .field(WorkflowExecutionKeys.startTs)
                                         .lessThan(statedTo);
    if (isNotEmpty(includeOnlyFields)) {
      includeOnlyFields.forEach(field -> query.project(field, true));
    }

    return new HIterator<>(query.fetch());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<WorkflowExecution> listExecutions(
      PageRequest<WorkflowExecution> pageRequest, boolean includeGraph) {
    return listExecutions(pageRequest, includeGraph, false, true, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<WorkflowExecution> listExecutions(PageRequest<WorkflowExecution> pageRequest,
      boolean includeGraph, boolean runningOnly, boolean withBreakdownAndSummary, boolean includeStatus) {
    PageResponse<WorkflowExecution> res = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (isEmpty(res)) {
      return res;
    }
    for (int i = 0; i < res.size(); i++) {
      WorkflowExecution workflowExecution = res.get(i);
      refreshBreakdown(workflowExecution);
      if (workflowExecution.getWorkflowType() == WorkflowType.PIPELINE) {
        // pipeline
        refreshPipelineExecution(workflowExecution);
        PipelineExecution pipelineExecution = workflowExecution.getPipelineExecution();

        // Done to ignore inconsistent pipeline executions with mismatch from setup
        if (pipelineExecution == null || pipelineExecution.getPipelineStageExecutions() == null
            || pipelineExecution.getPipeline() == null || pipelineExecution.getPipeline().getPipelineStages() == null
            || pipelineExecution.getPipelineStageExecutions().size()
                != pipelineExecution.getPipeline().getPipelineStages().size()) {
          res.remove(i);
          i--;
        }
        continue;
      }
      if (withBreakdownAndSummary) {
        try {
          refreshSummaries(workflowExecution);
        } catch (Exception e) {
          logger.error(
              format("Failed to refresh service summaries for the workflow execution %s", workflowExecution.getUuid()),
              e);
        }
      }

      if (!runningOnly || ExecutionStatus.isRunningStatus(workflowExecution.getStatus())
          || ExecutionStatus.isHaltedStatus(workflowExecution.getStatus())) {
        try {
          populateNodeHierarchy(workflowExecution, includeGraph, includeStatus, false);
        } catch (Exception e) {
          logger.error("Failed to populate node hierarchy for the workflow execution {}", res.toString(), e);
        }
      }
    }
    return res;
  }

  @Override
  public boolean updateNotes(String appId, String workflowExecutionId, ExecutionArgs executionArgs) {
    notNullCheck("executionArgs", executionArgs, USER);
    notNullCheck("notes", executionArgs.getNotes(), USER);

    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("workflowExecution", workflowExecution, USER);

    try {
      Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
                                           .filter(ID_KEY, workflowExecution.getUuid());
      UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                          .set("executionArgs.notes", executionArgs.getNotes());
      UpdateResults updateResults = wingsPersistence.update(query, updateOps);
      return updateResults != null && updateResults.getWriteResult() != null
          && updateResults.getWriteResult().getN() > 0;

    } catch (Exception ex) {
      return false;
    }
  }

  @Override
  public boolean approveOrRejectExecution(String appId, List<String> userGroupIds, ApprovalDetails approvalDetails) {
    if (isNotEmpty(userGroupIds)) {
      if (!verifyAuthorizedToAcceptOrReject(userGroupIds, asList(appId), null)) {
        throw new InvalidRequestException("User not authorized to accept or reject the approval");
      }
    }

    User user = UserThreadLocal.get();
    if (user != null) {
      approvalDetails.setApprovedBy(EmbeddedUser.builder().email(user.getEmail()).name(user.getName()).build());
    }

    if (null == approvalDetails.getApprovedBy()) {
      logger.error("Approved by not set in approval details. Details: {}", approvalDetails);
    }

    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .approvalId(approvalDetails.getApprovalId())
                                                   .approvedBy(approvalDetails.getApprovedBy())
                                                   .comments(approvalDetails.getComments())
                                                   .approvalFromSlack(approvalDetails.isApprovalFromSlack())
                                                   .variables(approvalDetails.getVariables())
                                                   .build();

    if (approvalDetails.getAction() == APPROVE) {
      executionData.setStatus(ExecutionStatus.SUCCESS);
    } else if (approvalDetails.getAction() == REJECT) {
      executionData.setStatus(ExecutionStatus.REJECTED);
    }

    waitNotifyEngine.doneWith(approvalDetails.getApprovalId(), executionData);
    return true;
  }

  @Override
  public ApprovalStateExecutionData fetchApprovalStateExecutionDataFromWorkflowExecution(
      String appId, String workflowExecutionId, String stateExecutionId, ApprovalDetails approvalDetails) {
    notNullCheck("appId", appId, USER);

    notNullCheck("ApprovalDetails", approvalDetails, USER);
    notNullCheck("ApprovalId", approvalDetails.getApprovalId());
    String approvalId = approvalDetails.getApprovalId();
    notNullCheck("Approval action", approvalDetails.getAction());

    notNullCheck("workflowExecutionId", workflowExecutionId, USER);
    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("workflowExecution", workflowExecution, USER);

    ApprovalStateExecutionData approvalStateExecutionData = null;
    String workflowType = "";

    // Pipeline approval
    if (workflowExecution.getWorkflowType() == WorkflowType.PIPELINE) {
      workflowType = "Pipeline";
      approvalStateExecutionData =
          fetchPipelineWaitingApprovalStateExecutionData(workflowExecution.getPipelineExecution(), approvalId);
    }

    // Workflow approval
    if (workflowExecution.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      workflowType = "Workflow";
      approvalStateExecutionData =
          fetchWorkflowWaitingApprovalStateExecutionData(workflowExecution, stateExecutionId, approvalId);
    }

    if (approvalStateExecutionData == null) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args",
              "No " + workflowType + " execution [" + workflowExecutionId + "] waiting for approval id: " + approvalId);
    }

    return approvalStateExecutionData;
  }

  private ApprovalStateExecutionData fetchPipelineWaitingApprovalStateExecutionData(
      PipelineExecution pipelineExecution, String approvalId) {
    if (pipelineExecution == null || pipelineExecution.getPipelineStageExecutions() == null) {
      return null;
    }

    for (PipelineStageExecution pe : pipelineExecution.getPipelineStageExecutions()) {
      if (pe.getStateExecutionData() instanceof ApprovalStateExecutionData) {
        ApprovalStateExecutionData approvalStateExecutionData = (ApprovalStateExecutionData) pe.getStateExecutionData();

        if (pe.getStatus() == ExecutionStatus.PAUSED && approvalStateExecutionData.getStatus() == ExecutionStatus.PAUSED
            && approvalId.equals(approvalStateExecutionData.getApprovalId())) {
          return approvalStateExecutionData;
        }
      }
    }

    return null;
  }

  private ApprovalStateExecutionData fetchWorkflowWaitingApprovalStateExecutionData(
      WorkflowExecution workflowExecution, String stateExecutionId, String approvalId) {
    if (stateExecutionId != null) {
      StateExecutionInstance stateExecutionInstance =
          wingsPersistence.createQuery(StateExecutionInstance.class).filter(ID_KEY, stateExecutionId).get();

      ApprovalStateExecutionData approvalStateExecutionData =
          (ApprovalStateExecutionData) stateExecutionInstance.fetchStateExecutionData();
      // Check for Approval Id in PAUSED status
      if (approvalStateExecutionData != null && approvalStateExecutionData.getStatus() == ExecutionStatus.PAUSED
          && approvalStateExecutionData.getApprovalId().equals(approvalId)) {
        return approvalStateExecutionData;
      }
    } else {
      List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(workflowExecution);
      for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
        if (stateExecutionInstance.fetchStateExecutionData() instanceof ApprovalStateExecutionData) {
          ApprovalStateExecutionData approvalStateExecutionData =
              (ApprovalStateExecutionData) stateExecutionInstance.fetchStateExecutionData();
          // Check for Approval Id in PAUSED status
          if (approvalStateExecutionData != null && approvalStateExecutionData.getStatus() == ExecutionStatus.PAUSED
              && approvalStateExecutionData.getApprovalId().equals(approvalId)) {
            return approvalStateExecutionData;
          }
        }
      }
    }

    return null;
  }

  private void refreshPipelineExecution(WorkflowExecution workflowExecution) {
    if (workflowExecution == null || workflowExecution.getPipelineExecution() == null) {
      return;
    }
    if (ExecutionStatus.isFinalStatus(workflowExecution.getPipelineExecution().getStatus())
        && workflowExecution.getPipelineExecution()
               .getPipelineStageExecutions()
               .stream()
               .flatMap(pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions().stream())
               .allMatch(workflowExecution1 -> ExecutionStatus.isFinalStatus(workflowExecution1.getStatus()))) {
      return;
    }

    PipelineExecution pipelineExecution = workflowExecution.getPipelineExecution();
    Pipeline pipeline = pipelineExecution.getPipeline();
    ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap =
        getStateExecutionInstanceMap(workflowExecution);
    List<PipelineStageExecution> stageExecutionDataList = new ArrayList<>();

    pipeline.getPipelineStages()
        .stream()
        .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
        .forEach(pipelineStageElement -> {
          StateExecutionInstance stateExecutionInstance = stateExecutionInstanceMap.get(pipelineStageElement.getName());

          if (stateExecutionInstance == null) {
            Long estimatedTime = pipeline.getStateEtaMap().get(pipelineStageElement.getUuid());
            // required for backward compatibility, Can be removed later
            if (estimatedTime == null) {
              estimatedTime = pipeline.getStateEtaMap().get(pipelineStageElement.getName());
            }
            stageExecutionDataList.add(PipelineStageExecution.builder()
                                           .stateUuid(pipelineStageElement.getUuid())
                                           .stateType(pipelineStageElement.getType())
                                           .stateName(pipelineStageElement.getName())
                                           .status(ExecutionStatus.QUEUED)
                                           .estimatedTime(estimatedTime)
                                           .build());

          } else if (APPROVAL.name().equals(stateExecutionInstance.getStateType())) {
            PipelineStageExecution stageExecution = PipelineStageExecution.builder()
                                                        .stateUuid(pipelineStageElement.getUuid())
                                                        .stateType(stateExecutionInstance.getStateType())
                                                        .status(stateExecutionInstance.getStatus())
                                                        .stateName(stateExecutionInstance.getDisplayName())
                                                        .startTs(stateExecutionInstance.getStartTs())
                                                        .endTs(stateExecutionInstance.getEndTs())
                                                        .build();
            StateExecutionData stateExecutionData = stateExecutionInstance.fetchStateExecutionData();

            if (stateExecutionData instanceof ApprovalStateExecutionData) {
              stageExecution.setStateExecutionData(stateExecutionData);

              ApprovalStateExecutionData approvalStateExecutionData = (ApprovalStateExecutionData) stateExecutionData;
              approvalStateExecutionData.setUserGroupList(
                  userGroupService.fetchUserGroupNamesFromIds(approvalStateExecutionData.getUserGroups()));
              approvalStateExecutionData.setAuthorized(
                  verifyAuthorizedToAcceptOrReject(approvalStateExecutionData.getUserGroups(),
                      asList(pipeline.getAppId()), pipelineExecution.getPipelineId()));
              approvalStateExecutionData.setAppId(pipeline.getAppId());
              approvalStateExecutionData.setWorkflowId(pipelineExecution.getPipelineId());
            }
            stageExecutionDataList.add(stageExecution);

          } else if (ENV_STATE.name().equals(stateExecutionInstance.getStateType())) {
            PipelineStageExecution stageExecution = PipelineStageExecution.builder()
                                                        .stateUuid(pipelineStageElement.getUuid())
                                                        .stateType(pipelineStageElement.getType())
                                                        .stateName(pipelineStageElement.getName())
                                                        .status(stateExecutionInstance.getStatus())
                                                        .startTs(stateExecutionInstance.getStartTs())
                                                        .endTs(stateExecutionInstance.getEndTs())
                                                        .build();
            StateExecutionData stateExecutionData = stateExecutionInstance.fetchStateExecutionData();

            if (stateExecutionData instanceof EnvStateExecutionData) {
              EnvStateExecutionData envStateExecutionData = (EnvStateExecutionData) stateExecutionData;
              if (envStateExecutionData.getWorkflowExecutionId() != null) {
                WorkflowExecution workflowExecution2 = getExecutionDetailsWithoutGraph(
                    workflowExecution.getAppId(), envStateExecutionData.getWorkflowExecutionId());
                workflowExecution2.setStateMachine(null);

                stageExecution.setWorkflowExecutions(asList(workflowExecution2));
                stageExecution.setStatus(workflowExecution2.getStatus());
              }
              stageExecution.setMessage(envStateExecutionData.getErrorMsg());
            }
            stageExecutionDataList.add(stageExecution);

          } else {
            throw new InvalidRequestException("Unknown stateType " + stateExecutionInstance.getStateType());
          }
        });

    pipelineExecution.setPipelineStageExecutions(stageExecutionDataList);

    if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
      pipelineExecution.setStatus(workflowExecution.getStatus());
    } else if (stageExecutionDataList.stream().anyMatch(
                   pipelineStageExecution -> pipelineStageExecution.getStatus() == WAITING)) {
      pipelineExecution.setStatus(WAITING);
    } else if (stageExecutionDataList.stream().anyMatch(pipelineStageExecution
                   -> pipelineStageExecution.getStatus() == PAUSED || pipelineStageExecution.getStatus() == PAUSING)) {
      pipelineExecution.setStatus(PAUSED);
    } else {
      pipelineExecution.setStatus(workflowExecution.getStatus());
    }

    workflowExecution.setStatus(pipelineExecution.getStatus());

    try {
      Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
                                           .filter(ID_KEY, workflowExecution.getUuid());
      UpdateOperations<WorkflowExecution> updateOps =
          wingsPersistence.createUpdateOperations(WorkflowExecution.class).set("pipelineExecution", pipelineExecution);
      wingsPersistence.update(query, updateOps);
      executorService.submit(() -> updatePipelineEstimates(workflowExecution));
    } catch (ConcurrentModificationException cex) {
      // do nothing as it gets refreshed in next fetch
      logger.warn("Pipeline execution update failed ", cex); // TODO: add retry
    }
  }

  private void updatePipelineEstimates(WorkflowExecution workflowExecution) {
    if (!ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
      return;
    }

    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, workflowExecution.getAppId())
                                  .addFilter("workflowId", EQ, workflowExecution.getWorkflowId())
                                  .addFilter("status", EQ, SUCCESS)
                                  .addOrder("endTs", OrderType.DESC)
                                  .withLimit("5")
                                  .build();
    List<WorkflowExecution> workflowExecutions = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    // Adding check for pse.getStateUuid() == null for backward compatibility. Can be removed later
    Map<String, LongSummaryStatistics> stateEstimatesSum =
        workflowExecutions.stream()
            .map(WorkflowExecution::getPipelineExecution)
            .flatMap(pe -> pe.getPipelineStageExecutions().stream())
            .collect(Collectors.groupingBy(pse
                -> (pse.getStateUuid() == null) ? pse.getStateName() : pse.getStateUuid(),
                Collectors.summarizingLong(this ::getEstimate)));

    Map<String, Long> newEstimates = new HashMap<>();

    stateEstimatesSum.keySet().forEach(s -> {
      LongSummaryStatistics longSummaryStatistics = stateEstimatesSum.get(s);
      if (longSummaryStatistics.getCount() != 0) {
        newEstimates.put(s, longSummaryStatistics.getSum() / longSummaryStatistics.getCount());
      }
    });
    wingsPersistence.update(wingsPersistence.createQuery(Pipeline.class)
                                .filter(PipelineKeys.appId, workflowExecution.getAppId())
                                .filter(ID_KEY, workflowExecution.getWorkflowId()),
        wingsPersistence.createUpdateOperations(Pipeline.class).set("stateEtaMap", newEstimates));
  }

  private Long getEstimate(PipelineStageExecution pipelineStageExecution) {
    if (pipelineStageExecution.getEndTs() != null && pipelineStageExecution.getStartTs() != null
        && pipelineStageExecution.getEndTs() > pipelineStageExecution.getStartTs()) {
      return pipelineStageExecution.getEndTs() - pipelineStageExecution.getStartTs();
    }
    return null;
  }

  private ImmutableMap<String, StateExecutionInstance> getStateExecutionInstanceMap(
      WorkflowExecution workflowExecution) {
    List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(workflowExecution);
    return Maps.uniqueIndex(stateExecutionInstances, StateExecutionInstance::getDisplayName);
  }

  private List<StateExecutionInstance> getStateExecutionInstances(WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> req =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("appId", EQ, workflowExecution.getAppId())
            .addFilter("executionUuid", EQ, workflowExecution.getUuid())
            .addFilter(StateExecutionInstanceKeys.createdAt, GE, workflowExecution.getCreatedAt())
            .build();
    return wingsPersistence.query(StateExecutionInstance.class, req).getResponse();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(String appId, String workflowExecutionId, boolean upToDate) {
    WorkflowExecution workflowExecution = getExecutionDetailsWithoutGraph(appId, workflowExecutionId);

    if (workflowExecution.getWorkflowType() == PIPELINE) {
      populateNodeHierarchy(workflowExecution, false, true, upToDate);
    } else {
      populateNodeHierarchy(workflowExecution, true, false, upToDate);
    }
    return workflowExecution;
  }

  @Override
  public WorkflowExecution getExecutionDetailsWithoutGraph(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = getExecutionWithoutSummary(appId, workflowExecutionId);

    if (workflowExecution.getWorkflowType() == PIPELINE) {
      refreshPipelineExecution(workflowExecution);
    } else {
      refreshBreakdown(workflowExecution);
      refreshSummaries(workflowExecution);
    }
    return workflowExecution;
  }

  @Override
  public WorkflowExecution getExecutionWithoutSummary(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("WorkflowExecution", workflowExecution, USER);

    ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
    if (executionArgs != null) {
      if (executionArgs.getServiceInstanceIdNames() != null) {
        executionArgs.setServiceInstances(
            serviceInstanceService.fetchServiceInstances(appId, executionArgs.getServiceInstanceIdNames().keySet()));
      }
      // TODO: This is being called multiple times during execution. We can optimize it by not fetching the artifacts
      // details again if fetched already
      if (executionArgs.getArtifactIdNames() != null) {
        String accountId = appService.getAccountIdByAppId(appId);
        executionArgs.setArtifacts(artifactService.listByIds(accountId, executionArgs.getArtifactIdNames().keySet()));
      }
    }
    return workflowExecution;
  }

  @Override
  public void stateExecutionStatusUpdated(
      String appId, String workflowExecution, String stateExecutionInstanceId, ExecutionStatus status) {
    // Starting is always followed with running. No need to force calculation for it.
    //    if (status == STARTING) {
    //      return;
    //    }
    //
    //    executorService.submit(() -> {
    //      if (proactiveGraphRenderings.get() < MAX_PROACTIVE_GRAPH_RENDERINGS) {
    //        final Tree ignore = calculateTree(null, appId, workflowExecution, ExecutionStatus.RUNNING, emptySet());
    //      }
    //    });
  }

  @Value
  @Builder
  public static class Tree implements Distributable, Ordinal {
    public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(Tree.class).getSerialVersionUID();

    private long contextOrder;
    private String key;
    private List<String> params;

    private ExecutionStatus overrideStatus;
    private GraphNode graph;
    private long lastUpdatedAt = System.currentTimeMillis();

    @Override
    public long structureHash() {
      return STRUCTURE_HASH;
    }

    @Override
    public long algorithmId() {
      return GraphRenderer.algorithmId;
    }

    @Override
    public long contextOrder() {
      return contextOrder;
    }

    @Override
    public String key() {
      return key;
    }

    @Override
    public List<String> parameters() {
      return params;
    }
  }

  private Tree calculateTree(String appId, String workflowExecutionId) {
    Map<String, StateExecutionInstance> allInstancesIdMap =
        stateExecutionService.executionStatesMap(appId, workflowExecutionId);
    Long lastUpdate = allInstancesIdMap.values()
                          .stream()
                          .map(StateExecutionInstance::getLastUpdatedAt)
                          .max(Long::compare)
                          .orElseGet(() -> Long.valueOf(0));

    String accountId = appService.getAccountIdByAppId(appId);
    List<String> params = null;
    params = getParamsForTree(accountId);
    Tree tree = mongoStore.get(GraphRenderer.algorithmId, Tree.STRUCTURE_HASH, workflowExecutionId, params);
    if (tree != null && tree.getContextOrder() >= lastUpdate) {
      return tree;
    }

    TreeBuilder treeBuilder = Tree.builder().key(workflowExecutionId).params(params).contextOrder(lastUpdate);
    if (allInstancesIdMap.values().stream().anyMatch(
            i -> i.getStatus() == ExecutionStatus.PAUSED || i.getStatus() == ExecutionStatus.PAUSING)) {
      treeBuilder.overrideStatus(ExecutionStatus.PAUSED);
    } else if (allInstancesIdMap.values().stream().anyMatch(i -> i.getStatus() == ExecutionStatus.WAITING)) {
      treeBuilder.overrideStatus(ExecutionStatus.WAITING);
    } else {
      List<ExecutionInterrupt> executionInterrupts =
          executionInterruptManager.checkForExecutionInterrupt(appId, workflowExecutionId);
      if (executionInterrupts != null
          && executionInterrupts.stream().anyMatch(
                 e -> e.getExecutionInterruptType() == ExecutionInterruptType.PAUSE_ALL && !e.isSeized())) {
        treeBuilder.overrideStatus(ExecutionStatus.PAUSED);
      }
    }

    treeBuilder.graph(graphRenderer.generateHierarchyNode(allInstancesIdMap));

    Tree cacheTree = treeBuilder.build();
    executorService.submit(() -> { mongoStore.upsert(cacheTree, ofDays(30)); });
    return cacheTree;
  }

  private List<String> getParamsForTree(String accountId) {
    List<String> params = null;
    if (featureFlagService.isEnabled(NODE_AGGREGATION, accountId)) {
      params = Collections.singletonList(String.valueOf(GraphRenderer.AGGREGATION_LIMIT));
    }
    return params;
  }

  @Override
  public WorkflowExecution getWorkflowExecution(String appId, String workflowExecutionId) {
    logger.debug("Retrieving workflow execution details for id {} of App Id {} ", workflowExecutionId, appId);
    return wingsPersistence.getWithAppId(WorkflowExecution.class, appId, workflowExecutionId);
  }

  private void populateNodeHierarchy(
      WorkflowExecution workflowExecution, boolean includeGraph, boolean includeStatus, boolean upToDate) {
    if (includeGraph) {
      includeStatus = true;
    }

    if (!includeStatus && !includeGraph) {
      return;
    }

    Tree tree = null;
    List<String> params = null;
    String accountId = appService.getAccountIdByAppId(workflowExecution.getAppId());
    params = getParamsForTree(accountId);
    if (!upToDate) {
      tree = mongoStore.<Tree>get(GraphRenderer.algorithmId, Tree.STRUCTURE_HASH, workflowExecution.getUuid(), params);
    }

    if (upToDate || tree == null || tree.lastUpdatedAt < (System.currentTimeMillis() - 5000)) {
      tree = calculateTree(workflowExecution.getAppId(), workflowExecution.getUuid());
    }

    if (includeStatus && tree.getOverrideStatus() != null) {
      workflowExecution.setStatus(tree.getOverrideStatus());
    }
    if (includeGraph) {
      workflowExecution.setExecutionNode(tree.getGraph());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerPipelineExecution(
      String appId, String pipelineId, ExecutionArgs executionArgs, Trigger trigger) {
    return triggerPipelineExecution(appId, pipelineId, executionArgs, null, trigger);
  }

  private void constructBarriers(Pipeline pipeline, String pipelineExecutionId) {
    // Initializing the list workarounds an issue with having the first stage having isParallel set.
    List<OrchestrationWorkflowInfo> orchestrationWorkflows = new ArrayList<>();
    int parallelIndex = 0;
    for (PipelineStage stage : pipeline.getPipelineStages()) {
      if (!stage.isParallel()) {
        if (!isEmpty(orchestrationWorkflows)) {
          barrierService
              .obtainInstances(pipeline.getAppId(), orchestrationWorkflows, pipelineExecutionId, parallelIndex)
              .forEach(barrier -> barrierService.save(barrier));
        }
        orchestrationWorkflows = new ArrayList<>();
      }

      // this array is legacy, we always have just one item
      PipelineStageElement element = stage.getPipelineStageElements().get(0);
      parallelIndex = element.getParallelIndex();

      if (element.checkDisableAssertion()) {
        continue;
      }

      if (!ENV_STATE.name().equals(element.getType())) {
        continue;
      }
      Workflow workflow =
          workflowService.readWorkflow(pipeline.getAppId(), (String) element.getProperties().get("workflowId"));

      if (workflow.getOrchestrationWorkflow() != null) {
        orchestrationWorkflows.add(OrchestrationWorkflowInfo.builder()
                                       .workflowId(workflow.getUuid())
                                       .pipelineStageId(element.getUuid())
                                       .orchestrationWorkflow(workflow.getOrchestrationWorkflow())
                                       .build());
      }
    }

    if (!isEmpty(orchestrationWorkflows)) {
      barrierService.obtainInstances(pipeline.getAppId(), orchestrationWorkflows, pipelineExecutionId, parallelIndex)
          .forEach(barrier -> barrierService.save(barrier));
    }
  }

  /**
   * Trigger pipeline execution workflow execution.
   *
   * @param appId                   the app id
   * @param pipelineId              the pipeline id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update  @return the workflow execution
   * @return the workflow execution
   */
  @VisibleForTesting
  WorkflowExecution triggerPipelineExecution(String appId, String pipelineId, ExecutionArgs executionArgs,
      WorkflowExecutionUpdate workflowExecutionUpdate, Trigger trigger) {
    String accountId = appService.getAccountIdByAppId(appId);
    checkPreDeploymentConditions(accountId, appId);

    Pipeline pipeline =
        pipelineService.readPipelineWithResolvedVariables(appId, pipelineId, executionArgs.getWorkflowVariables());

    if (pipeline == null) {
      throw new WingsException(ErrorCode.NON_EXISTING_PIPELINE);
    }

    PreDeploymentChecker deploymentFreezeChecker = new DeploymentFreezeChecker(
        governanceConfigService, new DeploymentCtx(appId, pipeline.getEnvIds()), environmentService);
    deploymentFreezeChecker.check(accountId);

    if (isEmpty(pipeline.getPipelineStages())) {
      throw new WingsException("You can not deploy an empty pipeline.", WingsException.USER);
    }
    preDeploymentChecks.checkIfPipelineUsingRestrictedFeatures(pipeline);

    List<WorkflowExecution> runningWorkflowExecutions =
        getRunningWorkflowExecutions(WorkflowType.PIPELINE, appId, pipelineId);
    if (runningWorkflowExecutions != null) {
      for (WorkflowExecution workflowExecution : runningWorkflowExecutions) {
        if (workflowExecution.getStatus() == NEW) {
          throw new WingsException(ErrorCode.PIPELINE_ALREADY_TRIGGERED).addParam("pipelineName", pipeline.getName());
        }
        // TODO: if (workflowExecution.getStatus() == RUNNING)
        // Analyze if pipeline is in initial stage
      }
    }

    StateMachine stateMachine = new StateMachine(pipeline, workflowService.stencilMap(pipeline.getAppId()));
    stateMachine.setOrchestrationWorkflow(null);

    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .uuid(generateUuid())
                                              .status(NEW)
                                              .appId(appId)
                                              .workflowId(pipelineId)
                                              .workflowType(WorkflowType.PIPELINE)
                                              .stateMachine(stateMachine)
                                              .name(pipeline.getName())
                                              .build();

    constructBarriers(pipeline, workflowExecution.getUuid());

    // Do not remove this. Morphia referencing it by id and one object getting overridden by the other
    pipeline.setUuid(generateUuid() + "_embedded");

    PipelineExecution pipelineExecution =
        aPipelineExecution().withPipelineId(pipelineId).withPipeline(pipeline).build();
    workflowExecution.setPipelineExecution(pipelineExecution);
    workflowExecution.setPipelineSummary(
        PipelineSummary.builder().pipelineId(pipelineId).pipelineName(pipeline.getName()).build());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    if (isNotEmpty(executionArgs.getArtifacts())) {
      stdParams.setArtifactIds(executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(toList()));
    }
    if (isNotEmpty(executionArgs.getWorkflowVariables())) {
      stdParams.setWorkflowVariables(executionArgs.getWorkflowVariables());
    }
    // Setting  exclude hosts with same artifact
    stdParams.setExcludeHostsWithSameArtifact(executionArgs.isExcludeHostsWithSameArtifact());
    stdParams.setNotifyTriggeredUserOnly(executionArgs.isNotifyTriggeredUserOnly());

    User user = UserThreadLocal.get();
    if (user != null) {
      stdParams.setCurrentUser(
          EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
    }
    workflowExecution.setExecutionArgs(executionArgs);

    if (pipeline.getServices() != null) {
      List<ElementExecutionSummary> serviceExecutionSummaries = new ArrayList<>();
      pipeline.getServices().forEach(service -> {
        serviceExecutionSummaries.add(
            anElementExecutionSummary()
                .withContextElement(ServiceElement.builder().uuid(service.getUuid()).name(service.getName()).build())
                .build());
      });
      workflowExecution.setServiceExecutionSummaries(serviceExecutionSummaries);
      workflowExecution.setServiceIds(pipeline.getServices().stream().map(Service::getUuid).collect(toList()));
    }
    workflowExecution.setEnvIds(pipeline.getEnvIds());
    workflowExecution.setWorkflowIds(pipeline.getWorkflowIds());
    workflowExecution.setInfraMappingIds(pipeline.getInfraMappingIds());
    workflowExecution.setCloudProviderIds(
        infrastructureMappingService.fetchCloudProviderIds(appId, workflowExecution.getInfraMappingIds()));

    return triggerExecution(
        workflowExecution, stateMachine, workflowExecutionUpdate, stdParams, trigger, pipeline, null);
  }

  // validations to check is a deployment should be allowed or not
  private void checkPreDeploymentConditions(String accountId, String appId) {
    accountExpirationChecker.check(accountId);
    checkInstanceUsageLimit(accountId, appId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String envId, String workflowId, ExecutionArgs executionArgs, Trigger trigger) {
    return triggerOrchestrationWorkflowExecution(appId, envId, workflowId, null, executionArgs, null, trigger);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerOrchestrationExecution(String appId, String envId, String workflowId,
      String pipelineExecutionId, ExecutionArgs executionArgs, Trigger trigger) {
    return triggerOrchestrationWorkflowExecution(
        appId, envId, workflowId, pipelineExecutionId, executionArgs, null, trigger);
  }

  @Override
  public WorkflowExecution triggerOrchestrationWorkflowExecution(String appId, String envId, String workflowId,
      String pipelineExecutionId, @NotNull ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate,
      Trigger trigger) {
    String accountId = appService.getAccountIdByAppId(appId);
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, accountId);

    logger.info("Execution Triggered. Type: {}", executionArgs.getWorkflowType());

    // TODO - validate list of artifact Ids if it's matching for all the services involved in this orchestration

    Workflow workflow = workflowExecutionServiceHelper.obtainWorkflow(appId, workflowId, infraRefactor);
    String resolveEnvId = workflowService.resolveEnvironmentId(workflow, executionArgs.getWorkflowVariables());
    envId = resolveEnvId != null ? resolveEnvId : envId;

    // Doing this check here so that workflow is already fetched from databae.
    preDeploymentChecks.checkIfWorkflowUsingRestrictedFeatures(workflow);
    PreDeploymentChecker deploymentFreezeChecker = new DeploymentFreezeChecker(
        governanceConfigService, new DeploymentCtx(appId, Collections.singletonList(envId)), environmentService);
    deploymentFreezeChecker.check(accountId);
    checkPreDeploymentConditions(accountId, appId);

    if (infraRefactor) {
      workflow.setOrchestrationWorkflow(workflowConcurrencyHelper.enhanceWithConcurrencySteps(
          workflow.getAppId(), workflow.getOrchestrationWorkflow()));
    }

    StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
        ((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph(),
        workflowService.stencilMap(appId), infraRefactor, false);

    // TODO: this is workaround for a side effect in the state machine generation that mangles with the original
    //       workflow object.
    workflow = workflowService.readWorkflow(appId, workflowId);

    stateMachine.setOrchestrationWorkflow(null);

    WorkflowExecution workflowExecution = workflowExecutionServiceHelper.obtainExecution(
        workflow, stateMachine, resolveEnvId, pipelineExecutionId, executionArgs, infraRefactor);

    validateExecutionArgsHosts(executionArgs.getHosts(), workflowExecution, workflow);
    validateWorkflowTypeAndService(workflow, executionArgs);

    WorkflowStandardParams stdParams =
        workflowExecutionServiceHelper.obtainWorkflowStandardParams(appId, envId, executionArgs, workflow);

    return triggerExecution(workflowExecution, stateMachine, new CanaryWorkflowExecutionAdvisor(),
        workflowExecutionUpdate, stdParams, trigger, null, workflow);
  }

  /*
  Rolling type workflow does not support k8s-v1 type of service
   */
  void validateWorkflowTypeAndService(Workflow workflow, ExecutionArgs executionArgs) {
    List<Service> services = workflowService.getResolvedServices(workflow, executionArgs.getWorkflowVariables());
    if (workflow.getOrchestrationWorkflow() != null
        && OrchestrationWorkflowType.ROLLING == workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType()) {
      for (Service service : emptyIfNull(services)) {
        if (service.getDeploymentType() == DeploymentType.KUBERNETES && !service.isK8sV2()) {
          throw new InvalidRequestException(format("Rolling Type Workflow does not suport k8s-v1 "
                  + "service [%s]",
              service.getName()));
        }
      }
    }
  }

  void validateExecutionArgsHosts(List<String> hosts, WorkflowExecution workflowExecution, Workflow workflow) {
    if (isEmpty(hosts)) {
      return;
    }

    List<String> serviceIds = workflowExecution.getServiceIds();
    if (isEmpty(serviceIds) || serviceIds.size() > 1) {
      throw new InvalidRequestException("Execution Hosts only supported for single service workflow", USER);
    }

    List<DeploymentType> deploymentTypes = workflow.getDeploymentTypes();
    if (isEmpty(deploymentTypes) || deploymentTypes.size() > 1) {
      throw new InvalidRequestException("Execution Hosts only supported for single deployment type", USER);
    }

    if (deploymentTypes.get(0) != DeploymentType.SSH) {
      throw new InvalidRequestException("Execution Hosts only supported for SSH deployment type", USER);
    }
  }

  private WorkflowExecution triggerExecution(WorkflowExecution workflowExecution, StateMachine stateMachine,
      WorkflowExecutionUpdate workflowExecutionUpdate, WorkflowStandardParams stdParams, Trigger trigger,
      Pipeline pipeline, Workflow workflow, ContextElement... contextElements) {
    return triggerExecution(workflowExecution, stateMachine, null, workflowExecutionUpdate, stdParams, trigger,
        pipeline, workflow, contextElements);
  }

  private WorkflowExecution triggerExecution(WorkflowExecution workflowExecution, StateMachine stateMachine,
      ExecutionEventAdvisor workflowExecutionAdvisor, WorkflowExecutionUpdate workflowExecutionUpdate,
      WorkflowStandardParams stdParams, Trigger trigger, Pipeline pipeline, Workflow workflow,
      ContextElement... contextElements) {
    Set<String> keywords = new HashSet<>();
    keywords.add(workflowExecution.normalizedName());
    if (workflowExecution.getWorkflowType() != null) {
      keywords.add(workflowExecution.getWorkflowType().name());
    }
    if (workflowExecution.getOrchestrationType() != null) {
      keywords.add(workflowExecution.getWorkflowType().name());
    }

    ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();

    Application app = appService.get(workflowExecution.getAppId());

    populateArtifactsAndServices(workflowExecution, stdParams, keywords, executionArgs, app.getAccountId());

    populatePipelineSummary(workflowExecution, keywords, executionArgs);

    workflowExecution.setAppName(app.getName());
    keywords.add(workflowExecution.getAppName());

    refreshEnvSummary(workflowExecution, keywords);

    populateCurrentUser(workflowExecution, stdParams, trigger, keywords, executionArgs);

    populateServiceInstances(workflowExecution, keywords, executionArgs);

    workflowExecution.setErrorStrategy(executionArgs.getErrorStrategy());

    workflowExecution.setKeywords(trimmedLowercaseSet(keywords));
    workflowExecution.setStatus(QUEUED);

    EntityVersion entityVersion = entityVersionService.newEntityVersion(workflowExecution.getAppId(), DEPLOYMENT,
        workflowExecution.getWorkflowId(), workflowExecution.displayName(), ChangeType.CREATED);

    workflowExecution.setReleaseNo(String.valueOf(entityVersion.getVersion()));
    workflowExecution.setAccountId(app.getAccountId());
    wingsPersistence.save(workflowExecution);

    logger.info("Created workflow execution {}", workflowExecution.getUuid());

    updateWorkflowElement(workflowExecution, stdParams, workflow, app.getAccountId());

    LinkedList<ContextElement> elements = new LinkedList<>();
    elements.push(stdParams);
    if (contextElements != null) {
      for (ContextElement contextElement : contextElements) {
        elements.push(contextElement);
      }
    }
    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(workflowExecution.getAppId());
    stateExecutionInstance.setExecutionName(workflowExecution.normalizedName());
    stateExecutionInstance.setExecutionUuid(workflowExecution.getUuid());
    stateExecutionInstance.setExecutionType(workflowExecution.getWorkflowType());
    stateExecutionInstance.setOrchestrationWorkflowType(workflowExecution.getOrchestrationType());
    stateExecutionInstance.setWorkflowId(workflowExecution.getWorkflowId());
    stateExecutionInstance.setPipelineStageElementId(executionArgs.getPipelinePhaseElementId());
    stateExecutionInstance.setPipelineStageParallelIndex(executionArgs.getPipelinePhaseParallelIndex());

    if (workflowExecutionUpdate == null) {
      workflowExecutionUpdate = new WorkflowExecutionUpdate();
    }
    workflowExecutionUpdate.setAppId(workflowExecution.getAppId());
    workflowExecutionUpdate.setWorkflowExecutionId(workflowExecution.getUuid());
    workflowExecutionUpdate.setNeedToNotifyPipeline(executionArgs.isTriggeredFromPipeline());

    stateExecutionInstance.setCallback(workflowExecutionUpdate);
    if (workflowExecutionAdvisor != null) {
      stateExecutionInstance.setExecutionEventAdvisors(asList(workflowExecutionAdvisor));
    }
    stateExecutionInstance.setContextElements(elements);
    stateExecutionInstance.setSubGraphFilterId("dummy");

    stateExecutionInstance = stateMachineExecutor.queue(stateMachine, stateExecutionInstance);

    WorkflowExecution savedWorkflowExecution;
    if (shouldNotQueueWorkflow(workflowExecution, workflow)) {
      stateMachineExecutor.startExecution(stateMachine, stateExecutionInstance);
      updateStartStatus(workflowExecution.getAppId(), workflowExecution.getUuid(), RUNNING);
      savedWorkflowExecution = wingsPersistence.getWithAppId(
          WorkflowExecution.class, workflowExecution.getAppId(), workflowExecution.getUuid());
      if (workflowExecution.getWorkflowType() == PIPELINE) {
        savePipelineSweepingOutPut(workflowExecution, pipeline, savedWorkflowExecution);
      }
    } else {
      // create queue event
      executionEventQueue.send(ExecutionEvent.builder()
                                   .appId(workflowExecution.getAppId())
                                   .workflowId(workflowExecution.getWorkflowId())
                                   .infraMappingIds(workflowExecution.getInfraMappingIds())
                                   .infraDefinitionIds(workflowExecution.getInfraDefinitionIds())
                                   .build());
      savedWorkflowExecution = wingsPersistence.getWithAppId(
          WorkflowExecution.class, workflowExecution.getAppId(), workflowExecution.getUuid());
    }

    return savedWorkflowExecution;
  }

  private boolean shouldNotQueueWorkflow(WorkflowExecution workflowExecution, Workflow workflow) {
    if (workflowExecution.getWorkflowType() != ORCHESTRATION || BUILD == workflowExecution.getOrchestrationType()) {
      return true;
    } else if (featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, workflow.getAccountId())) {
      CanaryOrchestrationWorkflow orchestrationWorkflow =
          (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
      return orchestrationWorkflow.getConcurrencyStrategy() != null
          && orchestrationWorkflow.getConcurrencyStrategy().isEnabled();
    } else {
      return false;
    }
  }

  private void savePipelineSweepingOutPut(
      WorkflowExecution workflowExecution, Pipeline pipeline, WorkflowExecution savedWorkflowExecution) {
    PipelineElement pipelineElement = PipelineElement.builder()
                                          .displayName(workflowExecution.displayName())
                                          .name(pipeline.getName())
                                          .description(pipeline.getDescription())
                                          .startTs(savedWorkflowExecution.getStartTs())
                                          .build();
    sweepingOutputService.save(SweepingOutputServiceImpl
                                   .prepareSweepingOutputBuilder(workflowExecution.getAppId(),
                                       workflowExecution.getUuid(), null, null, null, Scope.PIPELINE)
                                   .name("pipeline")
                                   .output(KryoUtils.asDeflatedBytes(pipelineElement))
                                   .build());
  }

  private void updateWorkflowElement(
      WorkflowExecution workflowExecution, WorkflowStandardParams stdParams, Workflow workflow, String accountId) {
    stdParams.setErrorStrategy(workflowExecution.getErrorStrategy());
    String workflowUrl = mainConfiguration.getPortal().getUrl() + "/"
        + format(mainConfiguration.getPortal().getExecutionUrlPattern(), workflowExecution.getAppId(),
              workflowExecution.getEnvId(), workflowExecution.getUuid());

    if (stdParams.getWorkflowElement() == null) {
      WorkflowElementBuilder workflowElementBuilder = WorkflowElement.builder()
                                                          .uuid(workflowExecution.getUuid())
                                                          .name(workflowExecution.normalizedName())
                                                          .url(workflowUrl)
                                                          .displayName(workflowExecution.displayName())
                                                          .releaseNo(workflowExecution.getReleaseNo());
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        workflowElementBuilder.artifactVariables(workflowExecution.getExecutionArgs().getArtifactVariables());
      }
      stdParams.setWorkflowElement(workflowElementBuilder.build());
    } else {
      stdParams.getWorkflowElement().setName(workflowExecution.normalizedName());
      stdParams.getWorkflowElement().setUuid(workflowExecution.getUuid());
      stdParams.getWorkflowElement().setUrl(workflowUrl);
      stdParams.getWorkflowElement().setDisplayName(workflowExecution.displayName());
      stdParams.getWorkflowElement().setReleaseNo(workflowExecution.getReleaseNo());
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        stdParams.getWorkflowElement().setArtifactVariables(
            workflowExecution.getExecutionArgs().getArtifactVariables());
      }
    }
    stdParams.getWorkflowElement().setPipelineDeploymentUuid(workflowExecution.getWorkflowType() == PIPELINE
            ? workflowExecution.getUuid()
            : workflowExecution.getPipelineExecutionId());
    lastGoodReleaseInfo(stdParams.getWorkflowElement(), workflowExecution);
    stdParams.getWorkflowElement().setDescription(workflow != null ? workflow.getDescription() : null);
  }

  private void populateServiceInstances(
      WorkflowExecution workflowExecution, Set<String> keywords, ExecutionArgs executionArgs) {
    if (executionArgs.getServiceInstances() != null) {
      List<String> serviceInstanceIds =
          executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(toList());
      PageRequest<ServiceInstance> pageRequest = aPageRequest()
                                                     .addFilter("appId", EQ, workflowExecution.getAppId())
                                                     .addFilter("uuid", IN, serviceInstanceIds.toArray())
                                                     .build();
      List<ServiceInstance> serviceInstances = serviceInstanceService.list(pageRequest).getResponse();

      if (serviceInstances == null || serviceInstances.size() != serviceInstanceIds.size()) {
        logger.error("Service instances argument and valid service instance retrieved size not matching");
        throw new InvalidRequestException("Invalid service instances");
      }
      executionArgs.setServiceInstanceIdNames(serviceInstances.stream().collect(toMap(ServiceInstance::getUuid,
          serviceInstance -> serviceInstance.getHostName() + ":" + serviceInstance.getServiceName())));

      keywords.addAll(serviceInstances.stream().map(ServiceInstance::getHostName).collect(toList()));
      keywords.addAll(serviceInstances.stream().map(ServiceInstance::getServiceName).collect(toList()));
    }
  }

  private void populateCurrentUser(WorkflowExecution workflowExecution, WorkflowStandardParams stdParams,
      Trigger trigger, Set<String> keywords, ExecutionArgs executionArgs) {
    User user = UserThreadLocal.get();
    if (user != null) {
      EmbeddedUser triggeredBy =
          EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
      workflowExecution.setTriggeredBy(triggeredBy);
      workflowExecution.setCreatedBy(triggeredBy);
    } else if (trigger != null) {
      // Triggered by Auto Trigger
      workflowExecution.setTriggeredBy(
          EmbeddedUser.builder().name(trigger.getName() + " (Deployment Trigger)").build());
      workflowExecution.setCreatedBy(EmbeddedUser.builder().name(trigger.getName() + " (Deployment Trigger)").build());
      workflowExecution.setDeploymentTriggerId(trigger.getUuid());
    } else if (executionArgs.getTriggerExecutionArgs() != null) {
      workflowExecution.setTriggeredBy(
          EmbeddedUser.builder()
              .name(executionArgs.getTriggerExecutionArgs().getTriggerName() + " (Deployment Trigger)")
              .build());
      workflowExecution.setCreatedBy(
          EmbeddedUser.builder()
              .name(executionArgs.getTriggerExecutionArgs().getTriggerName() + " (Deployment Trigger)")
              .build());
      workflowExecution.setDeploymentTriggerId(executionArgs.getTriggerExecutionArgs().getTriggerUuid());
    }
    if (workflowExecution.getCreatedBy() != null) {
      keywords.add(workflowExecution.getCreatedBy().getName());
      keywords.add(workflowExecution.getCreatedBy().getEmail());
    }
    stdParams.setCurrentUser(workflowExecution.getCreatedBy());
  }

  private void refreshEnvSummary(WorkflowExecution workflowExecution, Set<String> keywords) {
    if (isNotEmpty(workflowExecution.getEnvIds())) {
      List<EnvSummary> environmentSummaries =
          environmentService.obtainEnvironmentSummaries(workflowExecution.getAppId(), workflowExecution.getEnvIds());
      if (isNotEmpty(environmentSummaries)) {
        for (EnvSummary envSummary : environmentSummaries) {
          workflowExecution.setEnvironments(environmentSummaries);
          if (envSummary.getUuid().equals(workflowExecution.getEnvId())) {
            workflowExecution.setEnvName(envSummary.getName());
            workflowExecution.setEnvType(envSummary.getEnvironmentType());
          }
          if (workflowExecution.getEnvType() != null) {
            keywords.add(workflowExecution.getEnvType().name());
          }
          keywords.add(workflowExecution.getEnvName());
        }
      }
    }
  }

  private void populatePipelineSummary(
      WorkflowExecution workflowExecution, Set<String> keywords, ExecutionArgs executionArgs) {
    if (executionArgs.isTriggeredFromPipeline()) {
      if (executionArgs.getPipelineId() != null) {
        Pipeline pipeline =
            wingsPersistence.getWithAppId(Pipeline.class, workflowExecution.getAppId(), executionArgs.getPipelineId());
        workflowExecution.setPipelineSummary(
            PipelineSummary.builder().pipelineId(pipeline.getUuid()).pipelineName(pipeline.getName()).build());
        keywords.add(pipeline.getName());
      }
      if (workflowExecution.getPipelineExecutionId() != null) {
        WorkflowExecution pipelineExecution =
            wingsPersistence.createQuery(WorkflowExecution.class)
                .project(WorkflowExecutionKeys.triggeredBy, true)
                .project(WorkflowExecutionKeys.createdBy, true)
                .project(WorkflowExecutionKeys.deploymentTriggerId, true)
                .project(WorkflowExecutionKeys.artifacts, true)
                .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
                .filter(WorkflowExecutionKeys.uuid, workflowExecution.getPipelineExecutionId())
                .get();
        if (pipelineExecution != null) {
          workflowExecution.setTriggeredBy(pipelineExecution.getTriggeredBy());
          workflowExecution.setDeploymentTriggerId(pipelineExecution.getDeploymentTriggerId());
          workflowExecution.setCreatedBy(pipelineExecution.getCreatedBy());
        }
      }
    }
  }

  public void populateArtifactsAndServices(WorkflowExecution workflowExecution, WorkflowStandardParams stdParams,
      Set<String> keywords, ExecutionArgs executionArgs, String accountId) {
    if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
      populateArtifacts(workflowExecution, stdParams, keywords, executionArgs, accountId);
      return;
    }

    if (isEmpty(executionArgs.getArtifacts())) {
      return;
    }

    List<String> artifactIds = executionArgs.getArtifacts()
                                   .stream()
                                   .map(Artifact::getUuid)
                                   .filter(Objects::nonNull)
                                   .distinct()
                                   .collect(toList());
    if (isEmpty(artifactIds)) {
      return;
    }

    List<Artifact> artifacts =
        artifactService.listByIds(appService.getAccountIdByAppId(workflowExecution.getAppId()), artifactIds);
    if (artifacts == null || artifacts.size() != artifactIds.size()) {
      logger.error("artifactIds from executionArgs contains invalid artifacts");
      throw new InvalidRequestException("Invalid artifact");
    }

    // Filter out the artifacts that do not belong to the workflow.
    List<String> serviceIds =
        isEmpty(workflowExecution.getServiceIds()) ? new ArrayList<>() : workflowExecution.getServiceIds();
    Set<String> artifactStreamIds = new HashSet<>();
    serviceIds.forEach(serviceId -> {
      List<String> ids = artifactStreamServiceBindingService.listArtifactStreamIds(serviceId);
      if (isNotEmpty(ids)) {
        artifactStreamIds.addAll(ids);
      }
    });

    List<Artifact> filteredArtifacts = new ArrayList<>();
    for (Artifact artifact : artifacts) {
      if (artifactStreamIds.contains(artifact.getArtifactStreamId())) {
        filteredArtifacts.add(artifact);
      } else {
        logger.warn(
            "Artifact stream: [{}] is not available in services: {}", artifact.getArtifactStreamId(), serviceIds);
      }
    }

    executionArgs.setArtifactIdNames(
        filteredArtifacts.stream().collect(toMap(Artifact::getUuid, Artifact::getDisplayName)));
    filteredArtifacts.forEach(artifact -> {
      artifact.setArtifactFiles(null);
      artifact.setCreatedBy(null);
      artifact.setLastUpdatedBy(null);
      keywords.add(artifact.getArtifactSourceName());
      keywords.add(artifact.getDescription());
      keywords.add(artifact.getRevision());
      keywords.addAll(artifact.getMetadata().values());
    });

    executionArgs.setArtifacts(artifacts);
    workflowExecution.setArtifacts(filteredArtifacts);

    Set<String> serviceIdsSet = new HashSet<>();
    List<ServiceElement> services = new ArrayList<>();
    filteredArtifacts.forEach(artifact -> {
      List<Service> relatedServices = artifactStreamServiceBindingService.listServices(artifact.getArtifactStreamId());
      if (isEmpty(relatedServices)) {
        return;
      }

      for (Service relatedService : relatedServices) {
        if (serviceIdsSet.contains(relatedService.getUuid())) {
          continue;
        }

        serviceIdsSet.add(relatedService.getUuid());
        ServiceElement se = ServiceElement.builder().build();
        MapperUtils.mapObject(relatedService, se);
        services.add(se);
        keywords.add(se.getName());
      }
    });

    // Set the services in the context.
    stdParams.setServices(services);
  }

  private void populateArtifacts(WorkflowExecution workflowExecution, WorkflowStandardParams stdParams,
      Set<String> keywords, ExecutionArgs executionArgs, String accountId) {
    List<ArtifactVariable> artifactVariables = executionArgs.getArtifactVariables();
    List<Artifact> filteredArtifacts = multiArtifactWorkflowExecutionServiceHelper.filterArtifactsForExecution(
        artifactVariables, workflowExecution, accountId);

    removeDuplicates(filteredArtifacts);
    executionArgs.setArtifacts(filteredArtifacts);
    workflowExecution.setArtifacts(filteredArtifacts);
    stdParams.setArtifactIds(filteredArtifacts.stream().map(Artifact::getUuid).collect(toList()));

    List<String> serviceIds =
        isEmpty(workflowExecution.getServiceIds()) ? new ArrayList<>() : workflowExecution.getServiceIds();
    if (isNotEmpty(filteredArtifacts)) {
      executionArgs.setArtifactIdNames(
          filteredArtifacts.stream().collect(toMap(Artifact::getUuid, Artifact::getDisplayName)));
      filteredArtifacts.forEach(artifact -> {
        artifact.setArtifactFiles(null);
        artifact.setCreatedBy(null);
        artifact.setLastUpdatedBy(null);
        artifact.setServiceIds(artifactStreamServiceBindingService.listServiceIds(artifact.getArtifactStreamId()));

        Map<String, String> source =
            artifactStreamService.fetchArtifactSourceProperties(accountId, artifact.getArtifactStreamId());
        if (!source.containsKey(ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY)
            || ARTIFACT_SOURCE_DOCKER_CONFIG_PLACEHOLDER.equals(source.get(ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY))) {
          try {
            String dockerConfig = artifactCollectionUtils.getDockerConfig(artifact.getArtifactStreamId());
            source.put(ARTIFACT_SOURCE_DOCKER_CONFIG_NAME_KEY, dockerConfig);
          } catch (InvalidRequestException e) {
            // Artifact stream type doesn't have docker credentials. Ex. Jenkins
            // Ignore exception.
          }
        }
        artifact.setSource(source);

        keywords.add(artifact.getArtifactSourceName());
        keywords.add(artifact.getDescription());
        keywords.add(artifact.getRevision());
        keywords.add(artifact.getBuildNo());
      });
    }

    Set<String> serviceIdsSet = new HashSet<>();
    List<ServiceElement> services = new ArrayList<>();
    if (isNotEmpty(serviceIds)) {
      for (String serviceId : serviceIds) {
        if (serviceIdsSet.contains(serviceId)) {
          continue;
        }
        serviceIdsSet.add(serviceId);
        Service service = serviceResourceService.get(serviceId);
        ServiceElement se = ServiceElement.builder().build();
        MapperUtils.mapObject(service, se);
        services.add(se);
        keywords.add(se.getName());
      }
    }

    // Set the services in the context.
    stdParams.setServices(services);
  }

  private static void removeDuplicates(List<Artifact> artifacts) {
    if (isEmpty(artifacts)) {
      return;
    }

    Map<String, Artifact> map = new LinkedHashMap<>();
    for (Artifact artifact : artifacts) {
      map.put(artifact.getUuid(), artifact);
    }

    artifacts.clear();
    artifacts.addAll(map.values());
  }

  private void lastGoodReleaseInfo(WorkflowElement workflowElement, WorkflowExecution workflowExecution) {
    WorkflowExecution workflowExecutionLast = fetchLastSuccessDeployment(workflowExecution);
    if (workflowExecutionLast != null) {
      workflowElement.setLastGoodDeploymentDisplayName(workflowExecutionLast.displayName());
      workflowElement.setLastGoodDeploymentUuid(workflowExecutionLast.getUuid());
      workflowElement.setLastGoodReleaseNo(workflowExecutionLast.getReleaseNo());
    }
  }

  @Override
  public void updateWorkflowElementWithLastGoodReleaseInfo(
      String appId, WorkflowElement workflowElement, String workflowExecutionId) {
    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    lastGoodReleaseInfo(workflowElement, workflowExecution);
  }
  @Override
  public void updateStartStatus(String appId, String workflowExecutionId, ExecutionStatus status) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecutionKeys.appId, appId)
                                         .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                         .field(WorkflowExecutionKeys.status)
                                         .in(asList(NEW, QUEUED));

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set(WorkflowExecutionKeys.status, status)
                                                        .set(WorkflowExecutionKeys.startTs, System.currentTimeMillis());

    wingsPersistence.update(query, updateOps);
  }

  @Override
  public WorkflowExecution triggerEnvExecution(
      String appId, String envId, ExecutionArgs executionArgs, Trigger trigger) {
    String accountId = appService.getAccountIdByAppId(appId);

    validateExecutionArgsHosts(executionArgs, trigger);
    executionArgs.setHosts(trimExecutionArgsHosts(executionArgs.getHosts()));

    try {
      WorkflowExecution execution = triggerEnvExecution(appId, envId, executionArgs, null, trigger);
      alertService.closeAlertsOfType(accountId, appId, AlertType.USAGE_LIMIT_EXCEEDED);
      return execution;
    } catch (UsageLimitExceededException e) {
      String errMsg =
          "Deployment rate limit reached. Some deployments may not be allowed. Please contact Harness support.";
      logger.info("Message: {}, accountId={}", e.getMessage(), accountId);

      // open alert for triggers
      if (executionArgs.getWorkflowType() == WorkflowType.ORCHESTRATION && null != trigger) {
        alertService.openAlert(accountId, appId, AlertType.USAGE_LIMIT_EXCEEDED,
            new UsageLimitExceededAlert(e.getAccountId(), e.getLimit(), errMsg));
      }

      throw new WingsException(ErrorCode.USAGE_LIMITS_EXCEEDED, errMsg, USER);
    }
  }

  List<String> trimExecutionArgsHosts(List<String> hosts) {
    if (isEmpty(hosts)) {
      return Collections.emptyList();
    }
    return hosts.stream().map(StringUtils::trim).collect(Collectors.toList());
  }

  void validateExecutionArgsHosts(ExecutionArgs executionArgs, Trigger trigger) {
    if (!executionArgs.isTargetToSpecificHosts()) {
      // Hack as later validations are on empty host list, not on target flag
      executionArgs.setHosts(Collections.emptyList());
      return;
    }
    if (isEmpty(executionArgs.getHosts())) {
      throw new InvalidRequestException(
          "Host list can't be empty when Target To Specific Hosts option is enabled", USER);
    }

    if (executionArgs.getWorkflowType() == WorkflowType.PIPELINE) {
      throw new InvalidRequestException("Hosts can't be overridden for pipeline", USER);
    }

    if (trigger != null) {
      throw new InvalidRequestException("Hosts can't be overridden for triggers", USER);
    }
  }

  @Override
  public WorkflowExecution triggerRollbackExecutionWorkflow(String appId, WorkflowExecution workflowExecution) {
    try (AutoLogContext ignore1 = new AccountLogContext(workflowExecution.getAccountId(), OVERRIDE_ERROR)) {
      authorizeOnDemandRollback(appId, workflowExecution);

      if (!getOnDemandRollbackAvailable(appId, workflowExecution)) {
        throw new InvalidRequestException("On demand rollback should not be available for this execution");
      }

      WorkflowExecution activeWorkflowExecution = getRunningExecutions(workflowExecution);
      if (activeWorkflowExecution != null) {
        throw new InvalidRequestException("Cannot trigger Rollback, active execution found");
      }

      List<Artifact> previousArtifacts = validateAndGetPreviousArtifacts(workflowExecution);
      if (isEmpty(previousArtifacts)) {
        throw new InvalidRequestException("No previous artifact found to rollback to");
      }

      ExecutionArgs oldExecutionArgs = workflowExecution.getExecutionArgs();
      oldExecutionArgs.setArtifacts(previousArtifacts);
      oldExecutionArgs.setArtifactVariables(null);
      return triggerRollbackExecution(appId, workflowExecution.getEnvId(), oldExecutionArgs, workflowExecution);
    }
  }

  private void authorizeOnDemandRollback(String appId, WorkflowExecution workflowExecution) {
    String workflowId = workflowExecution.getWorkflowId();
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.DEPLOYMENT, Action.EXECUTE);
    List<PermissionAttribute> permissionAttributeList = Collections.singletonList(permissionAttribute);
    authHandler.authorize(permissionAttributeList, Collections.singletonList(appId), workflowId);
    authService.checkIfUserAllowedToDeployToEnv(appId, workflowExecution.getEnvId());
  }

  @Override
  public RollbackConfirmation getOnDemandRollbackConfirmation(String appId, WorkflowExecution workflowExecution) {
    String accountId = workflowExecution.getAccountId();
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      authorizeOnDemandRollback(appId, workflowExecution);

      if (alreadyRolledBack(workflowExecution)) {
        throw new InvalidRequestException("Rollback Execution is not available as already Rolled back");
      }

      if (!getOnDemandRollbackAvailable(appId, workflowExecution)) {
        throw new InvalidRequestException("On demand rollback should not be available for this execution");
      }

      if (!checkIfUsingSweepingOutputs(appId, workflowExecution)) {
        throw new InvalidRequestException("Rollback is not available for older deployment");
      }

      if (isEmpty(workflowExecution.getServiceIds()) || workflowExecution.getServiceIds().size() != 1) {
        throw new InvalidRequestException("Rollback Execution is not available for multi Service workflow");
      }

      if (workflowExecution.isOnDemandRollback()) {
        return RollbackConfirmation.builder()
            .valid(false)
            .validationMessage("Cannot trigger Rollback for RolledBack execution")
            .build();
      }

      String workflowId = workflowExecution.getWorkflowId();
      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheck("Error reading workflow associated with this workflowExecution. Might be deleted", workflow);
      notNullCheck("Error reading workflow associated with this workflowExecution. Might be deleted",
          workflow.getOrchestrationWorkflow());

      WorkflowExecution activeWorkflowExecution = getRunningExecutions(workflowExecution);
      if (activeWorkflowExecution != null) {
        return RollbackConfirmation.builder()
            .valid(false)
            .validationMessage("Cannot trigger Rollback, active execution found")
            .activeWorkflowExecution(activeWorkflowExecution)
            .workflowId(workflowId)
            .build();
      }

      List<Artifact> previousArtifacts = validateAndGetPreviousArtifacts(workflowExecution);
      if (isEmpty(previousArtifacts)) {
        throw new InvalidRequestException("No artifact found in previous execution");
      }

      return RollbackConfirmation.builder().artifacts(previousArtifacts).workflowId(workflowId).valid(true).build();
    }
  }

  private boolean checkIfUsingSweepingOutputs(String appId, WorkflowExecution workflowExecution) {
    List<String> infraDefId = workflowExecution.getInfraDefinitionIds();
    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefId.get(0));
    if (infrastructureDefinition.getDeploymentType() == DeploymentType.SSH
        || infrastructureDefinition.getDeploymentType() == DeploymentType.WINRM) {
      return workflowExecution.isUseSweepingOutputs();
    } else {
      return true;
    }
  }

  private boolean alreadyRolledBack(WorkflowExecution workflowExecution) {
    WorkflowExecution execution = wingsPersistence.createQuery(WorkflowExecution.class)
                                      .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
                                      .filter(WorkflowExecutionKeys.status, SUCCESS)
                                      .filter(WorkflowExecutionKeys.onDemandRollback, true)
                                      .filter("originalExecution.executionId", workflowExecution.getUuid())
                                      .get();
    return execution != null;
  }

  private WorkflowExecution getRunningExecutions(WorkflowExecution workflowExecution) {
    final Query<WorkflowExecution> query =
        wingsPersistence.createQuery(WorkflowExecution.class)
            .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
            .filter(WorkflowExecutionKeys.infraMappingIds, workflowExecution.getInfraMappingIds())
            .field(WorkflowExecutionKeys.status)
            .in(ExecutionStatus.activeStatuses());

    return query.get();
  }

  private List<Artifact> validateAndGetPreviousArtifacts(WorkflowExecution workflowExecution) {
    final Query<WorkflowExecution> query =
        wingsPersistence.createQuery(WorkflowExecution.class)
            .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
            .filter(WorkflowExecutionKeys.status, SUCCESS)
            .filter(WorkflowExecutionKeys.infraMappingIds, workflowExecution.getInfraMappingIds())
            .order(Sort.descending(WorkflowExecutionKeys.createdAt));
    final List<WorkflowExecution> workflowExecutionList = query.asList(new FindOptions().limit(2));

    if (isEmpty(workflowExecutionList)) {
      throw new InvalidRequestException(
          "Not able to find previous successful workflowExecutions for workflowExecution: "
          + workflowExecution.getName());
    }

    if (!workflowExecutionList.get(0).getUuid().equals(workflowExecution.getUuid())) {
      logger.info("Last successful execution found: {} ", workflowExecutionList.get(0));
      throw new InvalidRequestException(
          "This is not the latest successful workflowExecution: " + workflowExecution.getName());
    }

    if (workflowExecutionList.size() < 2) {
      throw new InvalidRequestException(
          "No previous execution before this execution to rollback to, workflowExecution: "
          + workflowExecution.getName());
    }

    WorkflowExecution lastSecondSuccessfulWE = workflowExecutionList.get(1);
    logger.info("Fetching artifact from execution: {}", lastSecondSuccessfulWE);
    return lastSecondSuccessfulWE.getArtifacts();
  }

  @Override
  public void incrementInProgressCount(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.inprogress", inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter(WorkflowExecutionKeys.appId, appId)
                                .filter(ID_KEY, workflowExecutionId),
        ops);
  }

  @Override
  public void incrementSuccess(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.success", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter(WorkflowExecutionKeys.appId, appId)
                                .filter(ID_KEY, workflowExecutionId),
        ops);
  }

  @Override
  public void incrementFailed(String appId, String workflowExecutionId, Integer inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.failed", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter(WorkflowExecutionKeys.appId, appId)
                                .filter(ID_KEY, workflowExecutionId),
        ops);
  }

  /**
   * Trigger env execution workflow execution.
   *
   * @param appId                   the app id
   * @param envId                   the env id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update
   * @return the workflow execution
   */
  WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs,
      WorkflowExecutionUpdate workflowExecutionUpdate, Trigger trigger) {
    String accountId = this.appService.getAccountIdByAppId(appId);
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Execution Triggered. Type: {}", executionArgs.getWorkflowType());
    }

    checkDeploymentRateLimit(accountId, appId);

    if (isEmpty(executionArgs.getArtifacts()) && isNotEmpty(executionArgs.getArtifactVariables())) {
      // Artifact Variables are passed but not artifacts. For backwards compatibility, set artifacts in executionArgs.
      List<Artifact> artifacts = executionArgs.getArtifactVariables()
                                     .stream()
                                     .map(ArtifactVariable::getValue)
                                     .filter(Objects::nonNull)
                                     .distinct()
                                     .map(artifactId -> Artifact.Builder.anArtifact().withUuid(artifactId).build())
                                     .collect(toList());
      executionArgs.setArtifacts(artifacts);
    }

    switch (executionArgs.getWorkflowType()) {
      case PIPELINE: {
        logger.debug("Received an pipeline execution request");
        if (executionArgs.getPipelineId() == null) {
          logger.error("pipelineId is null for an pipeline execution");
          throw new InvalidRequestException("pipelineId is null for an pipeline execution");
        }
        return triggerPipelineExecution(appId, executionArgs.getPipelineId(), executionArgs, trigger);
      }

      case ORCHESTRATION: {
        logger.debug("Received an orchestrated execution request");
        if (executionArgs.getOrchestrationId() == null) {
          logger.error("workflowId is null for an orchestrated execution");
          throw new InvalidRequestException("workflowId is null for an orchestrated execution");
        }
        return triggerOrchestrationExecution(appId, envId, executionArgs.getOrchestrationId(), executionArgs, trigger);
      }

      default:
        throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "workflowType");
    }
  }

  WorkflowExecution triggerRollbackExecution(
      String appId, String envId, ExecutionArgs executionArgs, WorkflowExecution previousWorkflowExecution) {
    String accountId = appService.getAccountIdByAppId(appId);
    if (PIPELINE == executionArgs.getWorkflowType()) {
      throw new InvalidRequestException("Emergency rollback not supported for pipelines");
    }

    logger.debug("Received an emergency rollback  execution request");
    if (executionArgs.getOrchestrationId() == null) {
      logger.error("workflowId is null for an orchestrated execution");
      throw new InvalidRequestException("workflowId is null for an orchestrated execution");
    }
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, accountId);

    logger.info("Execution Triggered. Type: {}, accountId={}", executionArgs.getWorkflowType(), accountId);

    String workflowId = executionArgs.getOrchestrationId();
    Workflow workflow = workflowExecutionServiceHelper.obtainWorkflow(appId, workflowId, infraRefactor);

    // Doing this check here so that workflow is already fetched from database.
    preDeploymentChecks.checkIfWorkflowUsingRestrictedFeatures(workflow);

    PreDeploymentChecker deploymentFreezeChecker = new DeploymentFreezeChecker(
        governanceConfigService, new DeploymentCtx(appId, Collections.singletonList(envId)), environmentService);
    deploymentFreezeChecker.check(accountId);

    // Not including instance limit and deployment limit check as it is a emergency rollback
    accountExpirationChecker.check(accountId);
    if (infraRefactor) {
      workflow.setOrchestrationWorkflow(workflowConcurrencyHelper.enhanceWithConcurrencySteps(
          workflow.getAppId(), workflow.getOrchestrationWorkflow()));
    }

    StateMachine stateMachine = rollbackStateMachineGenerator.generateForRollbackExecution(
        appId, previousWorkflowExecution.getUuid(), infraRefactor);

    // This is workaround for a side effect in the state machine generation that mangles with the original
    //       workflow object.
    workflow = workflowService.readWorkflow(appId, workflowId);

    stateMachine.setOrchestrationWorkflow(null);

    WorkflowExecution workflowExecution = workflowExecutionServiceHelper.obtainExecution(
        workflow, stateMachine, envId, null, executionArgs, infraRefactor);
    workflowExecution.setOnDemandRollback(true);
    workflowExecution.setOriginalExecution(WorkflowExecutionInfo.builder()
                                               .name(previousWorkflowExecution.getName())
                                               .startTs(previousWorkflowExecution.getStartTs())
                                               .executionId(previousWorkflowExecution.getUuid())
                                               .build());

    WorkflowStandardParams stdParams =
        workflowExecutionServiceHelper.obtainWorkflowStandardParams(appId, envId, executionArgs, workflow);

    return triggerExecution(
        workflowExecution, stateMachine, new CanaryWorkflowExecutionAdvisor(), null, stdParams, null, null, workflow);
  }

  private void checkDeploymentRateLimit(String accountId, String appId) {
    try {
      deployLimitChecker.check(accountId);
      alertService.closeAlertsOfType(accountId, GLOBAL_APP_ID, AlertType.DEPLOYMENT_RATE_APPROACHING_LIMIT);
    } catch (LimitApproachingException e) {
      String errMsg = e.getPercent()
          + "% of Deployment Rate Limit reached. Some deployments may not be allowed beyond 100% usage. Please contact Harness support.";
      logger.info("Approaching Limit Message: {}", e.getMessage());
      AlertData alertData = new DeploymentRateApproachingLimitAlert(e.getLimit(), accountId, e.getPercent(), errMsg);
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.DEPLOYMENT_RATE_APPROACHING_LIMIT, alertData);
    } catch (UsageLimitExceededException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error checking deployment rate limit. accountId={}", accountId, e);
    }
  }

  private void checkInstanceUsageLimit(String accountId, String appId) {
    try {
      siUsageChecker.check(accountId);
    } catch (InstanceUsageExceededLimitException e) {
      throw new WingsException(ErrorCode.USAGE_LIMITS_EXCEEDED,
          "You have reached your service instance limits. Deployments will be blocked.", USER);
    } catch (Exception e) {
      logger.error("Error while checking SI usage limit. accountId={}", accountId, e);
    }
  }

  private List<WorkflowExecution> getRunningWorkflowExecutions(
      WorkflowType workflowType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("workflowId", EQ, workflowId)
                                                     .addFilter("workflowType", EQ, workflowType)
                                                     .addFilter("status", IN, NEW, QUEUED, RUNNING, PAUSED)
                                                     .build();

    PageResponse<WorkflowExecution> pageResponse = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (pageResponse == null) {
      return null;
    }
    return pageResponse.getResponse();
  }

  @Override
  public ExecutionInterrupt triggerExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    String executionUuid = executionInterrupt.getExecutionUuid();
    WorkflowExecution workflowExecution =
        wingsPersistence.getWithAppId(WorkflowExecution.class, executionInterrupt.getAppId(), executionUuid);
    if (workflowExecution == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "No WorkflowExecution for executionUuid:" + executionUuid);
    }

    if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
      // There is a race between the workflow progress and request coming from the user.
      // It is completely normal the workflow to finish while interrupt request is coming.
      // Therefore there is nothing alarming when this occurs.
      throw new InvalidRequestException("Workflow execution already completed. executionUuid:" + executionUuid, USER);
    }

    if (workflowExecution.getWorkflowType() != PIPELINE) {
      executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      return executionInterrupt;
    }

    if (!(executionInterrupt.getExecutionInterruptType() == PAUSE_ALL
            || executionInterrupt.getExecutionInterruptType() == RESUME_ALL
            || executionInterrupt.getExecutionInterruptType() == ABORT_ALL)) {
      throw new InvalidRequestException("Invalid ExecutionInterrupt: " + executionInterrupt);
    }

    try {
      executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (RuntimeException exception) {
      logger.error("Error in interrupting workflowExecution - uuid: {}, executionInterruptType: {}",
          workflowExecution.getUuid(), executionInterrupt.getExecutionInterruptType(), exception);
    }

    List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(workflowExecution);
    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
      StateExecutionData stateExecutionData = stateExecutionInstance.fetchStateExecutionData();
      if (!(stateExecutionData instanceof EnvStateExecutionData)) {
        continue;
      }
      EnvStateExecutionData envStateExecutionData = (EnvStateExecutionData) stateExecutionData;
      WorkflowExecution workflowExecution2 =
          getWorkflowExecution(workflowExecution.getAppId(), envStateExecutionData.getWorkflowExecutionId());

      if (workflowExecution2 == null
          || (workflowExecution2.getStatus() != null
                 && ExecutionStatus.isFinalStatus(workflowExecution2.getStatus()))) {
        continue;
      }

      try {
        ExecutionInterrupt executionInterruptClone = KryoUtils.clone(executionInterrupt);
        executionInterruptClone.setUuid(generateUuid());
        executionInterruptClone.setExecutionUuid(workflowExecution2.getUuid());
        executionInterruptManager.registerExecutionInterrupt(executionInterruptClone);
      } catch (WingsException exception) {
        ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
      } catch (RuntimeException exception) {
        logger.error("Error in interrupting workflowExecution - uuid: {}, executionInterruptType: {}",
            workflowExecution.getUuid(), executionInterrupt.getExecutionInterruptType(), exception);
      }
    }

    return executionInterrupt;
  }

  @Override
  public RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs) {
    notNullCheck("workflowType", executionArgs.getWorkflowType());

    if (executionArgs.getWorkflowType() == ORCHESTRATION) {
      logger.debug("Received an orchestrated execution request");
      notNullCheck("orchestrationId", executionArgs.getOrchestrationId());

      Workflow workflow = workflowService.readWorkflow(appId, executionArgs.getOrchestrationId());
      if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
        throw new InvalidRequestException("OrchestrationWorkflow not found");
      }

      StateMachine stateMachine =
          workflowService.readStateMachine(appId, executionArgs.getOrchestrationId(), workflow.getDefaultVersion());
      if (stateMachine == null) {
        throw new InvalidRequestException("Associated state machine not found");
      }

      RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
      requiredExecutionArgs.setEntityTypes(workflow.getOrchestrationWorkflow().getRequiredEntityTypes());
      return requiredExecutionArgs;
    }

    return null;
  }

  @Override
  public WorkflowVariablesMetadata fetchWorkflowVariables(
      String appId, ExecutionArgs executionArgs, String workflowExecutionId) {
    return workflowExecutionServiceHelper.fetchWorkflowVariables(appId, executionArgs, workflowExecutionId);
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(
      String appId, ExecutionArgs executionArgs, boolean withDefaultArtifact, String workflowExecutionId) {
    notNullCheck("Workflow type is required", executionArgs.getWorkflowType());
    WorkflowExecution workflowExecution = null;
    if (withDefaultArtifact && workflowExecutionId != null) {
      workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    }

    DeploymentMetadata finalDeploymentMetadata;
    if (executionArgs.getWorkflowType() == ORCHESTRATION) {
      Workflow workflow = workflowService.readWorkflow(appId, executionArgs.getOrchestrationId());
      finalDeploymentMetadata = workflowService.fetchDeploymentMetadata(
          appId, workflow, executionArgs.getWorkflowVariables(), null, null, withDefaultArtifact, workflowExecution);
    } else {
      finalDeploymentMetadata = pipelineService.fetchDeploymentMetadata(appId, executionArgs.getPipelineId(),
          executionArgs.getWorkflowVariables(), null, null, withDefaultArtifact, workflowExecution);
    }

    if (finalDeploymentMetadata != null) {
      // Set environments.
      finalDeploymentMetadata.setEnvSummaries(
          environmentService.obtainEnvironmentSummaries(appId, finalDeploymentMetadata.getEnvIds()));
    }

    return finalDeploymentMetadata;
  }

  @Override
  public DeploymentMetadata fetchDeploymentMetadata(String appId, ExecutionArgs executionArgs) {
    return fetchDeploymentMetadata(appId, executionArgs, false, null);
  }

  @Override
  public boolean workflowExecutionsRunning(WorkflowType workflowType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("workflowId", EQ, workflowId)
                                                     .addFilter("workflowType", EQ, workflowType)
                                                     .addFilter("status", IN, NEW, RUNNING)
                                                     .addFieldsIncluded("uuid")
                                                     .build();

    PageResponse<WorkflowExecution> pageResponse = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (isEmpty(pageResponse)) {
      return false;
    }
    return true;
  }

  @Override
  public CountsByStatuses getBreakdown(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution =
        wingsPersistence.getWithAppId(WorkflowExecution.class, appId, workflowExecutionId);
    refreshBreakdown(workflowExecution);
    return workflowExecution.getBreakdown();
  }

  @Override
  public GraphNode getExecutionDetailsForNode(
      String appId, String workflowExecutionId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    notNullCheck("The state details are not available", stateExecutionInstance);
    return graphRenderer.convertToNode(stateExecutionInstance);
  }

  @Override
  public List<StateExecutionData> getExecutionHistory(
      String appId, String workflowExecutionId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    notNullCheck("The state history is not available", stateExecutionInstance);
    return stateExecutionInstance.getStateExecutionDataHistory();
  }

  @Override
  public int getExecutionInterruptCount(String stateExecutionInstanceId) {
    return (int) wingsPersistence.createQuery(ExecutionInterrupt.class)
        .filter(ExecutionInterruptKeys.stateExecutionInstanceId, stateExecutionInstanceId)
        .count();
  }

  @Override
  public List<StateExecutionInterrupt> getExecutionInterrupts(String appId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    notNullCheck("stateExecutionInstance", stateExecutionInstance);

    Map<String, ExecutionInterruptEffect> map = new HashMap<>();
    stateExecutionInstance.getInterruptHistory().forEach(effect -> map.put(effect.getInterruptId(), effect));

    List<StateExecutionInterrupt> interrupts =
        wingsPersistence.createQuery(ExecutionInterrupt.class)
            .filter(ExecutionInterruptKeys.appId, appId)
            .filter(ExecutionInterruptKeys.stateExecutionInstanceId, stateExecutionInstanceId)
            .asList()
            .stream()
            .map(interrupt
                -> StateExecutionInterrupt.builder()
                       .interrupt(interrupt)
                       .tookAffectAt(new Date(interrupt.getCreatedAt()))
                       .build())
            .collect(toList());

    if (isNotEmpty(stateExecutionInstance.getInterruptHistory())) {
      wingsPersistence.createQuery(ExecutionInterrupt.class)
          .filter(ExecutionInterruptKeys.appId, appId)
          .field(ID_KEY)
          .in(map.keySet())
          .asList()
          .forEach(interrupt -> {
            ExecutionInterruptEffect effect = map.get(interrupt.getUuid());
            interrupts.add(
                StateExecutionInterrupt.builder().interrupt(interrupt).tookAffectAt(effect.getTookEffectAt()).build());
          });
    }

    return interrupts;
  }

  @Override
  public List<StateExecutionElement> getExecutionElements(String appId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    notNullCheck("stateExecutionInstance", stateExecutionInstance);

    StateExecutionData stateExecutionData = stateExecutionInstance.fetchStateExecutionData();
    notNullCheck("stateExecutionData", stateExecutionData);
    if (!(stateExecutionData instanceof RepeatStateExecutionData)) {
      throw new InvalidRequestException("Request for elements of instance that is not repeated", USER);
    }

    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) stateExecutionData;

    Map<String, StateExecutionElement> elementMap = repeatStateExecutionData.getRepeatElements()
                                                        .stream()
                                                        .map(element
                                                            -> StateExecutionElement.builder()
                                                                   .executionContextElementId(element.getUuid())
                                                                   .name(element.getName())
                                                                   .progress(0)
                                                                   .status(STARTING)
                                                                   .build())
                                                        .collect(toMap(StateExecutionElement::getName, identity()));

    StateMachine stateMachine = stateExecutionService.obtainStateMachine(stateExecutionInstance);

    int subStates = (int) stateMachine.getChildStateMachines()
                        .get(stateExecutionInstance.getChildStateMachineId())
                        .getStates()
                        .stream()
                        .filter(t -> !t.getStateType().equals("REPEAT") && !t.getStateType().equals("FORK"))
                        .count();

    @Data
    @NoArgsConstructor
    class Stat {
      String element;
      String prevInstanceId;
      ExecutionStatus status;

      int children;
      List<ExecutionStatus> allStatuses = new ArrayList<>();
    }

    Map<String, Stat> stats = new HashMap<>();
    try (HIterator<StateExecutionInstance> stateExecutionInstances = new HIterator<>(
             wingsPersistence.createQuery(StateExecutionInstance.class)
                 .filter(StateExecutionInstanceKeys.appId, appId)
                 .filter(StateExecutionInstanceKeys.executionUuid, stateExecutionInstance.getExecutionUuid())
                 .filter(StateExecutionInstanceKeys.parentInstanceId, stateExecutionInstanceId)
                 .project(StateExecutionInstanceKeys.uuid, true)
                 .project(StateExecutionInstanceKeys.status, true)
                 .project(StateExecutionInstanceKeys.prevInstanceId, true)
                 .project(StateExecutionInstanceKeys.contextElement, true)
                 .fetch())) {
      while (stateExecutionInstances.hasNext()) {
        StateExecutionInstance instance = stateExecutionInstances.next();
        Stat stat = stats.computeIfAbsent(instance.getUuid(), x -> new Stat());
        stat.setElement(instance.getContextElement().getName());
        stat.setPrevInstanceId(instance.getPrevInstanceId());
        stat.setStatus(instance.getStatus());

        stat.setChildren(stat.getChildren() + 1);
        stat.allStatuses.add(instance.getStatus());

        // update previous aggregates
        while (stat.getPrevInstanceId() != null) {
          Stat previousStat = stats.get(stat.getPrevInstanceId());
          if (previousStat == null) {
            break;
          }
          previousStat.setChildren(stat.getChildren() + 1);
          List<ExecutionStatus> statuses = previousStat.getAllStatuses();
          statuses.clear();
          statuses.add(previousStat.getStatus());
          statuses.addAll(stat.getAllStatuses());
          stat = previousStat;
        }

        if (stat.getElement() != null) {
          StateExecutionElement stateExecutionElement = elementMap.get(stat.getElement());
          elementMap.put(stat.getElement(),
              StateExecutionElement.builder()
                  .executionContextElementId(stateExecutionElement.getExecutionContextElementId())
                  .name(stat.getElement())
                  .progress(100 * stat.getChildren() / subStates)
                  .status(GraphRenderer.aggregateStatus(stat.getAllStatuses()))
                  .build());
        }
      }
    }

    return elementMap.values().stream().collect(toList());
  }

  @Override
  public Map<String, GraphGroup> getNodeSubGraphs(
      String appId, String workflowExecutionId, Map<String, List<String>> selectedNodes) {
    Map<String, GraphGroup> nodeSubGraphs = new HashMap<>();
    for (Entry<String, List<String>> entry : selectedNodes.entrySet()) {
      String repeaterId = entry.getKey();
      List<String> selectedInstances = entry.getValue();
      StateExecutionInstance repeatInstance =
          wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, repeaterId);
      notNullCheck("Couldn't find Instance for Id:" + repeaterId, repeatInstance);
      // Instance Id to its stateExecutionInstanceIds.
      LinkedHashMap<String, Map<String, StateExecutionInstance>> nodesInstancesIdMap = new LinkedHashMap<>();

      // initializing map
      for (String instance : selectedInstances) {
        nodesInstancesIdMap.put(instance, new HashMap<>());
      }

      Query<StateExecutionInstance> query =
          getQueryForNodeSubgraphs(appId, workflowExecutionId, selectedInstances, repeaterId);
      try (HIterator<StateExecutionInstance> stateExecutionInstances = new HIterator<>(query.fetch())) {
        for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
          // for older executions stored in DB
          if (isEmpty(stateExecutionInstance.getSubGraphFilterId())) {
            populateNodeInstanceIdMapOlderExecutions(
                selectedNodes, repeaterId, nodesInstancesIdMap, stateExecutionInstance);
          } else {
            populateNodeInstanceIdMap(nodesInstancesIdMap, stateExecutionInstance);
          }
        }
      }

      nodeSubGraphs.put(repeaterId, graphRenderer.generateNodeSubGraph(nodesInstancesIdMap, repeatInstance));
    }
    return nodeSubGraphs;
  }

  private void populateNodeInstanceIdMap(Map<String, Map<String, StateExecutionInstance>> nodesInstancesIdMap,
      StateExecutionInstance stateExecutionInstance) {
    if (nodesInstancesIdMap.get(stateExecutionInstance.getSubGraphFilterId()) != null) {
      stateExecutionInstance.getStateExecutionMap().entrySet().removeIf(
          entry -> !entry.getKey().equals(stateExecutionInstance.getDisplayName()));
      nodesInstancesIdMap.get(stateExecutionInstance.getSubGraphFilterId())
          .put(stateExecutionInstance.getUuid(), stateExecutionInstance);
    }
  }

  private Query<StateExecutionInstance> getQueryForNodeSubgraphs(
      String appId, String workflowExecutionId, List<String> selectedInstances, String repeaterId) {
    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .filter(StateExecutionInstanceKeys.appId, appId)
                                              .filter(StateExecutionInstanceKeys.executionUuid, workflowExecutionId)
                                              .filter(StateExecutionInstanceKeys.parentInstanceId, repeaterId)
                                              .project(StateExecutionInstanceKeys.contextElement, true)
                                              .project(StateExecutionInstanceKeys.subGraphFilterId, true)
                                              .project(StateExecutionInstanceKeys.contextTransition, true)
                                              .project(StateExecutionInstanceKeys.dedicatedInterruptCount, true)
                                              .project(StateExecutionInstanceKeys.displayName, true)
                                              .project(StateExecutionInstanceKeys.executionType, true)
                                              .project(StateExecutionInstanceKeys.uuid, true)
                                              .project(StateExecutionInstanceKeys.interruptHistory, true)
                                              .project(StateExecutionInstanceKeys.lastUpdatedAt, true)
                                              .project(StateExecutionInstanceKeys.parentInstanceId, true)
                                              .project(StateExecutionInstanceKeys.prevInstanceId, true)
                                              .project(StateExecutionInstanceKeys.stateExecutionDataHistory, true)
                                              .project(StateExecutionInstanceKeys.stateExecutionMap, true)
                                              .project(StateExecutionInstanceKeys.stateName, true)
                                              .project(StateExecutionInstanceKeys.stateType, true)
                                              .project(StateExecutionInstanceKeys.status, true)
                                              .project(StateExecutionInstanceKeys.hasInspection, true)
                                              .project(StateExecutionInstanceKeys.appId, true);

    // SubGraphFilterId is the instance (host element) Id.
    // For older execution this will be null.
    CriteriaContainerImpl nullCriteria = query.criteria(StateExecutionInstanceKeys.subGraphFilterId).doesNotExist();
    CriteriaContainerImpl existsCriteria =
        query.criteria(StateExecutionInstanceKeys.subGraphFilterId).in(selectedInstances);
    query.or(nullCriteria, existsCriteria);
    return query;
  }

  private void populateNodeInstanceIdMapOlderExecutions(Map<String, List<String>> selectedNodes, String repeaterId,
      Map<String, Map<String, StateExecutionInstance>> nodesInstancesIdMap,
      StateExecutionInstance stateExecutionInstance) {
    if (stateExecutionInstance.getContextElement() instanceof InstanceElement) {
      InstanceElement contextInstanceElement = (InstanceElement) stateExecutionInstance.getContextElement();
      if (selectedNodes.get(repeaterId).contains(contextInstanceElement.getUuid())) {
        stateExecutionInstance.getStateExecutionMap().entrySet().removeIf(
            entry -> !entry.getKey().equals(stateExecutionInstance.getDisplayName()));
        if (nodesInstancesIdMap.get(contextInstanceElement.getUuid()) != null) {
          nodesInstancesIdMap.get(contextInstanceElement.getUuid())
              .put(stateExecutionInstance.getUuid(), stateExecutionInstance);
        }
      }
    }
  }

  @Override
  public StateExecutionInstance getStateExecutionData(String appId, String stateExecutionInstanceId) {
    return wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
  }

  @Override
  public List<StateExecutionInstance> getStateExecutionData(String appId, String executionUuid, String serviceId,
      String infraMappingId, Optional<String> infrastructureDefinitionId, StateType stateType, String stateName) {
    logger.info(
        "for execution {} looking for following appId: {} executionUuid: {} , stateType: {}, displayName: {}, contextElement.serviceElement.uuid:  {}, contextElement.infraMappingId: {}",
        executionUuid, appId, executionUuid, stateType, stateName, serviceId, infraMappingId);
    Query<StateExecutionInstance> stateExecutionInstanceQuery =
        wingsPersistence.createQuery(StateExecutionInstance.class, excludeAuthority)
            .disableValidation()
            .filter(StateExecutionInstanceKeys.appId, appId)
            .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
            .filter(StateExecutionInstanceKeys.stateType, stateType)
            .filter(StateExecutionInstanceKeys.displayName, stateName)
            .filter("contextElement.serviceElement.uuid", serviceId);
    if (infrastructureDefinitionId.isPresent()) {
      stateExecutionInstanceQuery.filter("contextElement.infraDefinitionId", infrastructureDefinitionId.get());
    } else {
      // once all the infra mappings are moved to infra mapping definition, we can get rid of this else block
      stateExecutionInstanceQuery.filter("contextElement.infraMappingId", infraMappingId);
    }
    List<StateExecutionInstance> rv = new ArrayList<>();
    try (HIterator<StateExecutionInstance> stateExecutionInstances =
             new HIterator<>(stateExecutionInstanceQuery.fetch())) {
      while (stateExecutionInstances.hasNext()) {
        rv.add(stateExecutionInstances.next());
      }
    }

    return rv;
  }

  private void refreshSummaries(WorkflowExecution workflowExecution) {
    if (workflowExecution.getServiceExecutionSummaries() != null) {
      return;
    }
    boolean infraRefactor = featureFlagService.isEnabled(
        INFRA_MAPPING_REFACTOR, appService.getAccountIdByAppId(workflowExecution.getAppId()));
    List<ElementExecutionSummary> serviceExecutionSummaries = new ArrayList<>();
    // TODO : version should also be captured as part of the WorkflowExecution
    Workflow workflow = workflowService.readWorkflow(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
    if (workflow != null && workflow.getOrchestrationWorkflow() != null) {
      List<Service> services = getResolvedServices(workflow, workflowExecution);
      List<InfrastructureMapping> infrastructureMappings = null;
      List<InfrastructureDefinition> infrastructureDefinitions = null;
      if (infraRefactor) {
        infrastructureDefinitions = getResolvedInfraDefinitions(workflow, workflowExecution);
      } else {
        infrastructureMappings = getResolvedInfraMappings(workflow, workflowExecution);
      }

      if (services != null) {
        List<InfrastructureMapping> finalInfrastructureMappings = infrastructureMappings;
        List<InfrastructureDefinition> finalInfrastructureDefinitions = infrastructureDefinitions;
        services.forEach(service -> {
          ServiceElement serviceElement =
              ServiceElement.builder().uuid(service.getUuid()).name(service.getName()).build();
          ElementExecutionSummary elementSummary =
              anElementExecutionSummary().withContextElement(serviceElement).withStatus(ExecutionStatus.QUEUED).build();

          if (infraRefactor) {
            List<InfraDefinitionSummary> infraDefinitionSummaries = new ArrayList<>();
            if (finalInfrastructureDefinitions != null) {
              for (InfrastructureDefinition infrastructureDefinition : finalInfrastructureDefinitions) {
                infraDefinitionSummaries.add(
                    InfraDefinitionSummary.builder()
                        .infraDefinitionId(infrastructureDefinition.getUuid())
                        .deploymentType(infrastructureDefinition.getDeploymentType())
                        .displayName(infrastructureDefinition.getName())
                        .cloudProviderType(infrastructureDefinition.getInfrastructure().getCloudProviderType())
                        .cloudProviderName(
                            infrastructureDefinitionService.cloudProviderNameForDefinition(infrastructureDefinition))
                        .build());
              }
              elementSummary.setInfraDefinitionSummaries(infraDefinitionSummaries);
            }
          } else {
            List<InfraMappingSummary> infraMappingSummaries = new ArrayList<>();
            if (finalInfrastructureMappings != null) {
              for (InfrastructureMapping infraMapping : finalInfrastructureMappings) {
                if (infraMapping.getServiceId().equals(service.getUuid())) {
                  DeploymentType deploymentType = serviceResourceService.getDeploymentType(infraMapping, service, null);
                  infraMappingSummaries.add(anInfraMappingSummary()
                                                .withInframappingId(infraMapping.getUuid())
                                                .withInfraMappingType(infraMapping.getInfraMappingType())
                                                .withComputerProviderName(infraMapping.getComputeProviderName())
                                                .withDisplayName(infraMapping.getName())
                                                .withDeploymentType(deploymentType.name())
                                                .withComputerProviderType(infraMapping.getComputeProviderType())
                                                .build());
                }
              }
              elementSummary.setInfraMappingSummary(infraMappingSummaries);
            }
          }
          serviceExecutionSummaries.add(elementSummary);
        });
      }
    }
    Map<String, ElementExecutionSummary> serviceExecutionSummaryMap =
        serviceExecutionSummaries.stream().collect(toMap(summary -> summary.getContextElement().getUuid(), identity()));

    populateServiceSummary(serviceExecutionSummaryMap, workflowExecution);

    if (!serviceExecutionSummaryMap.isEmpty()) {
      Collections.sort(serviceExecutionSummaries, ElementExecutionSummary.startTsComparator);
      workflowExecution.setServiceExecutionSummaries(serviceExecutionSummaries);
      if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
        wingsPersistence.updateField(WorkflowExecution.class, workflowExecution.getUuid(), "serviceExecutionSummaries",
            workflowExecution.getServiceExecutionSummaries());
      }
    }
  }

  private List<Service> getResolvedServices(Workflow workflow, WorkflowExecution workflowExecution) {
    ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
    Map<String, String> workflowVariables = executionArgs != null ? executionArgs.getWorkflowVariables() : null;
    return workflowService.getResolvedServices(workflow, workflowVariables);
  }

  @Override
  public List<InfrastructureMapping> getResolvedInfraMappings(Workflow workflow, WorkflowExecution workflowExecution) {
    Map<String, String> workflowVariables = workflowExecution.getExecutionArgs() != null
        ? workflowExecution.getExecutionArgs().getWorkflowVariables()
        : null;
    return workflowService.getResolvedInfraMappings(workflow, workflowVariables);
  }

  @Override
  public List<InfrastructureDefinition> getResolvedInfraDefinitions(
      Workflow workflow, WorkflowExecution workflowExecution) {
    Map<String, String> workflowVariables = workflowExecution.getExecutionArgs() != null
        ? workflowExecution.getExecutionArgs().getWorkflowVariables()
        : null;
    return workflowService.getResolvedInfraDefinitions(workflow, workflowVariables);
  }

  private void populateServiceSummary(
      Map<String, ElementExecutionSummary> serviceSummaryMap, WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter(StateExecutionInstanceKeys.appId, EQ, workflowExecution.getAppId())
            .addFilter(StateExecutionInstanceKeys.executionUuid, EQ, workflowExecution.getUuid())
            .addFilter(StateExecutionInstanceKeys.stateType, IN, StateType.REPEAT.name(), StateType.FORK.name(),
                StateType.SUB_WORKFLOW.name(), StateType.PHASE.name(), PHASE_STEP.name())
            .addFilter(StateExecutionInstanceKeys.parentInstanceId, NOT_EXISTS)
            .addOrder(StateExecutionInstanceKeys.createdAt, OrderType.ASC)
            .build();

    PageResponse<StateExecutionInstance> pageResponse =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest);

    if (isEmpty(pageResponse)) {
      return;
    }

    for (StateExecutionInstance stateExecutionInstance : pageResponse.getResponse()) {
      if (!(stateExecutionInstance.fetchStateExecutionData() instanceof ElementStateExecutionData)) {
        continue;
      }
      if (stateExecutionInstance.isRollback()
          && !checkIfOnDemand(workflowExecution.getAppId(), workflowExecution.getUuid())) {
        continue;
      }

      ElementStateExecutionData elementStateExecutionData =
          (ElementStateExecutionData) stateExecutionInstance.fetchStateExecutionData();
      if (isEmpty(elementStateExecutionData.getElementStatusSummary())) {
        continue;
      }
      for (ElementExecutionSummary summary : elementStateExecutionData.getElementStatusSummary()) {
        ServiceElement serviceElement = getServiceElement(summary.getContextElement());
        if (serviceElement == null) {
          continue;
        }
        ElementExecutionSummary serviceSummary = serviceSummaryMap.get(serviceElement.getUuid());
        if (serviceSummary == null) {
          serviceSummary =
              anElementExecutionSummary().withContextElement(serviceElement).withStatus(ExecutionStatus.QUEUED).build();
          serviceSummaryMap.put(serviceElement.getUuid(), serviceSummary);
        }
        if (serviceSummary.getStartTs() == null
            || (summary.getStartTs() != null && serviceSummary.getStartTs() > summary.getStartTs())) {
          serviceSummary.setStartTs(summary.getStartTs());
        }
        if (serviceSummary.getEndTs() == null
            || (summary.getEndTs() != null && serviceSummary.getEndTs() < summary.getEndTs())) {
          serviceSummary.setEndTs(summary.getEndTs());
        }
        if (serviceSummary.getInstanceStatusSummaries() == null) {
          serviceSummary.setInstanceStatusSummaries(new ArrayList<>());
        }
        if (summary.getInstanceStatusSummaries() != null) {
          serviceSummary.getInstanceStatusSummaries().addAll(summary.getInstanceStatusSummaries());
        }
        serviceSummary.setStatus(summary.getStatus());
      }
    }
  }

  private ServiceElement getServiceElement(ContextElement contextElement) {
    if (contextElement == null) {
      return null;
    }
    ContextElementType elementType = contextElement.getElementType();
    switch (elementType) {
      case SERVICE: {
        return (ServiceElement) contextElement;
      }
      case SERVICE_TEMPLATE: {
        return ((ServiceTemplateElement) contextElement).getServiceElement();
      }
      case INSTANCE: {
        return ((InstanceElement) contextElement).getServiceTemplateElement().getServiceElement();
      }
      case PARAM: {
        if (PhaseElement.PHASE_PARAM.equals(contextElement.getName())) {
          return ((PhaseElement) contextElement).getServiceElement();
        }
        break;
      }
      default:
        unhandled(elementType);
    }
    return null;
  }

  private void refreshBreakdown(WorkflowExecution workflowExecution) {
    if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus()) && workflowExecution.getBreakdown() != null) {
      return;
    }
    CountsByStatuses breakdown;
    int total;

    if (workflowExecution.getOrchestrationType() == OrchestrationWorkflowType.ROLLING
        && !workflowServiceHelper.isExecutionForK8sV2Service(workflowExecution)) {
      logger.info("Calculating the breakdown for workflowExecutionId {} and workflowId {} ",
          workflowExecution.getUuid(), workflowExecution.getWorkflowId());
      total = workflowExecution.getTotal();
      if (total == 0) {
        total = refreshTotal(workflowExecution);
      }
      breakdown = getBreakdownFromPhases(workflowExecution);
      breakdown.setQueued(total - (breakdown.getFailed() + breakdown.getSuccess() + breakdown.getInprogress()));
    } else {
      StateMachine sm = obtainStateMachine(workflowExecution);
      if (sm == null) {
        return;
      }

      Map<String, ExecutionStatus> stateExecutionStatuses = new HashMap<>();
      try (HIterator<StateExecutionInstance> iterator =
               new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                   .filter(StateExecutionInstanceKeys.appId, workflowExecution.getAppId())
                                   .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
                                   .project(StateExecutionInstanceKeys.contextElement, true)
                                   .project(StateExecutionInstanceKeys.displayName, true)
                                   .project(StateExecutionInstanceKeys.uuid, true)
                                   .project(StateExecutionInstanceKeys.parentInstanceId, true)
                                   .project(StateExecutionInstanceKeys.status, true)
                                   .fetch())) {
        stateMachineExecutionSimulator.prepareStateExecutionInstanceMap(iterator, stateExecutionStatuses);
      }

      breakdown = stateMachineExecutionSimulator.getStatusBreakdown(
          workflowExecution.getAppId(), workflowExecution.getEnvId(), sm, stateExecutionStatuses);
      total = breakdown.getFailed() + breakdown.getSuccess() + breakdown.getInprogress() + breakdown.getQueued();
    }

    workflowExecution.setBreakdown(breakdown);
    workflowExecution.setTotal(total);
    logger.info("Got the breakdown status: {}, breakdown: {}", workflowExecution.getStatus(), breakdown);

    if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
      logger.info(
          "Set the breakdown of the completed status: {}, breakdown: {}", workflowExecution.getStatus(), breakdown);

      Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
                                           .filter(ID_KEY, workflowExecution.getUuid());

      UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class);

      try {
        updateOps.set("breakdown", breakdown).set("total", total);
        UpdateResults updated = wingsPersistence.update(query, updateOps);
        logger.info("Updated : {} row", updated.getWriteResult().getN());
      } catch (Exception e) {
        logger.error("Error occurred while updating with breakdown summary", e);
      }
    }
  }

  private CountsByStatuses getBreakdownFromPhases(WorkflowExecution workflowExecution) {
    CountsByStatuses breakdown = new CountsByStatuses();

    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.appId, workflowExecution.getAppId())
            .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
            .filter(StateExecutionInstanceKeys.stateType, PHASE.name())
            .filter(StateExecutionInstanceKeys.rollback, workflowExecution.isOnDemandRollback())
            .field(StateExecutionInstanceKeys.createdAt)
            .greaterThanOrEq(workflowExecution.getCreatedAt())
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      breakdown.setSuccess(0);
    }
    for (StateExecutionInstance stateExecutionInstance : allStateExecutionInstances) {
      StateExecutionData stateExecutionData = stateExecutionInstance.fetchStateExecutionData();
      if (!(stateExecutionData instanceof PhaseExecutionData)) {
        continue;
      }
      PhaseExecutionData phaseExecutionData = (PhaseExecutionData) stateExecutionData;
      List<ElementExecutionSummary> elementStatusSummary = phaseExecutionData.getElementStatusSummary();
      if (isEmpty(elementStatusSummary)) {
        continue;
      }
      for (ElementExecutionSummary elementExecutionSummary : elementStatusSummary) {
        if (elementExecutionSummary == null || elementExecutionSummary.getInstanceStatusSummaries() == null) {
          continue;
        }
        for (InstanceStatusSummary instanceStatusSummary : elementExecutionSummary.getInstanceStatusSummaries()) {
          switch (instanceStatusSummary.getStatus()) {
            case SUCCESS: {
              breakdown.setSuccess(breakdown.getSuccess() + 1);
              break;
            }
            case ERROR:
            case FAILED: {
              breakdown.setFailed(breakdown.getFailed() + 1);
              break;
            }
            case STARTING:
            case RUNNING: {
              breakdown.setInprogress(breakdown.getInprogress() + 1);
              break;
            }
            default: {
              breakdown.setQueued(breakdown.getQueued() + 1);
              break;
            }
          }
        }
      }
    }
    return breakdown;
  }

  private int refreshTotal(WorkflowExecution workflowExecution) {
    boolean infraRefactor = featureFlagService.isEnabled(INFRA_MAPPING_REFACTOR, workflowExecution.getAccountId());
    Workflow workflow = workflowService.readWorkflow(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      logger.info("Workflow was deleted. Skipping the refresh total");
      return 0;
    }

    List<String> resolvedInfraMappingIds;
    if (infraRefactor) {
      resolvedInfraMappingIds = workflowExecution.getInfraMappingIds();
    } else {
      List<InfrastructureMapping> resolvedInfraMappings = getResolvedInfraMappings(workflow, workflowExecution);
      resolvedInfraMappingIds = resolvedInfraMappings.stream().map(InfrastructureMapping::getUuid).collect(toList());
    }
    if (isEmpty(resolvedInfraMappingIds)) {
      return 0;
    }
    try {
      List<Host> hosts = hostService.getHostsByInfraMappingIds(workflow.getAppId(), resolvedInfraMappingIds);
      return hosts == null ? 0 : hosts.size();
    } catch (Exception e) {
      logger.error(
          "Error occurred while calculating Refresh total for workflow execution {}", workflowExecution.getUuid(), e);
    }

    return 0;
  }

  // @TODO_PCF
  @Override
  public List<ElementExecutionSummary> getElementsSummary(
      String appId, String executionUuid, String parentStateExecutionInstanceId) {
    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.appId, appId)
            .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
            .filter(StateExecutionInstanceKeys.parentInstanceId, parentStateExecutionInstanceId)
            .order(Sort.ascending(StateExecutionInstanceKeys.createdAt))
            .project(StateExecutionInstanceKeys.contextElements, false)
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      return null;
    }

    List<StateExecutionInstance> contextTransitionInstances =
        allStateExecutionInstances.stream().filter(StateExecutionInstance::isContextTransition).collect(toList());
    Map<String, StateExecutionInstance> prevInstanceIdMap =
        allStateExecutionInstances.stream()
            .filter(instance -> instance.getPrevInstanceId() != null)
            .collect(toMap(StateExecutionInstance::getPrevInstanceId, identity()));

    List<ElementExecutionSummary> elementExecutionSummaries = new ArrayList<>();
    for (StateExecutionInstance stateExecutionInstance : contextTransitionInstances) {
      ContextElement contextElement = stateExecutionInstance.getContextElement();
      ElementExecutionSummary elementExecutionSummary = anElementExecutionSummary()
                                                            .withContextElement(contextElement)
                                                            .withStartTs(stateExecutionInstance.getStartTs())
                                                            .build();

      List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();

      StateExecutionInstance last = stateExecutionInstance;
      for (StateExecutionInstance next = stateExecutionInstance; next != null;
           next = prevInstanceIdMap.get(next.getUuid())) {
        StateType nextStateType = StateType.valueOf(next.getStateType());

        if ((nextStateType == StateType.REPEAT || nextStateType == StateType.FORK || nextStateType == StateType.PHASE
                || nextStateType == PHASE_STEP || nextStateType == StateType.SUB_WORKFLOW)
            && next.fetchStateExecutionData() instanceof ElementStateExecutionData) {
          ElementStateExecutionData elementStateExecutionData =
              (ElementStateExecutionData) next.fetchStateExecutionData();
          instanceStatusSummaries.addAll(elementStateExecutionData.getElementStatusSummary()
                                             .stream()
                                             .filter(e -> e.getInstanceStatusSummaries() != null)
                                             .flatMap(l -> l.getInstanceStatusSummaries().stream())
                                             .collect(toList()));
        } else if ((nextStateType == StateType.ECS_SERVICE_DEPLOY || nextStateType == StateType.KUBERNETES_DEPLOY
                       || nextStateType == StateType.AWS_CODEDEPLOY_STATE)
            && next.fetchStateExecutionData() instanceof CommandStateExecutionData) {
          CommandStateExecutionData commandStateExecutionData =
              (CommandStateExecutionData) next.fetchStateExecutionData();
          instanceStatusSummaries.addAll(commandStateExecutionData.getNewInstanceStatusSummaries());
        } else if (nextStateType == StateType.AWS_AMI_SERVICE_DEPLOY
            && next.fetchStateExecutionData() instanceof AwsAmiDeployStateExecutionData) {
          AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
              (AwsAmiDeployStateExecutionData) next.fetchStateExecutionData();
          instanceStatusSummaries.addAll(awsAmiDeployStateExecutionData.getNewInstanceStatusSummaries());
        } else if (nextStateType == StateType.HELM_DEPLOY) {
          StateExecutionData stateExecutionData = next.fetchStateExecutionData();
          if (stateExecutionData instanceof HelmDeployStateExecutionData) {
            HelmDeployStateExecutionData helmDeployStateExecutionData =
                (HelmDeployStateExecutionData) stateExecutionData;
            if (isNotEmpty(helmDeployStateExecutionData.getNewInstanceStatusSummaries())) {
              instanceStatusSummaries.addAll(helmDeployStateExecutionData.getNewInstanceStatusSummaries());
            }
          }
        } else if (nextStateType == StateType.KUBERNETES_STEADY_STATE_CHECK) {
          KubernetesSteadyStateCheckExecutionData kubernetesSteadyStateCheckExecutionData =
              (KubernetesSteadyStateCheckExecutionData) next.fetchStateExecutionData();
          if (isNotEmpty(kubernetesSteadyStateCheckExecutionData.getNewInstanceStatusSummaries())) {
            instanceStatusSummaries.addAll(kubernetesSteadyStateCheckExecutionData.getNewInstanceStatusSummaries());
          }
        } else if (nextStateType == StateType.K8S_DEPLOYMENT_ROLLING || nextStateType == StateType.K8S_CANARY_DEPLOY
            || nextStateType == StateType.K8S_BLUE_GREEN_DEPLOY || nextStateType == StateType.K8S_SCALE) {
          StateExecutionData stateExecutionData = next.fetchStateExecutionData();
          if (stateExecutionData instanceof K8sStateExecutionData) {
            K8sStateExecutionData k8sStateExecutionData = (K8sStateExecutionData) stateExecutionData;
            if (isNotEmpty(k8sStateExecutionData.getNewInstanceStatusSummaries())) {
              instanceStatusSummaries.addAll(k8sStateExecutionData.getNewInstanceStatusSummaries());
            }
          }
        }
        last = next;
      }

      if (elementExecutionSummary.getEndTs() == null || elementExecutionSummary.getEndTs() < last.getEndTs()) {
        elementExecutionSummary.setEndTs(last.getEndTs());
      }
      if (contextElement != null && contextElement.getElementType() == ContextElementType.INSTANCE) {
        instanceStatusSummaries.add(anInstanceStatusSummary()
                                        .withInstanceElement((InstanceElement) contextElement.cloneMin())
                                        .withStatus(last.getStatus())
                                        .build());
      }

      instanceStatusSummaries = instanceStatusSummaries.stream()
                                    .filter(instanceStatusSummary -> instanceStatusSummary.getInstanceElement() != null)
                                    .collect(toList());
      instanceStatusSummaries =
          instanceStatusSummaries.stream()
              .filter(distinctByKey(instanceStatusSummary -> instanceStatusSummary.getInstanceElement().getUuid()))
              .collect(toList());

      elementExecutionSummary.setStatus(last.getStatus());
      elementExecutionSummary.setInstanceStatusSummaries(instanceStatusSummaries);
      elementExecutionSummaries.add(elementExecutionSummary);
    }

    return elementExecutionSummaries;
  }

  @Override
  public PhaseExecutionSummary getPhaseExecutionSummary(
      String appId, String executionUuid, String stateExecutionInstanceId) {
    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.appId, appId)
            .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
            .filter(StateExecutionInstanceKeys.parentInstanceId, stateExecutionInstanceId)
            .filter(StateExecutionInstanceKeys.stateType, PHASE_STEP.name())
            .project(StateExecutionInstanceKeys.contextElement, true)
            .project(StateExecutionInstanceKeys.displayName, true)
            .project(StateExecutionInstanceKeys.uuid, true)
            .project(StateExecutionInstanceKeys.parentInstanceId, true)
            .project(StateExecutionInstanceKeys.stateExecutionMap, true)
            .project(StateExecutionInstanceKeys.stateType, true)
            .project(StateExecutionInstanceKeys.status, true)
            .asList();

    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    if (isEmpty(allStateExecutionInstances)) {
      return null;
    }

    allStateExecutionInstances.forEach(instance -> {
      StateExecutionData stateExecutionData = instance.fetchStateExecutionData();
      if (stateExecutionData instanceof PhaseStepExecutionData) {
        PhaseStepExecutionData phaseStepExecutionData = (PhaseStepExecutionData) stateExecutionData;
        phaseExecutionSummary.getPhaseStepExecutionSummaryMap().put(
            instance.getDisplayName(), phaseStepExecutionData.getPhaseStepExecutionSummary());
      }
    });

    return phaseExecutionSummary;
  }

  @Override
  public PhaseStepExecutionSummary getPhaseStepExecutionSummary(
      String appId, String executionUuid, String stateExecutionInstanceId) {
    PhaseStepExecutionSummary phaseStepExecutionSummary = new PhaseStepExecutionSummary();
    List<StepExecutionSummary> stepExecutionSummaryList = phaseStepExecutionSummary.getStepExecutionSummaryList();

    List<String> parentInstanceIds = new ArrayList<>();
    parentInstanceIds.add(stateExecutionInstanceId);

    while (isNotEmpty(parentInstanceIds)) {
      try (HIterator<StateExecutionInstance> iterator =
               new HIterator<>(wingsPersistence.createQuery(StateExecutionInstance.class)
                                   .filter(StateExecutionInstanceKeys.appId, appId)
                                   .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
                                   .field(StateExecutionInstanceKeys.parentInstanceId)
                                   .in(parentInstanceIds)
                                   .project(StateExecutionInstanceKeys.contextElement, true)
                                   .project(StateExecutionInstanceKeys.displayName, true)
                                   .project(StateExecutionInstanceKeys.uuid, true)
                                   .project(StateExecutionInstanceKeys.parentInstanceId, true)
                                   .project(StateExecutionInstanceKeys.stateExecutionMap, true)
                                   .project(StateExecutionInstanceKeys.stateType, true)
                                   .project(StateExecutionInstanceKeys.status, true)
                                   .fetch())) {
        if (!iterator.hasNext()) {
          return null;
        }

        parentInstanceIds.clear();
        while (iterator.hasNext()) {
          StateExecutionInstance instance = iterator.next();
          if (StateType.REPEAT.name().equals(instance.getStateType())
              || StateType.FORK.name().equals(instance.getStateType())) {
            parentInstanceIds.add(instance.getUuid());
          } else {
            stepExecutionSummaryList.add(instance.fetchStateExecutionData().getStepExecutionSummary());
          }
        }
      }
    }

    return phaseStepExecutionSummary;
  }

  public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }

  @Override
  public List<Artifact> getArtifactsCollected(String appId, String executionUuid) {
    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.appId, appId)
            .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
            .filter(StateExecutionInstanceKeys.stateType, ARTIFACT_COLLECTION.name())
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      return null;
    }

    List<Artifact> artifacts = new ArrayList<>();
    allStateExecutionInstances.forEach(stateExecutionInstance -> {
      ArtifactCollectionExecutionData artifactCollectionExecutionData =
          (ArtifactCollectionExecutionData) stateExecutionInstance.fetchStateExecutionData();
      artifacts.add(artifactService.get(artifactCollectionExecutionData.getArtifactId()));
    });
    return artifacts;
  }

  @Override
  public List<StateExecutionInstance> getStateExecutionInstances(String appId, String executionUuid) {
    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.appId, appId)
            .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
            .filter(StateExecutionInstanceKeys.stateType, ARTIFACT_COLLECTION.name())
            .asList();

    if (allStateExecutionInstances == null) {
      return new ArrayList<>();
    }
    return allStateExecutionInstances;
  }

  @Override
  public List<StateExecutionInstance> getStateExecutionInstancesForPhases(String executionUuid) {
    return wingsPersistence.createQuery(StateExecutionInstance.class)
        .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
        .filter(StateExecutionInstanceKeys.stateType, PHASE.name())
        .asList();
  }

  @Override
  public void refreshBuildExecutionSummary(String workflowExecutionId, BuildExecutionSummary buildExecutionSummary) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
    if (workflowExecution == null) {
      return;
    }

    List<BuildExecutionSummary> buildExecutionSummaries = workflowExecution.getBuildExecutionSummaries();
    if (isEmpty(buildExecutionSummaries)) {
      buildExecutionSummaries = new ArrayList<>();
    }
    buildExecutionSummaries.add(buildExecutionSummary);
    buildExecutionSummaries = buildExecutionSummaries.stream()
                                  .filter(distinctByKey(BuildExecutionSummary::getArtifactStreamId))
                                  .collect(toList());
    wingsPersistence.updateField(
        WorkflowExecution.class, workflowExecutionId, "buildExecutionSummaries", buildExecutionSummaries);
  }

  @Override
  public void refreshHelmExecutionSummary(String workflowExecutionId, HelmExecutionSummary helmExecutionSummary) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
    if (workflowExecution == null) {
      return;
    }

    wingsPersistence.updateField(
        WorkflowExecution.class, workflowExecutionId, WorkflowExecutionKeys.helmExecutionSummary, helmExecutionSummary);
  }

  @Override
  public void refreshAwsLambdaExecutionSummary(
      String workflowExecutionId, List<AwsLambdaExecutionSummary> awsLambdaExecutionSummaries) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
    if (workflowExecution == null) {
      return;
    }

    wingsPersistence.updateField(WorkflowExecution.class, workflowExecutionId,
        WorkflowExecutionKeys.awsLambdaExecutionSummaries, awsLambdaExecutionSummaries);
  }

  @Override
  public int getInstancesDeployedFromExecution(WorkflowExecution workflowExecution) {
    int instanceCount = 0;
    if ((workflowExecution.getWorkflowType() == ORCHESTRATION)
        && workflowExecution.getServiceExecutionSummaries() != null) {
      instanceCount += workflowExecution.getServiceExecutionSummaries()
                           .stream()
                           .map(ElementExecutionSummary::getInstancesCount)
                           .mapToInt(i -> i)
                           .sum();
    } else if (workflowExecution.getWorkflowType() == PIPELINE && workflowExecution.getPipelineExecution() != null
        && isNotEmpty(workflowExecution.getPipelineExecution().getPipelineStageExecutions())) {
      for (PipelineStageExecution pipelineStageExecution :
          workflowExecution.getPipelineExecution().getPipelineStageExecutions()) {
        if (pipelineStageExecution == null || pipelineStageExecution.getWorkflowExecutions() == null) {
          continue;
        }
        instanceCount += pipelineStageExecution.getWorkflowExecutions()
                             .stream()
                             .filter(workflowExecution1 -> workflowExecution1.getServiceExecutionSummaries() != null)
                             .flatMap(workflowExecution1 -> workflowExecution1.getServiceExecutionSummaries().stream())
                             .map(ElementExecutionSummary::getInstancesCount)
                             .mapToInt(i -> i)
                             .sum();
      }
    }
    return instanceCount;
  }

  @Override
  public Set<WorkflowExecutionBaseline> markBaseline(String appId, String workflowExecutionId, boolean isBaseline) {
    WorkflowExecution workflowExecution =
        wingsPersistence.getWithAppId(WorkflowExecution.class, appId, workflowExecutionId);
    if (workflowExecution == null) {
      throw new InvalidBaselineConfigurationException(
          "No workflow execution found with id: " + workflowExecutionId + " appId: " + appId);
    }
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    switch (workflowExecution.getWorkflowType()) {
      case PIPELINE:
        PipelineExecution pipelineExecution = workflowExecution.getPipelineExecution();
        if (pipelineExecution == null) {
          throw new InvalidBaselineConfigurationException("Pipeline has not been executed.");
        }

        List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
        if (isEmpty(pipelineStageExecutions)) {
          throw new InvalidBaselineConfigurationException("No workflows have been executed for this pipeline.");
        }
        pipelineStageExecutions.forEach(
            pipelineStageExecution -> workflowExecutions.addAll(pipelineStageExecution.getWorkflowExecutions()));
        break;
      case ORCHESTRATION:
        workflowExecutions.add(workflowExecution);
        break;
      default:
        unhandled(workflowExecution.getWorkflowType());
    }

    Set<WorkflowExecutionBaseline> baselines = new HashSet<>();

    if (!isEmpty(workflowExecutions)) {
      workflowExecutions.forEach(stageExecution -> {
        String executionUuid = stageExecution.getUuid();
        List<StateExecutionInstance> stateExecutionInstances =
            wingsPersistence.createQuery(StateExecutionInstance.class)
                .filter(StateExecutionInstanceKeys.appId, appId)
                .filter(StateExecutionInstanceKeys.executionUuid, executionUuid)
                .asList();

        boolean containsVerificationState = false;
        for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
          StateType stateType = StateType.valueOf(stateExecutionInstance.getStateType());
          if (stateType.isVerificationState()) {
            containsVerificationState = true;
            break;
          }
        }

        if (containsVerificationState) {
          for (String serviceId : stageExecution.getServiceIds()) {
            WorkflowExecutionBaseline executionBaseline = WorkflowExecutionBaseline.builder()
                                                              .workflowId(stageExecution.getWorkflowId())
                                                              .workflowExecutionId(executionUuid)
                                                              .envId(stageExecution.getEnvId())
                                                              .serviceId(serviceId)
                                                              .build();
            executionBaseline.setAppId(stageExecution.getAppId());
            if (workflowExecution.getWorkflowType() == WorkflowType.PIPELINE) {
              executionBaseline.setPipelineExecutionId(workflowExecutionId);
            }
            baselines.add(executionBaseline);
          }
        }
      });
    }

    if (isEmpty(baselines)) {
      throw new WingsException(ErrorCode.BASELINE_CONFIGURATION_ERROR,
          "Either there is no workflow execution with verification steps or verification steps haven't been executed for the workflow.")
          .addParam("message",
              "Either there is no workflow execution with verification steps or verification steps haven't been executed for the workflow.");
    }

    workflowExecutionBaselineService.markBaseline(Lists.newArrayList(baselines), workflowExecutionId, isBaseline);
    return baselines;
  }

  @Override
  public WorkflowExecutionBaseline getBaselineDetails(
      String appId, String baselineWorkflowExecutionId, String stateExecutionId, String currentExecId) {
    DeploymentExecutionContext executionContext =
        stateMachineExecutor.getExecutionContext(appId, currentExecId, stateExecutionId);
    if (executionContext == null) {
      logger.info("failed to get baseline details for app {}, workflow execution {}, uuid {}", appId, currentExecId,
          stateExecutionId);
      return null;
    }
    WorkflowStandardParams workflowStandardParams = executionContext.fetchWorkflowStandardParamsFromContext();
    String envId = workflowStandardParams.fetchRequiredEnv().getUuid();
    PhaseElement phaseElement = executionContext.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    final WorkflowExecutionBaseline workflowExecutionBaseline =
        wingsPersistence.createQuery(WorkflowExecutionBaseline.class, excludeAuthority)
            .filter(WorkflowExecutionBaselineKeys.workflowExecutionId, baselineWorkflowExecutionId)
            .filter(WorkflowExecutionBaselineKeys.envId, envId)
            .filter(WorkflowExecutionBaselineKeys.serviceId, serviceId)
            .get();
    if (workflowExecutionBaseline != null) {
      return workflowExecutionBaseline;
    }

    final WorkflowExecution baselineWorkflowExecution = getWorkflowExecution(appId, baselineWorkflowExecutionId);
    if (baselineWorkflowExecution == null) {
      return null;
    }

    return WorkflowExecutionBaseline.builder()
        .workflowId(baselineWorkflowExecution.getWorkflowId())
        .envId(baselineWorkflowExecution.getEnvId())
        .serviceId(serviceId)
        .workflowExecutionId(baselineWorkflowExecutionId)
        .pipelineExecutionId(baselineWorkflowExecution.getPipelineExecutionId())
        .build();
  }

  @Override
  public List<WorkflowExecution> obtainWorkflowExecutions(List<String> appIds, long fromDateEpochMilli) {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    try (HIterator<WorkflowExecution> iterator = obtainWorkflowExecutionIterator(appIds, fromDateEpochMilli)) {
      while (iterator.hasNext()) {
        workflowExecutions.add(iterator.next());
      }
    }
    return workflowExecutions;
  }

  @Override
  public List<WorkflowExecution> obtainWorkflowExecutions(String accountId, long fromDateEpochMilli) {
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    try (HIterator<WorkflowExecution> iterator = obtainWorkflowExecutionIterator(accountId, fromDateEpochMilli)) {
      while (iterator.hasNext()) {
        workflowExecutions.add(iterator.next());
      }
    }
    return workflowExecutions;
  }

  @Override
  public HIterator<WorkflowExecution> obtainWorkflowExecutionIterator(List<String> appIds, long epochMilli) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field(WorkflowExecutionKeys.createdAt)
                                         .greaterThanOrEq(epochMilli)
                                         .field(WorkflowExecutionKeys.pipelineExecutionId)
                                         .doesNotExist()
                                         .field(WorkflowExecutionKeys.appId)
                                         .in(appIds)
                                         .project(WorkflowExecutionKeys.stateMachine, false);
    return new HIterator<>(query.fetch());
  }

  private HIterator<WorkflowExecution> obtainWorkflowExecutionIterator(String accountId, long epochMilli) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field(WorkflowExecutionKeys.createdAt)
                                         .greaterThanOrEq(epochMilli)
                                         .field(WorkflowExecutionKeys.pipelineExecutionId)
                                         .doesNotExist()
                                         .field(WorkflowExecutionKeys.accountId)
                                         .equal(accountId)
                                         .project(WorkflowExecutionKeys.stateMachine, false);
    return new HIterator<>(query.fetch());
  }

  @Override
  public List<Artifact> obtainLastGoodDeployedArtifacts(String appId, String workflowId) {
    WorkflowExecution workflowExecution = fetchLastSuccessDeployment(appId, workflowId);
    if (workflowExecution != null) {
      ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
      if (executionArgs != null) {
        return executionArgs.getArtifacts();
      }
    }
    return new ArrayList<>();
  }

  @Override
  public List<ArtifactVariable> obtainLastGoodDeployedArtifactsVariables(String appId, String workflowId) {
    WorkflowExecution workflowExecution = fetchLastSuccessDeployment(appId, workflowId);
    // Todo call fetchLastSuccessDeployment (fetch infra mapping)
    if (workflowExecution != null) {
      ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
      if (executionArgs != null) {
        return executionArgs.getArtifactVariables();
      }
    }
    return new ArrayList<>();
  }

  private WorkflowExecution fetchLastSuccessDeployment(String appId, String workflowId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .filter(WorkflowExecutionKeys.workflowId, workflowId)
        .filter(WorkflowExecutionKeys.appId, appId)
        .filter(WorkflowExecutionKeys.status, SUCCESS)
        .order("-createdAt")
        .get();
  }

  private WorkflowExecution fetchLastSuccessDeployment(WorkflowExecution workflowExecution) {
    FindOptions findOptions = new FindOptions();
    Query<WorkflowExecution> workflowExecutionQuery =
        wingsPersistence.createQuery(WorkflowExecution.class)
            .filter(WorkflowExecutionKeys.status, SUCCESS)
            .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
            .filter(WorkflowExecutionKeys.workflowId, workflowExecution.getWorkflowId());
    if (isNotEmpty(workflowExecution.getInfraMappingIds())) {
      workflowExecutionQuery.filter(WorkflowExecutionKeys.infraMappingIds, workflowExecution.getInfraMappingIds());
    }
    if (workflowExecution.isOnDemandRollback()) {
      findOptions = findOptions.skip(1);
    }
    return workflowExecutionQuery.order("-createdAt").get(findOptions);
  }

  @Override
  public WorkflowExecution fetchWorkflowExecution(
      String appId, List<String> serviceIds, List<String> envIds, String workflowId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .filter(WorkflowExecutionKeys.workflowType, ORCHESTRATION)
        .filter(WorkflowExecutionKeys.workflowId, workflowId)
        .filter("appId", appId)
        .filter(WorkflowExecutionKeys.status, SUCCESS)
        .field("serviceIds")
        .in(serviceIds)
        .field("envIds")
        .in(envIds)
        .order(Sort.descending(WorkflowExecutionKeys.createdAt))
        .get(new FindOptions().skip(1));
  }

  @Override
  public boolean verifyAuthorizedToAcceptOrReject(List<String> userGroupIds, List<String> appIds, String workflowId) {
    User user = UserThreadLocal.get();
    if (user == null) {
      return true;
    }

    if (isEmpty(userGroupIds)) {
      try {
        if (isEmpty(appIds) || isBlank(workflowId)) {
          return true;
        }

        PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.DEPLOYMENT, EXECUTE);
        List<PermissionAttribute> permissionAttributeList = asList(permissionAttribute);

        authHandler.authorize(permissionAttributeList, appIds, workflowId);
        return true;
      } catch (WingsException e) {
        return false;
      }
    } else {
      return userGroupService.verifyUserAuthorizedToAcceptOrRejectApproval(null, userGroupIds);
    }
  }

  @Override
  public List<WorkflowExecution> listWaitingOnDeployments(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution =
        wingsPersistence.getWithAppId(WorkflowExecution.class, appId, workflowExecutionId);
    if (workflowExecution == null || ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
      return new ArrayList<>();
    }
    PageRequestBuilder pageRequestBuilder =
        aPageRequest()
            .addFieldsIncluded(WorkflowExecutionKeys.appId, WorkflowExecutionKeys.status,
                WorkflowExecutionKeys.workflowId, WorkflowExecutionKeys.createdAt, WorkflowExecutionKeys.uuid,
                WorkflowExecutionKeys.startTs, WorkflowExecutionKeys.endTs, WorkflowExecutionKeys.name)
            .addFilter(WorkflowExecutionKeys.appId, EQ, appId)
            .addFilter(WorkflowExecutionKeys.status, IN, ExecutionStatus.activeStatuses().toArray())
            .addFilter(WorkflowExecutionKeys.createdAt, LT_EQ, workflowExecution.getCreatedAt())
            .addOrder(WorkflowExecutionKeys.createdAt, OrderType.ASC)
            .addFilter(WorkflowExecutionKeys.workflowId, EQ, workflowExecution.getWorkflowId());

    if (isNotEmpty(workflowExecution.getInfraMappingIds())) {
      pageRequestBuilder.addFilter(
          WorkflowExecutionKeys.infraMappingIds, IN, workflowExecution.getInfraMappingIds().toArray());
    }
    return wingsPersistence.query(WorkflowExecution.class, pageRequestBuilder.build());
  }

  @Override
  public Long fetchWorkflowExecutionStartTs(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .project(WorkflowExecutionKeys.startTs, true)
                                              .project(WorkflowExecutionKeys.appId, true)
                                              .project(WorkflowExecutionKeys.uuid, true)
                                              .filter(WorkflowExecutionKeys.appId, appId)
                                              .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                              .get();
    return workflowExecution == null ? null : workflowExecution.getStartTs();
  }

  @Override
  public ApprovalAuthorization getApprovalAuthorization(String appId, List<String> userGroupIds) {
    ApprovalAuthorization approvalAuthorization = new ApprovalAuthorization();
    approvalAuthorization.setAuthorized(true);

    if (isNotEmpty(userGroupIds)) {
      if (!verifyAuthorizedToAcceptOrReject(userGroupIds, asList(appId), null)) {
        approvalAuthorization.setAuthorized(false);
      }
    }

    return approvalAuthorization;
  }

  /**
   * Do
   * @param appId
   * @param workflowExecutionId
   * @return
   */
  @Override
  public WorkflowExecution getWorkflowExecutionSummary(String appId, String workflowExecutionId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .project(WorkflowExecutionKeys.serviceExecutionSummaries, false)
        .project(WorkflowExecutionKeys.executionArgs, false)
        .filter(WorkflowExecutionKeys.appId, appId)
        .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
        .get();
  }

  @Override
  public WorkflowExecution getWorkflowExecutionForVerificationService(String appId, String workflowExecutionId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .project(WorkflowExecutionKeys.uuid, true)
        .project(WorkflowExecutionKeys.envId, true)
        .filter(WorkflowExecutionKeys.appId, appId)
        .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
        .get();
  }

  @Override
  public void refreshCollectedArtifacts(String appId, String pipelineExecutionId, String workflowExecutionId) {
    WorkflowExecution pipelineWorkflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                                      .project(WorkflowExecutionKeys.artifacts, true)
                                                      .filter(WorkflowExecutionKeys.appId, appId)
                                                      .filter(WorkflowExecutionKeys.uuid, pipelineExecutionId)
                                                      .get();

    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .project(WorkflowExecutionKeys.artifacts, true)
                                              .filter(WorkflowExecutionKeys.appId, appId)
                                              .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                              .get();

    // Workflow Step Artifacts
    List<Artifact> artifacts = workflowExecution.getArtifacts();
    if (isEmpty(artifacts)) {
      return;
    }

    // Pipeline Execution Artifacts collected so far
    List<Artifact> collectedArtifacts = pipelineWorkflowExecution.getArtifacts();
    if (collectedArtifacts == null) {
      pipelineWorkflowExecution.setArtifacts(new ArrayList<>());
      collectedArtifacts = pipelineWorkflowExecution.getArtifacts();
    }

    Set<String> collectedArtifactIds = collectedArtifacts.stream().map(Artifact::getUuid).collect(Collectors.toSet());
    Set<String> artifactIds = artifacts.stream().map(Artifact::getUuid).collect(Collectors.toSet());
    if (collectedArtifactIds.containsAll(artifactIds)) {
      return;
    }

    collectedArtifacts.addAll(artifacts);
    collectedArtifacts = collectedArtifacts.stream().filter(distinctByKey(Base::getUuid)).collect(toList());

    Query<WorkflowExecution> updatedQuery = wingsPersistence.createQuery(WorkflowExecution.class)
                                                .project(WorkflowExecutionKeys.artifacts, true)
                                                .filter(WorkflowExecutionKeys.appId, appId)
                                                .filter(WorkflowExecutionKeys.uuid, pipelineExecutionId);

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set(WorkflowExecutionKeys.artifacts, collectedArtifacts);

    wingsPersistence.update(updatedQuery, updateOps);
  }

  @Override
  public StateMachine obtainStateMachine(WorkflowExecution workflowExecution) {
    if (workflowExecution.getStateMachine() != null) {
      return workflowExecution.getStateMachine();
    }
    logger.warn("Workflow execution {} do not have inline state machine", workflowExecution.getUuid());
    return wingsPersistence.getWithAppId(
        StateMachine.class, workflowExecution.getAppId(), workflowExecution.getStateMachineId());
  }

  @Override
  public WorkflowExecution fetchLastWorkflowExecution(String appId, String workflowId, String serviceId, String envId) {
    Query<WorkflowExecution> workflowExecutionQuery = wingsPersistence.createQuery(WorkflowExecution.class)
                                                          .filter(WorkflowExecutionKeys.workflowType, ORCHESTRATION)
                                                          .filter(WorkflowExecutionKeys.workflowId, workflowId)
                                                          .filter(WorkflowExecutionKeys.appId, appId);

    if (StringUtils.isNotBlank(serviceId)) {
      workflowExecutionQuery.field(WorkflowExecutionKeys.serviceIds).in(Arrays.asList(serviceId));
    }

    if (StringUtils.isNotBlank(envId)) {
      workflowExecutionQuery.field(WorkflowExecutionKeys.envIds).in(Arrays.asList(envId));
    }

    return workflowExecutionQuery.order(Sort.descending(WorkflowExecutionKeys.createdAt)).get();
  }

  @Override
  public PageResponse<WorkflowExecution> fetchWorkflowExecutionList(
      String appId, String workflowId, String envId, int pageOffset, int pageLimit) {
    PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .addFilter(WorkflowExecutionKeys.workflowType, Operator.EQ, ORCHESTRATION)
            .addFilter(WorkflowExecutionKeys.workflowId, Operator.EQ, workflowId)
            .addFilter(WorkflowExecutionKeys.appId, Operator.EQ, appId)
            .addFilter(WorkflowExecutionKeys.envIds, Operator.IN, Arrays.asList(envId))
            .withLimit(String.valueOf(pageLimit))
            .withOffset(String.valueOf(pageOffset))
            .addOrder(aSortOrder().withField(WorkflowExecutionKeys.createdAt, OrderType.DESC).build())
            .build();

    return wingsPersistence.query(WorkflowExecution.class, pageRequest);
  }

  @Override
  public String getApplicationIdByExecutionId(String executionId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .filter(WorkflowExecutionKeys.uuid, executionId)
        .project(WorkflowExecutionKeys.appId, true)
        .get()
        .getAppId();
  }

  @Override
  public List<WorkflowExecution> getLastSuccessfulWorkflowExecutions(
      String appId, String workflowId, String serviceId) {
    final PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .addFilter(WorkflowExecutionKeys.appId, Operator.EQ, appId)
            .addFilter(WorkflowExecutionKeys.workflowId, Operator.EQ, workflowId)
            .addFilter(WorkflowExecutionKeys.status, Operator.EQ, ExecutionStatus.SUCCESS)
            .addOrder(WorkflowExecutionKeys.createdAt, OrderType.DESC)
            .build();
    if (!isEmpty(serviceId)) {
      pageRequest.addFilter(WorkflowExecutionKeys.serviceIds, Operator.CONTAINS, serviceId);
    }
    final PageResponse<WorkflowExecution> workflowExecutions = listExecutions(pageRequest, false, true, false, false);
    if (workflowExecutions != null) {
      return workflowExecutions.getResponse();
    }
    return null;
  }

  @Override
  public boolean appendInfraMappingId(String appId, String workflowExecutionId, String infraMappingId) {
    boolean modified = false;
    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    List<String> infraMappingIds = workflowExecution.getInfraMappingIds();
    if (isNotEmpty(infraMappingIds)) {
      if (!infraMappingIds.contains(infraMappingId)) {
        infraMappingIds.add(infraMappingId);
        modified = true;
      }
    } else {
      infraMappingIds = new ArrayList<>();
      infraMappingIds.add(infraMappingId);
      modified = true;
    }
    if (modified) {
      try {
        Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                             .filter(WorkflowExecutionKeys.appId, workflowExecution.getAppId())
                                             .filter(ID_KEY, workflowExecution.getUuid());

        UpdateOperations<WorkflowExecution> updateOps =
            wingsPersistence.createUpdateOperations(WorkflowExecution.class).set("infraMappingIds", infraMappingIds);
        UpdateResults updateResults = wingsPersistence.update(query, updateOps);
        return updateResults != null && updateResults.getWriteResult() != null
            && updateResults.getWriteResult().getN() > 0;

      } catch (Exception ex) {
        return false;
      }
    } else {
      return true;
    }
  }

  @Override
  public boolean isTriggerBasedDeployment(ExecutionContext context) {
    WorkflowExecution workflowExecution = getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    if (workflowExecution != null && workflowExecution.getTriggeredBy() != null
        && workflowExecution.getTriggeredBy().getName().contains("Deployment Trigger")) {
      return true;
    }
    return false;
  }

  @Override
  public List<WorkflowExecution> getLatestExecutionsFor(
      String appId, String infraMappingId, int limit, List<String> fieldList, boolean forInclusion) {
    final Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                               .filter(WorkflowExecutionKeys.appId, appId)
                                               .filter(WorkflowExecutionKeys.workflowType, ORCHESTRATION)
                                               .filter(WorkflowExecutionKeys.status, SUCCESS)
                                               .filter(WorkflowExecutionKeys.infraMappingIds, infraMappingId);

    emptyIfNull(fieldList).forEach(field -> query.project(field, forInclusion));

    final List<WorkflowExecution> workflowExecutionList = emptyIfNull(
        query.order(Sort.descending(WorkflowExecutionKeys.createdAt)).asList(new FindOptions().limit(limit)));

    workflowExecutionList.forEach(we -> we.setStateMachine(null));
    return workflowExecutionList;
  }

  @Override
  public ConcurrentExecutionResponse fetchConcurrentExecutions(
      String appId, String workflowExecutionId, String resourceConstraintName, String unit) {
    ResourceConstraint resourceConstraint =
        resourceConstraintService.getByName(appService.getAccountIdByAppId(appId), resourceConstraintName);
    notNullCheck("Resource Constraint not found for name " + resourceConstraintName, resourceConstraint);
    WorkflowExecution execution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("Workflow Execution not found", execution);
    ConcurrentExecutionResponseBuilder responseBuilder = ConcurrentExecutionResponse.builder();
    responseBuilder.unitType(
        execution.getConcurrencyStrategy() != null ? execution.getConcurrencyStrategy().getUnitType() : null);
    responseBuilder.infrastructureDetails(extractServiceInfrastructureDetails(appId, execution));
    if (ExecutionStatus.isRunningStatus(execution.getStatus())) {
      List<ResourceConstraintInstance> instances =
          resourceConstraintService.fetchResourceConstraintInstancesForUnitAndEntityType(
              appId, resourceConstraint.getUuid(), unit, HoldingScope.WORKFLOW.name());
      responseBuilder.state(extractState(workflowExecutionId, instances));
      responseBuilder.executions(fetchWorkflowExecutionsForResourceConstraint(
          appId, instances.stream().map(ResourceConstraintInstance::getReleaseEntityId).collect(Collectors.toList())));
    }
    return responseBuilder.build();
  }

  @Override
  public Map<String, Object> extractServiceInfrastructureDetails(String appId, WorkflowExecution execution) {
    Map<String, Object> infrastructureDetails = new LinkedHashMap<>();

    if (isNotEmpty(execution.getInfraMappingIds())) {
      InfrastructureMapping infrastructureMapping =
          infrastructureMappingService.get(appId, execution.getInfraMappingIds().get(0));

      Service service = serviceResourceService.get(appId, infrastructureMapping.getServiceId());
      infrastructureDetails.put("Service", service.getName());
      infrastructureDetails.put("DeploymentType", service.getDeploymentType());

      infrastructureDetails.put("CloudProvider", infrastructureMapping.getComputeProviderType());
      infrastructureDetails.put("CloudProviderName", infrastructureMapping.getComputeProviderName());
      if (infrastructureMapping instanceof ContainerInfrastructureMapping) {
        infrastructureDetails.put(
            "ClusterName", ((ContainerInfrastructureMapping) infrastructureMapping).getClusterName());
        String namespace = ((ContainerInfrastructureMapping) infrastructureMapping).getNamespace();
        if (namespace != null) {
          infrastructureDetails.put("namespace", namespace);
        }
      }
    }
    return infrastructureDetails;
  }

  private State extractState(String workflowExecutionId, List<ResourceConstraintInstance> instances) {
    Optional<ResourceConstraintInstance> optionalInstance = CollectionUtils.filterAndGetFirst(
        instances, instance -> workflowExecutionId.equals(instance.getReleaseEntityId()));
    if (optionalInstance.isPresent()) {
      ResourceConstraintInstance executionInstance = optionalInstance.get();
      return Consumer.State.valueOf(executionInstance.getState());
    } else {
      return null;
    }
  }

  @Override
  public boolean checkIfOnDemand(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("Workflow Execution is Null", workflowExecution);
    return workflowExecution.isOnDemandRollback();
  }

  @Override
  public List<WorkflowExecution> fetchWorkflowExecutionsForResourceConstraint(String appId, List<String> entityIds) {
    if (EmptyPredicate.isEmpty(entityIds)) {
      return Collections.emptyList();
    }
    final List<WorkflowExecution> workflowExecutions = wingsPersistence.createQuery(WorkflowExecution.class)
                                                           .project(WorkflowExecutionKeys.appId, true)
                                                           .project(WorkflowExecutionKeys.status, true)
                                                           .project(WorkflowExecutionKeys.workflowId, true)
                                                           .project(WorkflowExecutionKeys.createdAt, true)
                                                           .project(WorkflowExecutionKeys.uuid, true)
                                                           .project(WorkflowExecutionKeys.startTs, true)
                                                           .project(WorkflowExecutionKeys.endTs, true)
                                                           .project(WorkflowExecutionKeys.name, true)
                                                           .project(WorkflowExecutionKeys.envId, true)
                                                           .filter(WorkflowExecutionKeys.appId, appId)
                                                           .field(WorkflowExecutionKeys.uuid)
                                                           .in(entityIds)
                                                           .asList();
    workflowExecutions.sort(Comparator.comparing(item -> entityIds.indexOf(item.getUuid())));
    return workflowExecutions;
  }

  /**
   *
   * @param workflowExecution
   * @return only the skeleton of the environment object. Contains only the ID,the type and the name ->
   * Reconstructed from what we see in the pipelineExecution or the workflowExecution
   */
  @Override
  public List<EnvSummary> getEnvironmentsForExecution(WorkflowExecution workflowExecution) {
    Set<EnvSummary> environments = new HashSet<>();
    if (workflowExecution.getPipelineExecution() != null) {
      if (workflowExecution.getPipelineExecution().getPipelineStageExecutions() != null) {
        workflowExecution.getPipelineExecution().getPipelineStageExecutions().forEach(stageExecution
            -> stageExecution.getWorkflowExecutions()
                   .stream()
                   .filter(wfExecution -> wfExecution.getEnvId() != null && wfExecution.getEnvType() != null)
                   .map(wfExecution
                       -> EnvSummary.builder()
                              .uuid(wfExecution.getEnvId())
                              .environmentType(wfExecution.getEnvType())
                              .name(wfExecution.getEnvName())
                              .build())
                   .forEach(environments::add));
      }
    } else {
      if (workflowExecution.getEnvId() != null && workflowExecution.getEnvType() != null) {
        environments.add(EnvSummary.builder()
                             .environmentType(workflowExecution.getEnvType())
                             .uuid(workflowExecution.getEnvId())
                             .name(workflowExecution.getEnvName())
                             .build());
      }
    }
    return environments.stream().collect(Collectors.toList());
  }

  /**
   *
   * @param workflowExecution
   * @return only the serviceIds that have been deployed in the pipeline or workflowExecution ->
   * Reconstructed from what we see in the pipelineExecution or the workflowExecution
   */
  @Override
  public List<String> getServiceIdsForExecution(WorkflowExecution workflowExecution) {
    Set<String> serviceIds = new HashSet<>();
    if (workflowExecution.getPipelineExecution() != null) {
      if (workflowExecution.getPipelineExecution() != null) {
        if (workflowExecution.getPipelineExecution().getPipelineStageExecutions() != null) {
          workflowExecution.getPipelineExecution().getPipelineStageExecutions().forEach(stageExecution
              -> stageExecution.getWorkflowExecutions()
                     .stream()
                     .filter(wfExecution -> wfExecution.getServiceIds() != null)
                     .map(WorkflowExecution::getServiceIds)
                     .forEach(serviceIds::addAll));
        }
      }
    } else {
      if (!isNullOrEmpty(workflowExecution.getServiceIds())) {
        serviceIds.addAll(workflowExecution.getServiceIds());
      }
    }

    return new ArrayList<>(serviceIds);
  }

  /**
   *
   * @param workflowExecution
   * @return only the serviceIds that have been deployed in the pipeline or workflowExecution ->
   * Reconstructed from what we see in the pipelineExecution or the workflowExecution
   */
  @Override
  public List<String> getCloudProviderIdsForExecution(WorkflowExecution workflowExecution) {
    Set<String> cloudProviderIds = new HashSet<>();
    if (workflowExecution.getPipelineExecution() != null) {
      if (workflowExecution.getPipelineExecution() != null) {
        if (workflowExecution.getPipelineExecution().getPipelineStageExecutions() != null) {
          workflowExecution.getPipelineExecution().getPipelineStageExecutions().forEach(stageExecution
              -> stageExecution.getWorkflowExecutions()
                     .stream()
                     .filter(wfExecution -> wfExecution.getCloudProviderIds() != null)
                     .map(WorkflowExecution::getCloudProviderIds)
                     .forEach(cloudProviderIds::addAll));
        }
      }
    } else {
      if (!isNullOrEmpty(workflowExecution.getCloudProviderIds())) {
        cloudProviderIds.addAll(workflowExecution.getCloudProviderIds());
      }
    }

    return new ArrayList<>(cloudProviderIds);
  }

  @Override
  public boolean getOnDemandRollbackAvailable(String appId, WorkflowExecution lastWE) {
    if (lastWE.getStatus() != SUCCESS) {
      logger.info("On demand rollback not available for non successful executions {}", lastWE);
      return false;
    }
    if (lastWE.getWorkflowType() == PIPELINE) {
      logger.info("On demand rollback not available for pipeline executions {}", lastWE);
      return false;
    }
    if (lastWE.getEnvType() != EnvironmentType.PROD) {
      logger.info("On demand rollback not available for Non prod environments {}", lastWE);
      return false;
    }
    List<String> infraDefId = lastWE.getInfraDefinitionIds();
    if (isEmpty(infraDefId) || infraDefId.size() != 1) {
      // Only allowing on demand rollback for workflow deploying single infra definition.
      logger.info("On demand rollback not available, Infra definition size not equal to 1 {}", lastWE);
      return false;
    } else {
      InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.get(appId, infraDefId.get(0));
      return infrastructureDefinition != null && rollbackEnabledForDeploymentType(infrastructureDefinition);
    }
  }

  private boolean rollbackEnabledForDeploymentType(InfrastructureDefinition infrastructureDefinition) {
    return DeploymentType.PCF == infrastructureDefinition.getDeploymentType()
        || ((DeploymentType.SSH == infrastructureDefinition.getDeploymentType()
                || DeploymentType.WINRM == infrastructureDefinition.getDeploymentType())
               && featureFlagService.isEnabled(
                      SSH_WINRM_SO, appService.getAccountIdByAppId(infrastructureDefinition.getAppId())));
  }

  @Override
  public WorkflowExecutionInfo getWorkflowExecutionInfo(String workflowExecutionId) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
    if (workflowExecution == null) {
      throw new InvalidRequestException("Couldn't find a workflow Execution with Id: " + workflowExecutionId, USER);
    }

    WorkflowExecutionInfoBuilder workflowExecutionInfoBuilder = WorkflowExecutionInfo.builder()
                                                                    .accountId(workflowExecution.getAccountId())
                                                                    .name(workflowExecution.getName())
                                                                    .appId(workflowExecution.getAppId())
                                                                    .executionId(workflowExecutionId)
                                                                    .workflowId(workflowExecution.getWorkflowId())
                                                                    .startTs(workflowExecution.getStartTs());

    if (workflowExecution.getRollbackStartTs() != null) {
      ExecutionInterrupt executionInterrupt =
          wingsPersistence.createQuery(ExecutionInterrupt.class)
              .filter(ExecutionInterruptKeys.appId, workflowExecution.getAppId())
              .filter(ExecutionInterruptKeys.executionUuid, workflowExecutionId)
              .filter(ExecutionInterruptKeys.executionInterruptType, String.valueOf(ExecutionInterruptType.ROLLBACK))
              .order(Sort.descending(ExecutionInterruptKeys.createdAt))
              .get();

      RollbackType rollbackType;
      String rollbackStateExecutionId = null;
      if (executionInterrupt != null) {
        rollbackType = RollbackType.MANUAL;
        rollbackStateExecutionId = executionInterrupt.getStateExecutionInstanceId();
      } else {
        rollbackType = RollbackType.AUTO;
        Query<StateExecutionInstance> query =
            wingsPersistence.createQuery(StateExecutionInstance.class)
                .filter(StateExecutionInstanceKeys.appId, workflowExecution.getAppId())
                .filter(StateExecutionInstanceKeys.executionUuid, workflowExecutionId)
                .field(StateExecutionInstanceKeys.status)
                .in(Arrays.asList(FAILED, ERROR))
                .order(Sort.descending(StateExecutionInstanceKeys.createdAt))
                .project(StateExecutionInstanceKeys.uuid, true)
                .project(StateExecutionInstanceKeys.stateName, true)
                .project(StateExecutionInstanceKeys.status, true);

        StateExecutionInstance failedInstance = query.get();
        if (failedInstance != null) {
          rollbackStateExecutionId = failedInstance.getUuid();
        }
      }

      RollbackWorkflowExecutionInfo rollbackWorkflowExecutionInfo =
          RollbackWorkflowExecutionInfo.builder()
              .rollbackStartTs(workflowExecution.getRollbackStartTs())
              .rollbackDuration(workflowExecution.getRollbackDuration())
              .rollbackType(rollbackType)
              .rollbackStateExecutionId(rollbackStateExecutionId)
              .build();

      workflowExecutionInfoBuilder.rollbackWorkflowExecutionInfo(rollbackWorkflowExecutionInfo);
    }

    return workflowExecutionInfoBuilder.build();
  }
}
