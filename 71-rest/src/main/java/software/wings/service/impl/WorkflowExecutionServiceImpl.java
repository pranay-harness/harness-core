/**
 *
 */

package software.wings.service.impl;

import static com.google.common.collect.Lists.newArrayList;
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
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimList;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeValidate;
import static io.harness.persistence.ReadPref.CRITICAL;
import static java.lang.String.format;
import static java.time.Duration.ofDays;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ApprovalDetails.Action.APPROVE;
import static software.wings.beans.ApprovalDetails.Action.REJECT;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.EntityType.DEPLOYMENT;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.WorkflowExecution.STATUS_KEY;
import static software.wings.beans.WorkflowExecution.WORKFLOW_ID_KEY;
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
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import io.harness.beans.WorkflowType;
import io.harness.cache.Distributable;
import io.harness.cache.Ordinal;
import io.harness.context.ContextElementType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.limits.checker.LimitApproachingException;
import io.harness.limits.checker.UsageLimitExceededException;
import io.harness.lock.PersistentLocker;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.queue.Queue;
import io.harness.serializer.KryoUtils;
import io.harness.serializer.MapperUtils;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.CanaryWorkflowStandardParams;
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
import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.ApprovalAuthorization;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CanaryWorkflowExecutionAdvisor;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.CustomOrchestrationWorkflow;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EntityVersion;
import software.wings.beans.EntityVersion.ChangeType;
import software.wings.beans.EnvSummary;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.StateExecutionElement;
import software.wings.beans.StateExecutionInterrupt;
import software.wings.beans.SweepingOutput.Scope;
import software.wings.beans.User;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DeploymentRateApproachingLimitAlert;
import software.wings.beans.alert.UsageLimitExceededAlert;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.beans.trigger.Trigger;
import software.wings.common.Constants;
import software.wings.common.cache.MongoStore;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.WorkflowExecutionServiceImpl.Tree.TreeBuilder;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.BarrierService.OrchestrationWorkflowInfo;
import software.wings.service.intfc.EntityVersionService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptEffect;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;
import software.wings.utils.Validator;

import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
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
 *
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionServiceImpl.class);

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
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private PipelineService pipelineService;
  @Inject private ExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Queue<ExecutionEvent> executionEventQueue;
  @Inject private WorkflowExecutionBaselineService workflowExecutionBaselineService;
  @Inject private EntityVersionService entityVersionService;
  @Inject private MongoStore mongoStore;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserGroupService userGroupService;
  @Inject private AuthHandler authHandler;
  @Inject private SweepingOutputService sweepingOutputService;
  @Inject private LimitCheckerFactory limitCheckerFactory;
  @Inject private PreDeploymentChecker preDeploymentChecker;
  @Inject private AlertService alertService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;

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
          populateNodeHierarchy(workflowExecution, includeGraph, includeStatus, false, emptySet());
        } catch (Exception e) {
          logger.error(format("Failed to populate node hierarchy for the workflow execution %s", res.toString()), e);
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
                                           .filter("appId", workflowExecution.getAppId())
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
    approvalDetails.setApprovedBy(EmbeddedUser.builder().email(user.getEmail()).name(user.getName()).build());
    ApprovalStateExecutionData executionData = ApprovalStateExecutionData.builder()
                                                   .approvalId(approvalDetails.getApprovalId())
                                                   .approvedBy(approvalDetails.getApprovedBy())
                                                   .comments(approvalDetails.getComments())
                                                   .build();

    if (approvalDetails.getAction().equals(APPROVE)) {
      executionData.setStatus(ExecutionStatus.SUCCESS);
    } else if (approvalDetails.getAction().equals(REJECT)) {
      executionData.setStatus(ExecutionStatus.REJECTED);
    }

    waitNotifyEngine.notify(approvalDetails.getApprovalId(), executionData);
    return true;
  }

  @Override
  public ApprovalStateExecutionData fetchApprovalStateExecutionDataFromWorkflowExecution(
      String appId, String workflowExecutionId, String stateExecutionId, ApprovalDetails approvalDetails) {
    notNullCheck("appId", appId, USER);

    notNullCheck("ApprovalDetails", approvalDetails, USER);
    notNullCheck("ApprovalId", approvalDetails.getApprovalId());
    notNullCheck("Approval action", approvalDetails.getAction());
    String approvalId = approvalDetails.getApprovalId();

    notNullCheck("workflowExecutionId", workflowExecutionId, USER);
    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("workflowExecution", workflowExecution, USER);

    ApprovalStateExecutionData approvalStateExecutionData = null;
    String workflowType = "";

    // Pipeline approval
    if (workflowExecution.getWorkflowType().equals(WorkflowType.PIPELINE)) {
      workflowType = "Pipeline";
      approvalStateExecutionData =
          fetchPipelineWaitingApprovalStateExecutionData(workflowExecution.getPipelineExecution(), approvalId);
    }

    // Workflow approval
    if (workflowExecution.getWorkflowType().equals(WorkflowType.ORCHESTRATION)) {
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

        if (pe.getStatus().equals(ExecutionStatus.PAUSED)
            && approvalStateExecutionData.getStatus().equals(ExecutionStatus.PAUSED)
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
          (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionData();
      // Check for Approval Id in PAUSED status
      if (approvalStateExecutionData != null && approvalStateExecutionData.getStatus().equals(ExecutionStatus.PAUSED)
          && approvalStateExecutionData.getApprovalId().equals(approvalId)) {
        return approvalStateExecutionData;
      }
    } else {
      List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(workflowExecution);
      for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
        if (stateExecutionInstance.getStateExecutionData() instanceof ApprovalStateExecutionData) {
          ApprovalStateExecutionData approvalStateExecutionData =
              (ApprovalStateExecutionData) stateExecutionInstance.getStateExecutionData();
          // Check for Approval Id in PAUSED status
          if (approvalStateExecutionData != null
              && approvalStateExecutionData.getStatus().equals(ExecutionStatus.PAUSED)
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
            StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();

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
            StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();

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
                                           .filter("appId", workflowExecution.getAppId())
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
            .map(we -> we.getPipelineExecution())
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
                                .filter("appId", workflowExecution.getAppId())
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
    return Maps.uniqueIndex(stateExecutionInstances, v -> v.getDisplayName());
  }

  private List<StateExecutionInstance> getStateExecutionInstances(WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> req =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("appId", EQ, workflowExecution.getAppId())
            .addFilter("executionUuid", EQ, workflowExecution.getUuid())
            .addFilter(StateExecutionInstance.CREATED_AT_KEY, GE, workflowExecution.getCreatedAt())
            .build();
    return wingsPersistence.query(StateExecutionInstance.class, req).getResponse();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(
      String appId, String workflowExecutionId, boolean upToDate, Set<String> excludeFromAggregation) {
    WorkflowExecution workflowExecution = getExecutionDetailsWithoutGraph(appId, workflowExecutionId);

    if (workflowExecution.getWorkflowType() == PIPELINE) {
      populateNodeHierarchy(workflowExecution, false, true, upToDate, excludeFromAggregation);
    } else {
      populateNodeHierarchy(workflowExecution, true, false, upToDate, excludeFromAggregation);
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
        executionArgs.setArtifacts(artifactService.fetchArtifacts(appId, executionArgs.getArtifactIdNames().keySet()));
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
  static class Tree implements Distributable, Ordinal {
    public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(Tree.class).getSerialVersionUID();

    private long contextOrder;
    private String key;

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
  }

  private Tree calculateTree(String appId, String workflowExecutionId) {
    Map<String, StateExecutionInstance> allInstancesIdMap =
        stateExecutionService.executionStatesMap(appId, workflowExecutionId);

    Long lastUpdate = allInstancesIdMap.values()
                          .stream()
                          .map(StateExecutionInstance::getLastUpdatedAt)
                          .max(Long::compare)
                          .orElseGet(() -> Long.valueOf(0));

    Tree tree = mongoStore.get(GraphRenderer.algorithmId, Tree.STRUCTURE_HASH, workflowExecutionId);
    if (tree != null && tree.getContextOrder() >= lastUpdate) {
      return tree;
    }

    TreeBuilder treeBuilder = Tree.builder().key(workflowExecutionId).contextOrder(lastUpdate);
    if (allInstancesIdMap.values().stream().anyMatch(
            i -> i.getStatus() == ExecutionStatus.PAUSED || i.getStatus() == ExecutionStatus.PAUSING)) {
      treeBuilder.overrideStatus(ExecutionStatus.PAUSED);
    } else if (allInstancesIdMap.values().stream().anyMatch(i -> i.getStatus() == ExecutionStatus.WAITING)) {
      treeBuilder.overrideStatus(ExecutionStatus.WAITING);
    } else {
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
                   e -> e.getExecutionInterruptType() == ExecutionInterruptType.PAUSE_ALL)) {
          treeBuilder.overrideStatus(ExecutionStatus.PAUSED);
        }
      }
    }

    treeBuilder.graph(graphRenderer.generateHierarchyNode(allInstancesIdMap, emptySet()));

    final Tree cacheTree = treeBuilder.build();
    executorService.submit(() -> { mongoStore.upsert(cacheTree, ofDays(30)); });
    return cacheTree;
  }

  @Override
  public WorkflowExecution getWorkflowExecution(String appId, String workflowExecutionId) {
    logger.debug("Retrieving workflow execution details for id {} of App Id {} ", workflowExecutionId, appId);
    return wingsPersistence.getWithAppId(WorkflowExecution.class, appId, workflowExecutionId);
  }

  private void populateNodeHierarchy(WorkflowExecution workflowExecution, boolean includeGraph, boolean includeStatus,
      boolean upToDate, Set<String> excludeFromAggregation) {
    if (includeGraph) {
      includeStatus = true;
    }

    if (!includeStatus && !includeGraph) {
      return;
    }

    Tree tree = null;
    if (!upToDate) {
      tree = mongoStore.<Tree>get(GraphRenderer.algorithmId, Tree.STRUCTURE_HASH, workflowExecution.getUuid());
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
    for (PipelineStage stage : pipeline.getPipelineStages()) {
      if (!stage.isParallel()) {
        if (!isEmpty(orchestrationWorkflows)) {
          barrierService.obtainInstances(pipeline.getAppId(), orchestrationWorkflows, pipelineExecutionId)
              .forEach(barrier -> barrierService.save(barrier));
        }
        orchestrationWorkflows = new ArrayList<>();
      }

      // this array is legacy, we always have just one item
      final PipelineStageElement element = stage.getPipelineStageElements().get(0);

      if (element.isDisable()) {
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
                                       .pipelineStateId(element.getUuid())
                                       .orchestrationWorkflow(workflow.getOrchestrationWorkflow())
                                       .build());
      }
    }

    if (!isEmpty(orchestrationWorkflows)) {
      barrierService.obtainInstances(pipeline.getAppId(), orchestrationWorkflows, pipelineExecutionId)
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
  public WorkflowExecution triggerPipelineExecution(String appId, String pipelineId, ExecutionArgs executionArgs,
      WorkflowExecutionUpdate workflowExecutionUpdate, Trigger trigger) {
    preDeploymentChecker.isDeploymentAllowed(appId);
    Pipeline pipeline =
        pipelineService.readPipelineWithResolvedVariables(appId, pipelineId, executionArgs.getWorkflowVariables());
    if (pipeline == null) {
      throw new WingsException(ErrorCode.NON_EXISTING_PIPELINE);
    }
    if (isEmpty(pipeline.getPipelineStages())) {
      throw new WingsException("You can not deploy an empty pipeline.", WingsException.USER);
    }

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

    StateMachine stateMachine = new StateMachine(pipeline, workflowService.stencilMap());
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
                .withContextElement(aServiceElement().withUuid(service.getUuid()).withName(service.getName()).build())
                .build());
      });
      workflowExecution.setServiceExecutionSummaries(serviceExecutionSummaries);
      workflowExecution.setServiceIds(pipeline.getServices().stream().map(Service::getUuid).collect(toList()));
    }
    workflowExecution.setEnvIds(pipeline.getEnvIds());
    workflowExecution.setInfraMappingIds(pipeline.getInfraMappingIds());
    return triggerExecution(
        workflowExecution, stateMachine, workflowExecutionUpdate, stdParams, trigger, pipeline, null);
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
    preDeploymentChecker.isDeploymentAllowed(appId);

    // TODO - validate list of artifact Ids if it's matching for all the services involved in this orchestration

    Workflow workflow = workflowService.readWorkflow(appId, workflowId);

    if (!workflow.getOrchestrationWorkflow().isValid()) {
      throw new InvalidRequestException("Workflow requested for execution is not valid/complete.");
    }

    StateMachine stateMachine = new StateMachine(workflow, workflow.getDefaultVersion(),
        ((CustomOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getGraph(), workflowService.stencilMap());

    // TODO: this is workaround for a side effect in the state machine generation that mangles with the original
    //       workflow object.
    workflow = workflowService.readWorkflow(appId, workflowId);

    stateMachine.setOrchestrationWorkflow(null);

    WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    workflowExecution.setAppId(appId);
    String resolveEnvId = workflowService.resolveEnvironmentId(workflow, executionArgs.getWorkflowVariables());
    if (resolveEnvId != null) {
      envId = resolveEnvId;
      workflowExecution.setEnvId(resolveEnvId);
      workflowExecution.setEnvIds(Collections.singletonList(resolveEnvId));
    }
    workflowExecution.setWorkflowId(workflowId);
    workflowExecution.setName(workflow.getName());
    workflowExecution.setWorkflowType(ORCHESTRATION);
    workflowExecution.setOrchestrationType(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
    workflowExecution.setStateMachine(stateMachine);
    workflowExecution.setPipelineExecutionId(pipelineExecutionId);
    workflowExecution.setExecutionArgs(executionArgs);

    Map<String, String> workflowVariables = executionArgs.getWorkflowVariables();
    List<Service> services = workflowService.getResolvedServices(workflow, workflowVariables);
    if (isNotEmpty(services)) {
      workflowExecution.setServiceIds(services.stream().map(Service::getUuid).collect(toList()));
    }

    workflowExecution.setInfraMappingIds(workflowService.getResolvedInfraMappingIds(workflow, workflowVariables));

    WorkflowStandardParams stdParams;
    if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.CANARY
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.BASIC
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.ROLLING
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.MULTI_SERVICE
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == BUILD
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.BLUE_GREEN) {
      stdParams = new CanaryWorkflowStandardParams();

      if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
            (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
        if (canaryOrchestrationWorkflow.getUserVariables() != null) {
          stdParams.setWorkflowElement(WorkflowElement.builder()
                                           .variables(getWorkflowVariables(canaryOrchestrationWorkflow, executionArgs))
                                           .build());
        }
      }
    } else {
      stdParams = new WorkflowStandardParams();
    }

    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    if (isNotEmpty(executionArgs.getArtifacts())) {
      stdParams.setArtifactIds(executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(toList()));
    }

    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    stdParams.setExcludeHostsWithSameArtifact(executionArgs.isExcludeHostsWithSameArtifact());
    stdParams.setNotifyTriggeredUserOnly(executionArgs.isNotifyTriggeredUserOnly());

    return triggerExecution(workflowExecution, stateMachine, new CanaryWorkflowExecutionAdvisor(),
        workflowExecutionUpdate, stdParams, trigger, null, workflow);
  }

  private Map<String, Object> getWorkflowVariables(
      CanaryOrchestrationWorkflow orchestrationWorkflow, ExecutionArgs executionArgs) {
    Map<String, Object> variables = new HashMap<>();
    if (orchestrationWorkflow.getUserVariables() == null) {
      return variables;
    }
    for (Variable variable : orchestrationWorkflow.getUserVariables()) {
      if (variable.isFixed()) {
        setVariables(variable.getName(), variable.getValue(), variables);
        continue;
      }

      // no input from user
      if (executionArgs == null || isEmpty(executionArgs.getWorkflowVariables())
          || isBlank(executionArgs.getWorkflowVariables().get(variable.getName()))) {
        if (variable.isMandatory() && isBlank(variable.getValue())) {
          throw new InvalidRequestException(
              "Workflow variable [" + variable.getName() + "] is mandatory for execution", USER);
        }
        if (isBlank(variable.getValue())) {
          setVariables(variable.getName(), "", variables);
        } else {
          setVariables(variable.getName(), variable.getValue(), variables);
        }
        continue;
      }
      setVariables(variable.getName(), executionArgs.getWorkflowVariables().get(variable.getName()), variables);
    }
    return variables;
  }

  private void setVariables(String key, Object value, Map<String, Object> variableMap) {
    if (!isNull(key)) {
      variableMap.put(key, value);
    }
  }

  private boolean isNull(String string) {
    return isEmpty(string) || string.equals("null");
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
    List<Object> keywords = newArrayList(workflowExecution.normalizedName(), workflowExecution.getWorkflowType(),
        workflowExecution.getOrchestrationType());

    ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();

    populateArtifactsAndServices(workflowExecution, stdParams, keywords, executionArgs);

    populatePipelineSummary(workflowExecution, keywords, executionArgs);

    Application app = appService.get(workflowExecution.getAppId());
    workflowExecution.setAppName(app.getName());
    keywords.add(workflowExecution.getAppName());

    refreshEnvSummary(workflowExecution, keywords);

    populateCurrentUser(workflowExecution, stdParams, trigger, keywords);

    populateServiceInstances(workflowExecution, keywords, executionArgs);

    workflowExecution.setErrorStrategy(executionArgs.getErrorStrategy());

    workflowExecution.setKeywords(trimList(keywords));
    workflowExecution.setStatus(QUEUED);

    EntityVersion entityVersion = entityVersionService.newEntityVersion(workflowExecution.getAppId(), DEPLOYMENT,
        workflowExecution.getWorkflowId(), workflowExecution.displayName(), ChangeType.CREATED);

    workflowExecution.setReleaseNo(String.valueOf(entityVersion.getVersion()));
    wingsPersistence.save(workflowExecution);

    logger.info("Created workflow execution {}", workflowExecution.getUuid());

    updateWorkflowElement(workflowExecution, stdParams, workflow);

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
    stateExecutionInstance.setPipelineStateElementId(executionArgs.getPipelinePhaseElementId());

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

    stateExecutionInstance = stateMachineExecutor.queue(stateMachine, stateExecutionInstance);

    WorkflowExecution savedWorkflowExecution;
    if (workflowExecution.getWorkflowType() != ORCHESTRATION) {
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
                                   .build());
      savedWorkflowExecution = wingsPersistence.getWithAppId(
          WorkflowExecution.class, workflowExecution.getAppId(), workflowExecution.getUuid());
    }

    return savedWorkflowExecution;
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
                                       workflowExecution.getUuid(), null, null, Scope.PIPELINE)
                                   .name("pipeline")
                                   .output(KryoUtils.asDeflatedBytes(pipelineElement))
                                   .build());
  }

  private void updateWorkflowElement(
      WorkflowExecution workflowExecution, WorkflowStandardParams stdParams, Workflow workflow) {
    stdParams.setErrorStrategy(workflowExecution.getErrorStrategy());
    String workflowUrl = mainConfiguration.getPortal().getUrl() + "/"
        + format(mainConfiguration.getPortal().getExecutionUrlPattern(), workflowExecution.getAppId(),
              workflowExecution.getEnvId(), workflowExecution.getUuid());

    if (stdParams.getWorkflowElement() == null) {
      stdParams.setWorkflowElement(WorkflowElement.builder()
                                       .uuid(workflowExecution.getUuid())
                                       .name(workflowExecution.normalizedName())
                                       .url(workflowUrl)
                                       .displayName(workflowExecution.displayName())
                                       .releaseNo(workflowExecution.getReleaseNo())
                                       .build());
    } else {
      stdParams.getWorkflowElement().setName(workflowExecution.normalizedName());
      stdParams.getWorkflowElement().setUuid(workflowExecution.getUuid());
      stdParams.getWorkflowElement().setUrl(workflowUrl);
      stdParams.getWorkflowElement().setDisplayName(workflowExecution.displayName());
      stdParams.getWorkflowElement().setReleaseNo(workflowExecution.getReleaseNo());
    }
    stdParams.getWorkflowElement().setPipelineDeploymentUuid(workflowExecution.getWorkflowType() == PIPELINE
            ? workflowExecution.getUuid()
            : workflowExecution.getPipelineExecutionId());
    lastGoodReleaseInfo(stdParams.getWorkflowElement(), workflowExecution);
    stdParams.getWorkflowElement().setDescription(workflow != null ? workflow.getDescription() : null);
  }

  private void populateServiceInstances(
      WorkflowExecution workflowExecution, List<Object> keywords, ExecutionArgs executionArgs) {
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

  private void populateCurrentUser(
      WorkflowExecution workflowExecution, WorkflowStandardParams stdParams, Trigger trigger, List<Object> keywords) {
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
    }
    if (workflowExecution.getCreatedBy() != null) {
      keywords.add(workflowExecution.getCreatedBy().getName());
      keywords.add(workflowExecution.getCreatedBy().getEmail());
    }
    stdParams.setCurrentUser(workflowExecution.getCreatedBy());
  }

  private void refreshEnvSummary(WorkflowExecution workflowExecution, List<Object> keywords) {
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
          keywords.add(workflowExecution.getEnvType());
          keywords.add(workflowExecution.getEnvName());
        }
      }
    }
  }

  private void populatePipelineSummary(
      WorkflowExecution workflowExecution, List<Object> keywords, ExecutionArgs executionArgs) {
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
                .project(WorkflowExecution.TRIGGERED_BY, true)
                .project(WorkflowExecution.CREATED_BY_KEY, true)
                .project(WorkflowExecution.DEPLOYMENT_TRIGGERED_ID_KEY, true)
                .project(WorkflowExecution.ARTIFACTS_KEY, true)
                .filter(WorkflowExecution.APP_ID_KEY, workflowExecution.getAppId())
                .filter(WorkflowExecution.ID_KEY, workflowExecution.getPipelineExecutionId())
                .get();
        if (pipelineExecution != null) {
          workflowExecution.setTriggeredBy(pipelineExecution.getTriggeredBy());
          workflowExecution.setDeploymentTriggerId(pipelineExecution.getDeploymentTriggerId());
          workflowExecution.setCreatedBy(pipelineExecution.getCreatedBy());
        }
      }
    }
  }

  private void populateArtifactsAndServices(WorkflowExecution workflowExecution, WorkflowStandardParams stdParams,
      List<Object> keywords, ExecutionArgs executionArgs) {
    if (isNotEmpty(executionArgs.getArtifacts())) {
      List<String> artifactIds = executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(toList());

      PageRequest<Artifact> pageRequest = aPageRequest()
                                              .addFilter("appId", EQ, workflowExecution.getAppId())
                                              .addFilter("uuid", IN, artifactIds.toArray())
                                              .build();
      List<Artifact> artifacts = artifactService.list(pageRequest, false).getResponse();

      if (artifacts == null || artifacts.size() != artifactIds.size()) {
        logger.error("Artifact argument and valid artifact retrieved size not matching");
        throw new InvalidRequestException("Invalid artifact");
      }

      // Filter out the artifacts that does not belong to the workflow
      List<String> serviceIds =
          isEmpty(workflowExecution.getServiceIds()) ? new ArrayList<>() : workflowExecution.getServiceIds();
      List<Artifact> filteredArtifacts = new ArrayList<>();
      for (Artifact artifact : artifacts) {
        if (serviceIds.containsAll(artifact.getServiceIds())) {
          filteredArtifacts.add(artifact);
        } else {
          logger.warn("Artifact ServiceId  {} does not contain in serviceIds {}", artifact.getServiceIds(), serviceIds);
        }
      }

      // TODO: get rid of artifactIdNames when UI moves to artifact list
      executionArgs.setArtifactIdNames(
          filteredArtifacts.stream().collect(toMap(Artifact::getUuid, Artifact::getDisplayName)));
      filteredArtifacts.forEach(artifact -> {
        artifact.setArtifactFiles(null);
        artifact.setCreatedBy(null);
        artifact.setLastUpdatedBy(null);
        keywords.add(artifact.getArtifactSourceName());
        keywords.add(artifact.getDescription());
        keywords.add(artifact.getRevision());
        keywords.add(artifact.getMetadata());
      });

      executionArgs.setArtifacts(artifacts);
      workflowExecution.setArtifacts(filteredArtifacts);

      List<ServiceElement> services = new ArrayList<>();
      filteredArtifacts.forEach(artifact -> artifact.getServiceIds().forEach(serviceId -> {
        Service service = serviceResourceService.get(artifact.getAppId(), serviceId, false);
        ServiceElement se = new ServiceElement();
        MapperUtils.mapObject(service, se);
        services.add(se);
        keywords.add(se.getName());
      }));

      // Set the services in the context
      stdParams.setServices(services);
    }
  }

  private void lastGoodReleaseInfo(WorkflowElement workflowElement, WorkflowExecution workflowExecution) {
    WorkflowExecution workflowExecutionLast =
        fetchLastSuccessDeployment(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
    if (workflowExecutionLast != null) {
      workflowElement.setLastGoodDeploymentDisplayName(workflowExecutionLast.displayName());
      workflowElement.setLastGoodDeploymentUuid(workflowExecutionLast.getUuid());
      workflowElement.setLastGoodReleaseNo(workflowExecutionLast.getReleaseNo());
    }
  }

  @Override
  public void updateStartStatus(String appId, String workflowExecutionId, ExecutionStatus status) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecution.APP_ID_KEY, appId)
                                         .filter(WorkflowExecution.ID_KEY, workflowExecutionId)
                                         .field(WorkflowExecution.STATUS_KEY)
                                         .in(asList(NEW, QUEUED));

    UpdateOperations<WorkflowExecution> updateOps =
        wingsPersistence.createUpdateOperations(WorkflowExecution.class)
            .set(WorkflowExecution.STATUS_KEY, status)
            .set(WorkflowExecution.START_TS_KEY, System.currentTimeMillis());

    wingsPersistence.update(query, updateOps);
  }

  @Override
  public WorkflowExecution triggerEnvExecution(
      String appId, String envId, ExecutionArgs executionArgs, Trigger trigger) {
    String accountId = appService.getAccountIdByAppId(appId);

    try {
      WorkflowExecution execution = triggerEnvExecution(appId, envId, executionArgs, null, trigger);
      alertService.closeAlertsOfType(accountId, appId, AlertType.USAGE_LIMIT_EXCEEDED);
      return execution;
    } catch (UsageLimitExceededException e) {
      String errMsg =
          "Deployment rate limit reached. Some deployments may not be allowed. Please contact Harness support.";
      logger.error(e.getMessage());

      // open alert for triggers
      if (executionArgs.getWorkflowType() == WorkflowType.ORCHESTRATION && null != trigger) {
        alertService.openAlert(accountId, appId, AlertType.USAGE_LIMIT_EXCEEDED,
            new UsageLimitExceededAlert(e.getAccountId(), e.getLimit(), errMsg));
      }

      throw new WingsException(ErrorCode.USAGE_LIMITS_EXCEEDED, errMsg);
    }
  }

  @Override
  public void incrementInProgressCount(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.inprogress", inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter("appId", appId)
                                .filter(ID_KEY, workflowExecutionId),
        ops);
  }

  @Override
  public void incrementSuccess(String appId, String workflowExecutionId, int inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.success", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter("appId", appId)
                                .filter(ID_KEY, workflowExecutionId),
        ops);
  }

  @Override
  public void incrementFailed(String appId, String workflowExecutionId, Integer inc) {
    UpdateOperations<WorkflowExecution> ops = wingsPersistence.createUpdateOperations(WorkflowExecution.class);
    ops.inc("breakdown.failed", inc);
    ops.inc("breakdown.inprogress", -1 * inc);
    wingsPersistence.update(wingsPersistence.createQuery(WorkflowExecution.class)
                                .filter("appId", appId)
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
    // logging as JSON because logDNA automatically parses json objects.
    // https://docs.logdna.com/docs/ingestion#section-json-parsing
    JSONObject context = new JSONObject().put("appId", appId);
    String accountId = appService.getAccountIdByAppId(appId);
    context.put("accountId", accountId);

    // This log is used to derive deployment metrics in logDNA, so if you change it, update the logDNA dashboard.
    logger.info("Execution Triggered. {}", context.toString());

    try {
      preDeploymentChecker.checkDeploymentRateLimit(accountId, appId);
      alertService.closeAlertsOfType(accountId, GLOBAL_APP_ID, AlertType.DEPLOYMENT_RATE_APPROACHING_LIMIT);
    } catch (LimitApproachingException e) {
      String errMsg = e.getPercent()
          + "% of Deployment Rate Limit reached. Some deployments may not be allowed beyond 100% usage. Please contact Harness support.";
      logger.error(e.getMessage());
      AlertData alertData = new DeploymentRateApproachingLimitAlert(e.getLimit(), accountId, e.getPercent(), errMsg);
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.DEPLOYMENT_RATE_APPROACHING_LIMIT, alertData);
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
      logger.error(format("Error in interrupting workflowExecution - uuid: %s, executionInterruptType: %s",
                       workflowExecution.getUuid(), executionInterrupt.getExecutionInterruptType()),
          exception);
    }

    List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(workflowExecution);
    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
      StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
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
        logger.error(format("Error in interrupting workflowExecution - uuid: %s, executionInterruptType: %s",
                         workflowExecution.getUuid(), executionInterrupt.getExecutionInterruptType()),
            exception);
      }
    }

    return executionInterrupt;
  }

  @SuppressFBWarnings("RpC_REPEATED_CONDITIONAL_TEST")
  @Override
  public RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs) {
    notNullCheck("workflowType", executionArgs.getWorkflowType());

    if (executionArgs.getWorkflowType() == ORCHESTRATION || executionArgs.getWorkflowType() == ORCHESTRATION) {
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
  public DeploymentMetadata fetchDeploymentMetadata(String appId, ExecutionArgs executionArgs) {
    Validator.notNullCheck("Workflow type is required", executionArgs.getWorkflowType());
    if (executionArgs.getWorkflowType() == ORCHESTRATION) {
      Workflow workflow = workflowService.readWorkflow(appId, executionArgs.getOrchestrationId());
      DeploymentMetadata deploymentMetadata = workflowService.fetchDeploymentMetadata(
          appId, workflow, executionArgs.getWorkflowVariables(), null, null, null);
      // Set Services
      deploymentMetadata.setArtifactRequiredServices(
          serviceResourceService.fetchServicesByUuids(appId, deploymentMetadata.getArtifactRequiredServiceIds()));

      deploymentMetadata.setEnvSummaries(
          environmentService.obtainEnvironmentSummaries(appId, deploymentMetadata.getEnvIds()));

      return deploymentMetadata;
    }
    return DeploymentMetadata.builder().build();
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
    return graphRenderer.convertToNode(stateExecutionInstance);
  }

  @Override
  public List<StateExecutionData> getExecutionHistory(
      String appId, String workflowExecutionId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    return stateExecutionInstance.getStateExecutionDataHistory();
  }

  @Override
  public int getExecutionInterruptCount(String stateExecutionInstanceId) {
    return (int) wingsPersistence.createQuery(ExecutionInterrupt.class)
        .filter("stateExecutionInstanceId", stateExecutionInstanceId)
        .count();
  }

  @Override
  public List<StateExecutionInterrupt> getExecutionInterrupts(String appId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    Validator.notNullCheck("stateExecutionInstance", stateExecutionInstance);

    Map<String, ExecutionInterruptEffect> map = new HashMap<>();
    stateExecutionInstance.getInterruptHistory().forEach(effect -> map.put(effect.getInterruptId(), effect));

    List<StateExecutionInterrupt> interrupts = wingsPersistence.createQuery(ExecutionInterrupt.class)
                                                   .filter(ExecutionInterrupt.APP_ID_KEY, appId)
                                                   .filter("stateExecutionInstanceId", stateExecutionInstanceId)
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
          .filter(ExecutionInterrupt.APP_ID_KEY, appId)
          .field(ID_KEY)
          .in(map.keySet())
          .asList()
          .forEach(interrupt -> {
            final ExecutionInterruptEffect effect = map.get(interrupt.getUuid());
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
    Validator.notNullCheck("stateExecutionInstance", stateExecutionInstance);

    StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
    Validator.notNullCheck("stateExecutionData", stateExecutionData);
    if (!(stateExecutionData instanceof RepeatStateExecutionData)) {
      throw new InvalidRequestException("Request for elements of instance that is not repeated", USER);
    }

    RepeatStateExecutionData repeatStateExecutionData = (RepeatStateExecutionData) stateExecutionData;

    final Map<String, StateExecutionElement> elementMap = repeatStateExecutionData.getRepeatElements()
                                                              .stream()
                                                              .map(element
                                                                  -> StateExecutionElement.builder()
                                                                         .executionContextElementId(element.getUuid())
                                                                         .name(element.getName())
                                                                         .progress(0)
                                                                         .status(STARTING)
                                                                         .build())
                                                              .collect(toMap(StateExecutionElement::getName, x -> x));

    final StateMachine stateMachine = stateExecutionService.obtainStateMachine(stateExecutionInstance);

    int subStates =
        stateMachine.getChildStateMachines().get(stateExecutionInstance.getChildStateMachineId()).getStates().size()
        - 1;

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
                 .filter(StateExecutionInstance.APP_ID_KEY, appId)
                 .filter(StateExecutionInstance.EXECUTION_UUID_KEY, stateExecutionInstance.getExecutionUuid())
                 .filter(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, stateExecutionInstanceId)
                 .project(StateExecutionInstance.ID_KEY, true)
                 .project(StateExecutionInstance.STATUS_KEY, true)
                 .project(StateExecutionInstance.PREV_INSTANCE_ID_KEY, true)
                 .project(StateExecutionInstance.CONTEXT_ELEMENT_KEY, true)
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
          final StateExecutionElement stateExecutionElement = elementMap.get(stat.getElement());
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
  public StateExecutionInstance getStateExecutionData(String appId, String stateExecutionInstanceId) {
    return wingsPersistence.getWithAppId(StateExecutionInstance.class, appId, stateExecutionInstanceId);
  }

  @Override
  public List<StateExecutionInstance> getStateExecutionData(String appId, String executionUuid, String serviceId,
      String infraMappingId, StateType stateType, String stateName) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .addFilter("stateType", EQ, stateType)
            .addFilter("displayName", EQ, stateName)
            .addFilter("contextElement.serviceElement.uuid", EQ, serviceId)
            .addFilter("contextElement.infraMappingId", EQ, infraMappingId)
            .build();

    PageResponse<StateExecutionInstance> query =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest, excludeValidate);
    return query.getResponse();
  }

  private void refreshSummaries(WorkflowExecution workflowExecution) {
    if (workflowExecution.getServiceExecutionSummaries() != null) {
      return;
    }
    List<ElementExecutionSummary> serviceExecutionSummaries = new ArrayList<>();
    // TODO : version should also be captured as part of the WorkflowExecution
    Workflow workflow = workflowService.readWorkflow(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
    if (workflow != null && workflow.getOrchestrationWorkflow() != null) {
      List<Service> services = getResolvedServices(workflow, workflowExecution);
      List<InfrastructureMapping> infrastructureMappings = getResolvedInfraMappings(workflow, workflowExecution);
      if (services != null) {
        services.forEach(service -> {
          ServiceElement serviceElement =
              aServiceElement().withUuid(service.getUuid()).withName(service.getName()).build();
          ElementExecutionSummary elementSummary =
              anElementExecutionSummary().withContextElement(serviceElement).withStatus(ExecutionStatus.QUEUED).build();
          List<InfraMappingSummary> infraMappingSummaries = new ArrayList<>();
          if (infrastructureMappings != null) {
            for (InfrastructureMapping infraMapping : infrastructureMappings) {
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
          serviceExecutionSummaries.add(elementSummary);
        });
      }
    }
    Map<String, ElementExecutionSummary> serviceExecutionSummaryMap = serviceExecutionSummaries.stream().collect(
        toMap(summary -> summary.getContextElement().getUuid(), Function.identity()));

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

  private void populateServiceSummary(
      Map<String, ElementExecutionSummary> serviceSummaryMap, WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(UNLIMITED)
            .addFilter(StateExecutionInstance.APP_ID_KEY, EQ, workflowExecution.getAppId())
            .addFilter(StateExecutionInstance.EXECUTION_UUID_KEY, EQ, workflowExecution.getUuid())
            .addFilter(StateExecutionInstance.STATE_TYPE_KEY, IN, StateType.REPEAT.name(), StateType.FORK.name(),
                StateType.SUB_WORKFLOW.name(), StateType.PHASE.name(), PHASE_STEP.name())
            .addFilter(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, NOT_EXISTS)
            .addOrder(StateExecutionInstance.CREATED_AT_KEY, OrderType.ASC)
            .build();

    PageResponse<StateExecutionInstance> pageResponse =
        wingsPersistence.query(StateExecutionInstance.class, pageRequest);

    if (isEmpty(pageResponse)) {
      return;
    }

    for (StateExecutionInstance stateExecutionInstance : pageResponse.getResponse()) {
      if (!(stateExecutionInstance.getStateExecutionData() instanceof ElementStateExecutionData)) {
        continue;
      }
      if (stateExecutionInstance.isRollback()) {
        continue;
      }

      ElementStateExecutionData elementStateExecutionData =
          (ElementStateExecutionData) stateExecutionInstance.getStateExecutionData();
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
    final ContextElementType elementType = contextElement.getElementType();
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
        if (Constants.PHASE_PARAM.equals(contextElement.getName())) {
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
                                   .filter(StateExecutionInstance.APP_ID_KEY, workflowExecution.getAppId())
                                   .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecution.getUuid())
                                   .project(StateExecutionInstance.CONTEXT_ELEMENT_KEY, true)
                                   .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
                                   .project(StateExecutionInstance.ID_KEY, true)
                                   .project(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, true)
                                   .project(StateExecutionInstance.STATUS_KEY, true)
                                   .fetch())) {
        stateMachineExecutionSimulator.prepareStateExecutionInstanceMap(iterator, stateExecutionStatuses);
      }

      breakdown = stateMachineExecutionSimulator.getStatusBreakdown(
          workflowExecution.getAppId(), workflowExecution.getEnvId(), sm, stateExecutionStatuses);
      total = breakdown.getFailed() + breakdown.getSuccess() + breakdown.getInprogress() + breakdown.getQueued();
    }

    workflowExecution.setBreakdown(breakdown);
    workflowExecution.setTotal(total);
    logger.info("Got the breakdown workflowExecution: {}, status: {}, breakdown: {}", workflowExecution.getUuid(),
        workflowExecution.getStatus(), breakdown);

    if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
      logger.info("Set the breakdown of the completed workflowExecution: {}, status: {}, breakdown: {}",
          workflowExecution.getUuid(), workflowExecution.getStatus(), breakdown);

      Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .filter("appId", workflowExecution.getAppId())
                                           .filter(ID_KEY, workflowExecution.getUuid());

      UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class);

      try {
        updateOps.set("breakdown", breakdown).set("total", total);
        UpdateResults updated = wingsPersistence.update(query, updateOps);
        logger.info("Updated : {} row", updated.getWriteResult().getN());
      } catch (java.lang.Exception e) {
        logger.error(
            "Error occurred while updating workflow execution {} with breakdown summary", workflowExecution, e);
      }
    }
  }

  private CountsByStatuses getBreakdownFromPhases(WorkflowExecution workflowExecution) {
    CountsByStatuses breakdown = new CountsByStatuses();

    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, workflowExecution.getAppId())
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflowExecution.getUuid())
            .filter(StateExecutionInstance.STATE_TYPE_KEY, PHASE.name())
            .filter(StateExecutionInstance.ROLLBACK_KEY, false)
            .field(StateExecutionInstance.CREATED_AT_KEY)
            .greaterThanOrEq(workflowExecution.getCreatedAt())
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      breakdown.setSuccess(0);
    }
    for (StateExecutionInstance stateExecutionInstance : allStateExecutionInstances) {
      StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
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
    Workflow workflow = workflowService.readWorkflow(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      logger.info("Workflow was deleted. Skipping the refresh total");
      return 0;
    }
    List<InfrastructureMapping> resolvedInfraMappings = getResolvedInfraMappings(workflow, workflowExecution);
    if (isEmpty(resolvedInfraMappings)) {
      return 0;
    }
    return infrastructureMappingService.listHosts(workflowExecution.getAppId(), resolvedInfraMappings.get(0).getUuid())
        .size();
  }

  // @TODO_PCF
  @Override
  public List<ElementExecutionSummary> getElementsSummary(
      String appId, String executionUuid, String parentStateExecutionInstanceId) {
    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, appId)
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
            .filter(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, parentStateExecutionInstanceId)
            .order(Sort.ascending(StateExecutionInstance.CREATED_AT_KEY))
            .project(StateExecutionInstance.CONTEXT_ELEMENTS_KEY, false)
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      return null;
    }

    List<StateExecutionInstance> contextTransitionInstances =
        allStateExecutionInstances.stream().filter(instance -> instance.isContextTransition()).collect(toList());
    Map<String, StateExecutionInstance> prevInstanceIdMap =
        allStateExecutionInstances.stream()
            .filter(instance -> instance.getPrevInstanceId() != null)
            .collect(toMap(instance -> instance.getPrevInstanceId(), Function.identity()));

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
            && next.getStateExecutionData() instanceof ElementStateExecutionData) {
          ElementStateExecutionData elementStateExecutionData =
              (ElementStateExecutionData) next.getStateExecutionData();
          instanceStatusSummaries.addAll(elementStateExecutionData.getElementStatusSummary()
                                             .stream()
                                             .filter(e -> e.getInstanceStatusSummaries() != null)
                                             .flatMap(l -> l.getInstanceStatusSummaries().stream())
                                             .collect(toList()));
        } else if ((nextStateType == StateType.ECS_SERVICE_DEPLOY || nextStateType == StateType.KUBERNETES_DEPLOY
                       || nextStateType == StateType.AWS_CODEDEPLOY_STATE)
            && next.getStateExecutionData() instanceof CommandStateExecutionData) {
          CommandStateExecutionData commandStateExecutionData =
              (CommandStateExecutionData) next.getStateExecutionData();
          instanceStatusSummaries.addAll(commandStateExecutionData.getNewInstanceStatusSummaries());
        } else if (nextStateType == StateType.AWS_AMI_SERVICE_DEPLOY
            && next.getStateExecutionData() instanceof AwsAmiDeployStateExecutionData) {
          AwsAmiDeployStateExecutionData awsAmiDeployStateExecutionData =
              (AwsAmiDeployStateExecutionData) next.getStateExecutionData();
          instanceStatusSummaries.addAll(awsAmiDeployStateExecutionData.getNewInstanceStatusSummaries());
        } else if (nextStateType == StateType.HELM_DEPLOY) {
          StateExecutionData stateExecutionData = next.getStateExecutionData();
          if (stateExecutionData instanceof HelmDeployStateExecutionData) {
            HelmDeployStateExecutionData helmDeployStateExecutionData =
                (HelmDeployStateExecutionData) stateExecutionData;
            if (isNotEmpty(helmDeployStateExecutionData.getNewInstanceStatusSummaries())) {
              instanceStatusSummaries.addAll(helmDeployStateExecutionData.getNewInstanceStatusSummaries());
            }
          }
        } else if (nextStateType == StateType.KUBERNETES_STEADY_STATE_CHECK) {
          KubernetesSteadyStateCheckExecutionData kubernetesSteadyStateCheckExecutionData =
              (KubernetesSteadyStateCheckExecutionData) next.getStateExecutionData();
          if (isNotEmpty(kubernetesSteadyStateCheckExecutionData.getNewInstanceStatusSummaries())) {
            instanceStatusSummaries.addAll(kubernetesSteadyStateCheckExecutionData.getNewInstanceStatusSummaries());
          }
        } else if (nextStateType == StateType.K8S_DEPLOYMENT_ROLLING || nextStateType == StateType.K8S_CANARY_DEPLOY
            || nextStateType == StateType.K8S_BLUE_GREEN_DEPLOY || nextStateType == StateType.K8S_SCALE) {
          StateExecutionData stateExecutionData = next.getStateExecutionData();
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
            .filter(StateExecutionInstance.APP_ID_KEY, appId)
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
            .filter(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, stateExecutionInstanceId)
            .filter(StateExecutionInstance.STATE_TYPE_KEY, PHASE_STEP.name())
            .project(StateExecutionInstance.CONTEXT_ELEMENT_KEY, true)
            .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
            .project(StateExecutionInstance.ID_KEY, true)
            .project(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, true)
            .project(StateExecutionInstance.STATE_EXECUTION_MAP_KEY, true)
            .project(StateExecutionInstance.STATE_TYPE_KEY, true)
            .project(StateExecutionInstance.STATUS_KEY, true)
            .asList();

    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    if (isEmpty(allStateExecutionInstances)) {
      return null;
    }

    allStateExecutionInstances.forEach(instance -> {
      StateExecutionData stateExecutionData = instance.getStateExecutionData();
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
      try (HIterator<StateExecutionInstance> iterator = new HIterator<StateExecutionInstance>(
               wingsPersistence.createQuery(StateExecutionInstance.class)
                   .filter(StateExecutionInstance.APP_ID_KEY, appId)
                   .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
                   .field(StateExecutionInstance.PARENT_INSTANCE_ID_KEY)
                   .in(parentInstanceIds)
                   .project(StateExecutionInstance.CONTEXT_ELEMENT_KEY, true)
                   .project(StateExecutionInstance.DISPLAY_NAME_KEY, true)
                   .project(StateExecutionInstance.ID_KEY, true)
                   .project(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, true)
                   .project(StateExecutionInstance.STATE_EXECUTION_MAP_KEY, true)
                   .project(StateExecutionInstance.STATE_TYPE_KEY, true)
                   .project(StateExecutionInstance.STATUS_KEY, true)
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
            stepExecutionSummaryList.add(instance.getStateExecutionData().getStepExecutionSummary());
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

  public List<Artifact> getArtifactsCollected(String appId, String executionUuid) {
    List<StateExecutionInstance> allStateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, appId)
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
            .filter(StateExecutionInstance.STATE_TYPE_KEY, ARTIFACT_COLLECTION.name())
            .asList();

    if (isEmpty(allStateExecutionInstances)) {
      return null;
    }

    List<Artifact> artifacts = new ArrayList<>();
    allStateExecutionInstances.forEach(stateExecutionInstance -> {
      ArtifactCollectionExecutionData artifactCollectionExecutionData =
          (ArtifactCollectionExecutionData) stateExecutionInstance.getStateExecutionData();
      artifacts.add(artifactService.get(appId, artifactCollectionExecutionData.getArtifactId()));
    });
    return artifacts;
  }

  @Override
  public void refreshBuildExecutionSummary(
      String appId, String workflowExecutionId, BuildExecutionSummary buildExecutionSummary) {
    WorkflowExecution workflowExecution =
        wingsPersistence.getWithAppId(WorkflowExecution.class, appId, workflowExecutionId, CRITICAL);
    if (workflowExecution == null) {
      return;
    }

    List<BuildExecutionSummary> buildExecutionSummaries = workflowExecution.getBuildExecutionSummaries();
    if (isEmpty(buildExecutionSummaries)) {
      buildExecutionSummaries = new ArrayList<>();
    }
    buildExecutionSummaries.add(buildExecutionSummary);
    buildExecutionSummaries =
        buildExecutionSummaries.stream().filter(distinctByKey(bs -> bs.getArtifactStreamId())).collect(toList());
    wingsPersistence.updateField(
        WorkflowExecution.class, workflowExecutionId, "buildExecutionSummaries", buildExecutionSummaries);
  }

  @Override
  public Set<WorkflowExecutionBaseline> markBaseline(String appId, String workflowExecutionId, boolean isBaseline) {
    WorkflowExecution workflowExecution =
        wingsPersistence.getWithAppId(WorkflowExecution.class, appId, workflowExecutionId);
    if (workflowExecution == null) {
      throw new WingsException(ErrorCode.BASELINE_CONFIGURATION_ERROR,
          "No workflow execution found with id: " + workflowExecutionId + " appId: " + appId);
    }
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    switch (workflowExecution.getWorkflowType()) {
      case PIPELINE:
        PipelineExecution pipelineExecution = workflowExecution.getPipelineExecution();
        if (pipelineExecution == null) {
          throw new WingsException(ErrorCode.BASELINE_CONFIGURATION_ERROR, "Pipeline has not been executed.")
              .addParam("message", "Pipeline has not been executed.");
        }

        List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
        if (isEmpty(pipelineStageExecutions)) {
          throw new WingsException(
              ErrorCode.BASELINE_CONFIGURATION_ERROR, "No workflows have been executed for this pipeline.")
              .addParam("message", "No workflows have been executed for this pipeline.");
        }
        pipelineStageExecutions.forEach(
            pipelineStageExecution -> workflowExecutions.addAll(pipelineStageExecution.getWorkflowExecutions()));
        break;

      case ORCHESTRATION:
        workflowExecutions.add(workflowExecution);
        break;
      default:
        throw new WingsException(
            ErrorCode.BASELINE_CONFIGURATION_ERROR, "Invalid workflow type: " + workflowExecution.getWorkflowType())
            .addParam("message", "Invalid workflow type: " + workflowExecution.getWorkflowType());
    }

    final Set<WorkflowExecutionBaseline> baselines = new HashSet<>();

    if (!isEmpty(workflowExecutions)) {
      workflowExecutions.forEach(stageExecution -> {
        String executionUuid = stageExecution.getUuid();
        List<StateExecutionInstance> stateExecutionInstances =
            wingsPersistence.createQuery(StateExecutionInstance.class)
                .filter(StateExecutionInstance.APP_ID_KEY, appId)
                .filter(StateExecutionInstance.EXECUTION_UUID_KEY, executionUuid)
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
      String appId, String workflowExecutionId, String stateExecutionId, String currentExecId) {
    ExecutionContext executionContext =
        stateMachineExecutor.getExecutionContext(appId, currentExecId, stateExecutionId);
    if (executionContext == null) {
      logger.info("failed to get baseline details for app {}, workflow execution {}, uuid {}", appId, currentExecId,
          stateExecutionId);
      return null;
    }
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams.getEnv().getUuid();
    PhaseElement phaseElement = executionContext.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    PageRequest<WorkflowExecutionBaseline> pageRequest = aPageRequest()
                                                             .addFilter("workflowExecutionId", EQ, workflowExecutionId)
                                                             .addFilter("envId", EQ, envId)
                                                             .addFilter("serviceId", EQ, serviceId)
                                                             .addFilter("appId", EQ, appId)
                                                             .build();
    PageResponse<WorkflowExecutionBaseline> pageResponse =
        wingsPersistence.query(WorkflowExecutionBaseline.class, pageRequest);
    Preconditions.checkState(
        pageResponse.size() <= 1, "workflowExecutionId " + workflowExecutionId + " exists in more than one baselines");

    if (isEmpty(pageResponse.getResponse())) {
      return null;
    }

    return pageResponse.getResponse().get(0);
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
  public HIterator<WorkflowExecution> obtainWorkflowExecutionIterator(List<String> appIds, long epochMilli) {
    final Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                               .field(WorkflowExecution.CREATED_AT_KEY)
                                               .greaterThanOrEq(epochMilli)
                                               .field(WorkflowExecution.PIPELINE_EXECUTION_ID_KEY)
                                               .doesNotExist()
                                               .field(WorkflowExecution.APP_ID_KEY)
                                               .in(appIds)
                                               .project(WorkflowExecution.STATE_MACHINE_KEY, false);
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

  private WorkflowExecution fetchLastSuccessDeployment(String appId, String workflowId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .filter(WORKFLOW_ID_KEY, workflowId)
        .filter(APP_ID_KEY, appId)
        .filter(STATUS_KEY, SUCCESS)
        .order("-createdAt")
        .get();
  }

  @Override
  public WorkflowExecution fetchWorkflowExecution(
      String appId, List<String> serviceIds, List<String> envIds, String workflowId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .filter("workflowType", ORCHESTRATION)
        .filter("workflowId", workflowId)
        .filter("appId", appId)
        .filter("status", SUCCESS)
        .field("serviceIds")
        .in(serviceIds)
        .field("envIds")
        .in(envIds)
        .order(Sort.descending(WorkflowExecution.CREATED_AT_KEY))
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
            .addFieldsIncluded(WorkflowExecution.APP_ID_KEY, WorkflowExecution.STATUS_KEY,
                WorkflowExecution.WORKFLOW_ID_KEY, WorkflowExecution.CREATED_AT_KEY, WorkflowExecution.ID_KEY,
                WorkflowExecution.START_TS_KEY, WorkflowExecution.END_TS_KEY, WorkflowExecution.NAME_KEY)
            .addFilter(WorkflowExecution.APP_ID_KEY, EQ, appId)
            .addFilter(WorkflowExecution.STATUS_KEY, IN, ExecutionStatus.activeStatuses().toArray())
            .addFilter(WorkflowExecution.CREATED_AT_KEY, LT_EQ, workflowExecution.getCreatedAt())
            .addOrder(WorkflowExecution.CREATED_AT_KEY, OrderType.ASC)
            .addFilter(WorkflowExecution.WORKFLOW_ID_KEY, EQ, workflowExecution.getWorkflowId());

    if (isNotEmpty(workflowExecution.getInfraMappingIds())) {
      pageRequestBuilder.addFilter(
          WorkflowExecution.INFRA_MAPPING_IDS_KEY, IN, workflowExecution.getInfraMappingIds().toArray());
    }
    return wingsPersistence.query(WorkflowExecution.class, pageRequestBuilder.build());
  }

  @Override
  public Long fetchWorkflowExecutionStartTs(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .project(WorkflowExecution.START_TS_KEY, true)
                                              .project(WorkflowExecution.APP_ID_KEY, true)
                                              .project(WorkflowExecution.ID_KEY, true)
                                              .filter(WorkflowExecution.APP_ID_KEY, appId)
                                              .filter(WorkflowExecution.ID_KEY, workflowExecutionId)
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
        .project(WorkflowExecution.SERVICE_EXECUTION_SUMMARIES, false)
        .project(WorkflowExecution.EXECUTION_ARGS, false)
        .filter(WorkflowExecution.APP_ID_KEY, appId)
        .filter(WorkflowExecution.ID_KEY, workflowExecutionId)
        .get();
  }

  @Override
  public WorkflowExecution getWorkflowExecutionForVerificationService(String appId, String workflowExecutionId) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .project(WorkflowExecution.UUID_KEY, true)
        .project(WorkflowExecution.ENV_ID_KEY, true)
        .filter(WorkflowExecution.APP_ID_KEY, appId)
        .filter(WorkflowExecution.ID_KEY, workflowExecutionId)
        .get();
  }

  @Override
  public void refreshCollectedArtifacts(String appId, String pipelineExecutionId, String workflowExecutionId) {
    WorkflowExecution pipelineWorkflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                                      .project(WorkflowExecution.ARTIFACTS_KEY, true)
                                                      .filter(WorkflowExecution.APP_ID_KEY, appId)
                                                      .filter(WorkflowExecution.ID_KEY, pipelineExecutionId)
                                                      .get();

    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                              .project(WorkflowExecution.ARTIFACTS_KEY, true)
                                              .filter(WorkflowExecution.APP_ID_KEY, appId)
                                              .filter(WorkflowExecution.ID_KEY, workflowExecutionId)
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
    collectedArtifacts =
        collectedArtifacts.stream().filter(distinctByKey(artifact -> artifact.getUuid())).collect(toList());

    Query<WorkflowExecution> updatedQuery = wingsPersistence.createQuery(WorkflowExecution.class)
                                                .project(WorkflowExecution.ARTIFACTS_KEY, true)
                                                .filter(WorkflowExecution.APP_ID_KEY, appId)
                                                .filter(WorkflowExecution.ID_KEY, pipelineExecutionId);

    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set(WorkflowExecution.ARTIFACTS_KEY, collectedArtifacts);

    wingsPersistence.update(updatedQuery, updateOps);
  }

  public StateMachine obtainStateMachine(WorkflowExecution workflowExecution) {
    if (workflowExecution.getStateMachine() != null) {
      return workflowExecution.getStateMachine();
    }
    logger.warn(String.format("Workflow execution %s do not have inline state machine", workflowExecution.getUuid()));
    return wingsPersistence.getWithAppId(
        StateMachine.class, workflowExecution.getAppId(), workflowExecution.getStateMachineId());
  }

  @Override
  public WorkflowExecution fetchLastWorkflowExecution(String appId, String workflowId, String serviceId, String envId) {
    Query<WorkflowExecution> workflowExecutionQuery = wingsPersistence.createQuery(WorkflowExecution.class)
                                                          .filter(WorkflowExecution.WORKFLOW_TYPE_ID_KEY, ORCHESTRATION)
                                                          .filter(WorkflowExecution.WORKFLOW_ID_KEY, workflowId)
                                                          .filter(WorkflowExecution.APP_ID_KEY, appId);

    if (StringUtils.isNotBlank(serviceId)) {
      workflowExecutionQuery.field(WorkflowExecution.WORKFLOW_SERVICE_IDS_KEY).in(Arrays.asList(serviceId));
    }

    if (StringUtils.isNotBlank(envId)) {
      workflowExecutionQuery.field(WorkflowExecution.WORKFLOW_ENV_IDS_KEY).in(Arrays.asList(envId));
    }

    return workflowExecutionQuery.order(Sort.descending(WorkflowExecution.CREATED_AT_KEY)).get();
  }

  @Override
  public PageResponse<WorkflowExecution> fetchWorkflowExecutionList(
      String appId, String workflowId, String envId, String pageOffset, String pageLimit) {
    PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .addFilter(WorkflowExecution.WORKFLOW_TYPE_ID_KEY, Operator.EQ, ORCHESTRATION)
            .addFilter(WorkflowExecution.APP_ID_KEY, Operator.EQ, appId)
            .addFilter(WorkflowExecution.WORKFLOW_ENV_IDS_KEY, Operator.IN, Arrays.asList(envId))
            .withLimit(pageLimit)
            .withOffset(pageOffset)
            .addOrder(
                SortOrder.Builder.aSortOrder().withField(WorkflowExecution.CREATED_AT_KEY, OrderType.DESC).build())
            .build();

    return wingsPersistence.query(WorkflowExecution.class, pageRequest);
  }
}
