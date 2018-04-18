/**
 *
 */

package software.wings.service.impl;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtil.trimList;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.ApprovalStateExecutionData.Builder.anApprovalStateExecutionData;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.api.WorkflowElement.WorkflowElementBuilder.aWorkflowElement;
import static software.wings.beans.ApprovalDetails.Action.APPROVE;
import static software.wings.beans.ApprovalDetails.Action.REJECT;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.beans.EntityType.SIMPLE_DEPLOYMENT;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.PipelineStageExecution.Builder.aPipelineStageExecution;
import static software.wings.beans.ReadPref.CRITICAL;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.GE;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.exception.WingsException.HARMLESS;
import static software.wings.service.impl.ExecutionEvent.ExecutionEventBuilder.anExecutionEvent;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.PAUSE_ALL;
import static software.wings.sm.ExecutionInterruptType.RESUME_ALL;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.NEW;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.PAUSING;
import static software.wings.sm.ExecutionStatus.QUEUED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.STARTING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.InfraMappingSummary.Builder.anInfraMappingSummary;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.PHASE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBCursor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.AwsAmiDeployStateExecutionData;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.BuildExecutionSummary;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CanaryWorkflowExecutionAdvisor;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.RequiredExecutionArgs;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.StateExecutionElement;
import software.wings.beans.StateExecutionInterrupt;
import software.wings.beans.User;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.Constants;
import software.wings.core.queue.Queue;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.BarrierService.OrchestrationWorkflowInfo;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionEventAdvisor;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptEffect;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionInterruptType;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionCallback;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ElementStateExecutionData;
import software.wings.sm.states.RepeatState.RepeatStateExecutionData;
import software.wings.utils.KryoUtils;
import software.wings.utils.MapperUtils;
import software.wings.utils.Validator;
import software.wings.waitnotify.WaitNotifyEngine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class WorkflowExecutionServiceImpl.
 *
 * @author Rishi
 */
@Singleton
@ValidateOnExecution
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {
  private static final String COMMAND_NAME_PREF = "Command: ";
  private static final String WORKFLOW_NAME_PREF = "Workflow: ";
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
  @Inject private WorkflowService workflowService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private PipelineService pipelineService;
  @Inject private ExecutorService executorService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Queue<ExecutionEvent> executionEventQueue;
  @Inject private WorkflowExecutionBaselineService workflowExecutionBaselineService;

  @Inject private WingsPersistence wingsPersistence;

  /**
   * {@inheritDoc}
   */
  @Override
  public void trigger(String appId, String stateMachineId, String executionUuid, String executionName) {
    trigger(appId, stateMachineId, executionUuid, executionName, null);
  }

  /**
   * Trigger.
   *
   * @param appId          the app id
   * @param stateMachineId the state machine id
   * @param executionUuid  the execution uuid
   * @param executionName  the execution name
   * @param callback       the callback
   */
  void trigger(String appId, String stateMachineId, String executionUuid, String executionName,
      StateMachineExecutionCallback callback) {
    stateMachineExecutor.execute(appId, stateMachineId, executionUuid, executionName, null, callback);
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
          logger.error("Failed to refresh service summaries for the workflow execution {} ", workflowExecution, e);
        }
      }

      if (!runningOnly || workflowExecution.isRunningStatus() || workflowExecution.isPausedStatus()) {
        try {
          populateNodeHierarchy(workflowExecution, includeGraph, includeStatus, emptySet());
        } catch (Exception e) {
          logger.error("Failed to populate node hierarchy for the workflow execution {} ", res, e);
        }
      }
    }
    return res;
  }

  @Override
  public boolean updateNotes(String appId, String workflowExecutionId, ExecutionArgs executionArgs) {
    notNullCheck("executionArgs", executionArgs);
    notNullCheck("notes", executionArgs.getNotes());

    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("workflowExecution", workflowExecution);

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
  public boolean approveOrRejectExecution(String appId, String workflowExecutionId, ApprovalDetails approvalDetails) {
    notNullCheck("ApprovalDetails", approvalDetails);
    String approvalId = approvalDetails.getApprovalId();

    WorkflowExecution workflowExecution = getWorkflowExecution(appId, workflowExecutionId);
    notNullCheck("workflowExecution", workflowExecution);

    if (!isPipelineWaitingApproval(workflowExecution.getPipelineExecution(), approvalId)) {
      throw new WingsException(INVALID_ARGUMENT)
          .addParam("args",
              "No Pipeline execution [" + workflowExecutionId
                  + "] waiting for approval id: " + approvalDetails.getApprovalId());
    }
    User user = UserThreadLocal.get();
    if (user != null) {
      approvalDetails.setApprovedBy(EmbeddedUser.builder().email(user.getEmail()).name(user.getName()).build());
    }
    ApprovalStateExecutionData executionData = null;
    if (approvalDetails.getAction() == null || approvalDetails.getAction().equals(APPROVE)) {
      logger.debug("Notifying to approve the pipeline execution {} for approval id {} ", workflowExecutionId,
          approvalDetails.getApprovalId());
      executionData = anApprovalStateExecutionData()
                          .withApprovalId(approvalDetails.getApprovalId())
                          .withStatus(ExecutionStatus.SUCCESS)
                          .withApprovedBy(approvalDetails.getApprovedBy())
                          .withComments(approvalDetails.getComments())
                          .build();
    } else if (approvalDetails.getAction().equals(REJECT)) {
      logger.debug("Notifying to reject the pipeline execution {} for approval id {} ", workflowExecutionId,
          approvalDetails.getApprovalId());
      executionData = anApprovalStateExecutionData()
                          .withApprovalId(approvalDetails.getApprovalId())
                          .withStatus(ExecutionStatus.ABORTED)
                          .withApprovedBy(approvalDetails.getApprovedBy())
                          .withComments(approvalDetails.getComments())
                          .build();
    }
    waitNotifyEngine.notify(approvalDetails.getApprovalId(), executionData);
    return true;
  }

  private boolean isPipelineWaitingApproval(PipelineExecution pipelineExecution, String approvalId) {
    if (pipelineExecution == null || pipelineExecution.getPipelineStageExecutions() == null) {
      return false;
    }

    return pipelineExecution.getPipelineStageExecutions().stream().anyMatch(pe
        -> pe.getStatus() == PAUSED && pe.getStateExecutionData() instanceof ApprovalStateExecutionData
            && approvalId.equals(((ApprovalStateExecutionData) pe.getStateExecutionData()).getApprovalId()));
  }

  private void refreshPipelineExecution(WorkflowExecution workflowExecution) {
    if (workflowExecution == null || workflowExecution.getPipelineExecution() == null) {
      return;
    }
    if (workflowExecution.getPipelineExecution().getStatus().isFinalStatus()
        && workflowExecution.getPipelineExecution()
               .getPipelineStageExecutions()
               .stream()
               .flatMap(pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions().stream())
               .allMatch(workflowExecution1 -> workflowExecution1.getStatus().isFinalStatus())) {
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
            stageExecutionDataList.add(aPipelineStageExecution()
                                           .withStateType(pipelineStageElement.getType())
                                           .withStateName(pipelineStageElement.getName())
                                           .withStatus(ExecutionStatus.QUEUED)
                                           .withEstimatedTime(pipelineExecution.getPipeline().getStateEtaMap().get(
                                               pipelineStageElement.getName()))
                                           .build());

          } else if (APPROVAL.name().equals(stateExecutionInstance.getStateType())) {
            PipelineStageExecution stageExecution = aPipelineStageExecution()
                                                        .withStateType(stateExecutionInstance.getStateType())
                                                        .withStatus(stateExecutionInstance.getStatus())
                                                        .withStateName(stateExecutionInstance.getDisplayName())
                                                        .withStartTs(stateExecutionInstance.getStartTs())
                                                        .withEndTs(stateExecutionInstance.getEndTs())
                                                        .build();
            StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();

            if (stateExecutionData != null && stateExecutionData instanceof ApprovalStateExecutionData) {
              stageExecution.setStateExecutionData(stateExecutionData);
            }
            stageExecutionDataList.add(stageExecution);

          } else if (ENV_STATE.name().equals(stateExecutionInstance.getStateType())) {
            PipelineStageExecution stageExecution = aPipelineStageExecution()
                                                        .withStateType(pipelineStageElement.getType())
                                                        .withStateName(pipelineStageElement.getName())
                                                        .withStatus(stateExecutionInstance.getStatus())
                                                        .withStartTs(stateExecutionInstance.getStartTs())
                                                        .withEndTs(stateExecutionInstance.getEndTs())
                                                        .build();
            StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();

            if (stateExecutionData != null && stateExecutionData instanceof EnvStateExecutionData) {
              EnvStateExecutionData envStateExecutionData = (EnvStateExecutionData) stateExecutionData;
              if (envStateExecutionData.getWorkflowExecutionId() != null) {
                WorkflowExecution workflowExecution2 = getExecutionDetailsWithoutGraph(
                    workflowExecution.getAppId(), envStateExecutionData.getWorkflowExecutionId());
                stageExecution.setWorkflowExecutions(asList(workflowExecution2));
                stageExecution.setStatus(workflowExecution2.getStatus());
              }
              stageExecution.setMessage(envStateExecutionData.getErrorMsg());
            }
            stageExecutionDataList.add(stageExecution);

          } else {
            throw new WingsException(ErrorCode.INVALID_REQUEST)
                .addParam("message", "Unknown stateType " + stateExecutionInstance.getStateType());
          }
        });

    pipelineExecution.setPipelineStageExecutions(stageExecutionDataList);

    if (workflowExecution.getStatus().isFinalStatus()) {
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
    if (!workflowExecution.getStatus().isFinalStatus()) {
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

    Map<String, LongSummaryStatistics> stateEstimatesSum =
        workflowExecutions.stream()
            .map(we -> we.getPipelineExecution())
            .flatMap(pe -> pe.getPipelineStageExecutions().stream())
            .collect(Collectors.groupingBy(
                PipelineStageExecution::getStateName, Collectors.summarizingLong(this ::getEstimate)));

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
    PageRequest<StateExecutionInstance> req = aPageRequest()
                                                  .withLimit(UNLIMITED)
                                                  .addFilter("appId", EQ, workflowExecution.getAppId())
                                                  .addFilter("executionUuid", EQ, workflowExecution.getUuid())
                                                  .addFilter("createdAt", GE, workflowExecution.getCreatedAt())
                                                  .build();
    return wingsPersistence.query(StateExecutionInstance.class, req).getResponse();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution getExecutionDetails(
      String appId, String workflowExecutionId, Set<String> excludeFromAggregation) {
    WorkflowExecution workflowExecution = getExecutionDetailsWithoutGraph(appId, workflowExecutionId);

    if (workflowExecution.getWorkflowType() == PIPELINE) {
      return workflowExecution;
    }

    if (workflowExecution != null) {
      populateNodeHierarchyWithGraph(workflowExecution, excludeFromAggregation);
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
    notNullCheck("WorkflowExecution", workflowExecution);

    if (workflowExecution.getExecutionArgs() != null) {
      if (workflowExecution.getExecutionArgs().getServiceInstanceIdNames() != null) {
        PageRequest<ServiceInstance> pageRequest =
            aPageRequest()
                .addFilter("appId", EQ, appId)
                .addFilter("uuid", Operator.IN,
                    workflowExecution.getExecutionArgs().getServiceInstanceIdNames().keySet().toArray())
                .build();
        workflowExecution.getExecutionArgs().setServiceInstances(
            serviceInstanceService.list(pageRequest).getResponse());
      }
      if (workflowExecution.getExecutionArgs().getArtifactIdNames() != null) {
        PageRequest<Artifact> pageRequest =
            aPageRequest()
                .addFilter("appId", EQ, appId)
                .addFilter(
                    "uuid", Operator.IN, workflowExecution.getExecutionArgs().getArtifactIdNames().keySet().toArray())
                .build();
        workflowExecution.getExecutionArgs().setArtifacts(artifactService.list(pageRequest, false).getResponse());
      }
    }
    return workflowExecution;
  }

  @Override
  public WorkflowExecution getWorkflowExecution(String appId, String workflowExecutionId) {
    logger.debug("Retrieving workflow execution details for id {} of App Id {} ", workflowExecutionId, appId);
    return wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
  }

  private void populateNodeHierarchy(WorkflowExecution workflowExecution, boolean includeGraph, boolean includeStatus,
      Set<String> excludeFromAggregation) {
    if (!includeStatus && !includeGraph) {
      return;
    }

    Map<String, StateExecutionInstance> allInstancesIdMap =
        stateExecutionService.executionStatesMap(workflowExecution.getAppId(), workflowExecution.getUuid());

    if (allInstancesIdMap.isEmpty()) {
      return;
    }

    if (!workflowExecution.getStatus().isFinalStatus()) {
      if (allInstancesIdMap.values().stream().anyMatch(
              i -> i.getStatus() == ExecutionStatus.PAUSED || i.getStatus() == ExecutionStatus.PAUSING)) {
        workflowExecution.setStatus(ExecutionStatus.PAUSED);
      } else if (allInstancesIdMap.values().stream().anyMatch(i -> i.getStatus() == ExecutionStatus.WAITING)) {
        workflowExecution.setStatus(ExecutionStatus.WAITING);
      } else {
        List<ExecutionInterrupt> executionInterrupts = executionInterruptManager.checkForExecutionInterrupt(
            workflowExecution.getAppId(), workflowExecution.getUuid());
        if (executionInterrupts != null
            && executionInterrupts.stream().anyMatch(
                   e -> e.getExecutionInterruptType() == ExecutionInterruptType.PAUSE_ALL)) {
          workflowExecution.setStatus(ExecutionStatus.PAUSING);
        }
      }
    }
    if (!includeGraph) {
      return;
    }

    final GraphNode graphNode = graphRenderer.generateHierarchyNode(allInstancesIdMap, excludeFromAggregation);
    workflowExecution.setExecutionNode(graphNode);
  }

  private void populateNodeHierarchyWithGraph(WorkflowExecution workflowExecution, Set<String> excludeFromAggregation) {
    populateNodeHierarchy(workflowExecution, true, false, excludeFromAggregation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerPipelineExecution(String appId, String pipelineId, ExecutionArgs executionArgs) {
    return triggerPipelineExecution(appId, pipelineId, executionArgs, null);
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
  public WorkflowExecution triggerPipelineExecution(
      String appId, String pipelineId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    Pipeline pipeline = pipelineService.readPipeline(appId, pipelineId, true);
    if (pipeline == null) {
      throw new WingsException(ErrorCode.NON_EXISTING_PIPELINE);
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

    StateMachine stateMachine = workflowService.readLatestStateMachine(appId, pipelineId);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + pipelineId);
    }
    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setUuid(generateUuid());
    workflowExecution.setAppId(appId);
    workflowExecution.setWorkflowId(pipelineId);
    workflowExecution.setWorkflowType(WorkflowType.PIPELINE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());
    workflowExecution.setName(pipeline.getName());

    constructBarriers(pipeline, workflowExecution.getUuid());

    // Do not remove this. Morphia referencing it by id and one object getting overridden by the other
    pipeline.setUuid(generateUuid() + "_embedded");

    PipelineExecution pipelineExecution =
        aPipelineExecution().withPipelineId(pipelineId).withPipeline(pipeline).build();
    workflowExecution.setPipelineExecution(pipelineExecution);
    workflowExecution.setPipelineSummary(PipelineSummary.Builder.aPipelineSummary()
                                             .withPipelineId(pipelineId)
                                             .withPipelineName(pipeline.getName())
                                             .build());

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    if (isNotEmpty(executionArgs.getArtifacts())) {
      stdParams.setArtifactIds(executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(toList()));
    }
    if (isNotEmpty(executionArgs.getWorkflowVariables())) {
      stdParams.setWorkflowVariables(executionArgs.getWorkflowVariables());
    }
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

    Set<String> envIds = new HashSet<>();
    pipeline.getPipelineStages()
        .stream()
        .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
        .forEach(pipelineStageElement -> {
          if (pipelineStageElement.getType().equals(ENV_STATE.name())) {
            if (pipelineStageElement.getProperties() != null
                && pipelineStageElement.getProperties().get("envId") != null) {
              envIds.add(String.valueOf(pipelineStageElement.getProperties().get("envId")));
            }
          }
        });
    workflowExecution.setEnvIds(new ArrayList<>(envIds));
    return triggerExecution(workflowExecution, stateMachine, workflowExecutionUpdate, stdParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String envId, String workflowId, ExecutionArgs executionArgs) {
    return triggerOrchestrationWorkflowExecution(appId, envId, workflowId, null, executionArgs, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerOrchestrationExecution(
      String appId, String envId, String workflowId, String pipelineExecutionId, ExecutionArgs executionArgs) {
    return triggerOrchestrationWorkflowExecution(appId, envId, workflowId, pipelineExecutionId, executionArgs, null);
  }

  /**
   * Trigger orchestration execution workflow execution.
   *
   * @param appId                   the app id
   * @param envId                   the env id
   * @param workflowId              the orchestration id
   * @param executionArgs           the execution args
   * @param workflowExecutionUpdate the workflow execution update
   * @return the workflow execution
   */
  public WorkflowExecution triggerOrchestrationWorkflowExecution(String appId, String envId, String workflowId,
      String pipelineExecutionId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    // TODO - validate list of artifact Ids if it's matching for all the services involved in this orchestration

    Workflow workflow = workflowService.readWorkflow(appId, workflowId);

    if (!workflow.getOrchestrationWorkflow().isValid()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam("message", "Workflow requested for execution is not valid/complete.");
    }
    StateMachine stateMachine = workflowService.readStateMachine(appId, workflowId, workflow.getDefaultVersion());
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + workflowId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    if (envId != null) {
      workflowExecution.setEnvId(envId);
      workflowExecution.setEnvIds(Collections.singletonList(envId));
    }
    workflowExecution.setWorkflowId(workflowId);
    workflowExecution.setName(workflow.getName());
    workflowExecution.setWorkflowType(ORCHESTRATION);
    workflowExecution.setOrchestrationType(workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType());
    workflowExecution.setStateMachineId(stateMachine.getUuid());
    workflowExecution.setPipelineExecutionId(pipelineExecutionId);
    workflowExecution.setExecutionArgs(executionArgs);

    List<Service> services;
    if (isServiceTemplatized(workflow)) {
      Map<String, String> workflowVariables = workflowExecution.getExecutionArgs() != null
          ? workflowExecution.getExecutionArgs().getWorkflowVariables()
          : null;
      services = workflowService.resolveServices(workflow, workflowVariables);
    } else {
      services = workflow.getServices();
    }

    if (isNotEmpty(services)) {
      workflowExecution.setServiceIds(services.stream().map(Service::getUuid).collect(toList()));
    }

    WorkflowStandardParams stdParams;
    if (workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.CANARY
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.BASIC
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.ROLLING
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.MULTI_SERVICE
        || workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType() == OrchestrationWorkflowType.BUILD) {
      stdParams = new CanaryWorkflowStandardParams();

      if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
        CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
            (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
        if (canaryOrchestrationWorkflow.getUserVariables() != null) {
          stdParams.setWorkflowElement(
              aWorkflowElement()
                  .withVariables(getWorkflowVariables(canaryOrchestrationWorkflow, executionArgs))
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

    return triggerExecution(
        workflowExecution, stateMachine, new CanaryWorkflowExecutionAdvisor(), workflowExecutionUpdate, stdParams);
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
        if (variable.isMandatory() && variable.getValue() == null) {
          throw new WingsException(ErrorCode.INVALID_REQUEST)
              .addParam("message", "Workflow variable [" + variable.getName() + "] is mandatory for execution");
        }
        setVariables(variable.getName(), variable.getValue(), variables);
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
      WorkflowExecutionUpdate workflowExecutionUpdate, WorkflowStandardParams stdParams,
      ContextElement... contextElements) {
    return triggerExecution(workflowExecution, stateMachine, null, workflowExecutionUpdate, stdParams, contextElements);
  }

  private WorkflowExecution triggerExecution(WorkflowExecution workflowExecution, StateMachine stateMachine,
      ExecutionEventAdvisor workflowExecutionAdvisor, WorkflowExecutionUpdate workflowExecutionUpdate,
      WorkflowStandardParams stdParams, ContextElement... contextElements) {
    List<Object> keywords = newArrayList(
        workflowExecution.getName(), workflowExecution.getWorkflowType(), workflowExecution.getOrchestrationType());

    Application app = appService.get(workflowExecution.getAppId());
    workflowExecution.setAppName(app.getName());
    keywords.add(workflowExecution.getAppName());

    if (workflowExecution.getEnvId() != null) {
      Environment env = environmentService.get(workflowExecution.getAppId(), workflowExecution.getEnvId(), false);
      workflowExecution.setEnvName(env.getName());
      workflowExecution.setEnvType(env.getEnvironmentType());
    }
    keywords.add(workflowExecution.getEnvType());
    keywords.add(workflowExecution.getEnvName());

    User user = UserThreadLocal.get();
    if (user != null) {
      EmbeddedUser triggeredBy =
          EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
      workflowExecution.setTriggeredBy(triggeredBy);
      workflowExecution.setCreatedBy(triggeredBy);
    } else if (workflowExecution.getExecutionArgs() != null
        && workflowExecution.getExecutionArgs().getTriggeredBy() != null) {
      workflowExecution.setTriggeredBy(workflowExecution.getExecutionArgs().getTriggeredBy());
      workflowExecution.setCreatedBy(workflowExecution.getExecutionArgs().getTriggeredBy());
    } else {
      // Triggered by Auto Trigger
      workflowExecution.setTriggeredBy(EmbeddedUser.builder().name("Deployment trigger").build());
      workflowExecution.setCreatedBy(EmbeddedUser.builder().name("Deployment trigger").build());
    }
    keywords.add(workflowExecution.getCreatedBy().getName());
    keywords.add(workflowExecution.getCreatedBy().getEmail());

    ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
    if (executionArgs != null) {
      if (executionArgs.getServiceInstances() != null) {
        List<String> serviceInstanceIds =
            executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(toList());
        PageRequest<ServiceInstance> pageRequest = aPageRequest()
                                                       .addFilter("appId", EQ, workflowExecution.getAppId())
                                                       .addFilter("uuid", Operator.IN, serviceInstanceIds.toArray())
                                                       .build();
        List<ServiceInstance> serviceInstances = serviceInstanceService.list(pageRequest).getResponse();

        if (serviceInstances == null || serviceInstances.size() != serviceInstanceIds.size()) {
          logger.error("Service instances argument and valid service instance retrieved size not matching");
          throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", "Invalid service instances");
        }
        executionArgs.setServiceInstanceIdNames(serviceInstances.stream().collect(toMap(ServiceInstance::getUuid,
            serviceInstance -> serviceInstance.getHostName() + ":" + serviceInstance.getServiceName())));

        keywords.addAll(serviceInstances.stream().map(ServiceInstance::getHostName).collect(toList()));
        keywords.addAll(serviceInstances.stream().map(ServiceInstance::getServiceName).collect(toList()));
      }

      if (isNotEmpty(executionArgs.getArtifacts())) {
        List<String> artifactIds = executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(toList());
        PageRequest<Artifact> pageRequest = aPageRequest()
                                                .addFilter("appId", EQ, workflowExecution.getAppId())
                                                .addFilter("uuid", Operator.IN, artifactIds.toArray())
                                                .build();
        List<Artifact> artifacts = artifactService.list(pageRequest, false).getResponse();

        if (artifacts == null || artifacts.size() != artifactIds.size()) {
          logger.error("Artifact argument and valid artifact retrieved size not matching");
          throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", "Invalid artifact");
        }

        // TODO: get rid of artifactIdNames when UI moves to artifact list
        executionArgs.setArtifactIdNames(
            artifacts.stream().collect(toMap(Artifact::getUuid, Artifact::getDisplayName)));
        artifacts.forEach(artifact -> {
          artifact.setArtifactFiles(null);
          artifact.setCreatedBy(null);
          artifact.setLastUpdatedBy(null);
          keywords.add(artifact.getArtifactSourceName());
          keywords.add(artifact.getDescription());
          keywords.add(artifact.getRevision());
          keywords.add(artifact.getMetadata());
        });
        executionArgs.setArtifacts(artifacts);
        List<ServiceElement> services = new ArrayList<>();
        artifacts.forEach(artifact -> {
          artifact.getServiceIds().forEach(serviceId -> {
            Service service = serviceResourceService.get(artifact.getAppId(), serviceId, false);
            ServiceElement se = new ServiceElement();
            MapperUtils.mapObject(service, se);
            services.add(se);
            keywords.add(se.getName());
          });
        });
        stdParams.setServices(services);
      }
      workflowExecution.setErrorStrategy(executionArgs.getErrorStrategy());
    }
    if (executionArgs.isTriggeredFromPipeline()) {
      if (executionArgs.getPipelineId() != null) {
        Pipeline pipeline =
            wingsPersistence.get(Pipeline.class, workflowExecution.getAppId(), executionArgs.getPipelineId());
        workflowExecution.setPipelineSummary(PipelineSummary.Builder.aPipelineSummary()
                                                 .withPipelineId(pipeline.getUuid())
                                                 .withPipelineName(pipeline.getName())
                                                 .build());
        keywords.add(pipeline.getName());
      }
    }

    workflowExecution.setKeywords(trimList(keywords));
    workflowExecution.setStatus(QUEUED);
    workflowExecution = wingsPersistence.saveAndGet(WorkflowExecution.class, workflowExecution);

    StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
    stateExecutionInstance.setAppId(workflowExecution.getAppId());
    stateExecutionInstance.setExecutionName(workflowExecution.getName());
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

    stdParams.setErrorStrategy(workflowExecution.getErrorStrategy());
    String workflowUrl = mainConfiguration.getPortal().getUrl() + "/"
        + format(mainConfiguration.getPortal().getExecutionUrlPattern(), workflowExecution.getAppId(),
              workflowExecution.getEnvId(), workflowExecution.getUuid());
    if (stdParams.getWorkflowElement() == null) {
      stdParams.setWorkflowElement(aWorkflowElement()
                                       .withUuid(workflowExecution.getUuid())
                                       .withName(workflowExecution.getName())
                                       .withUrl(workflowUrl)
                                       .build());
    } else {
      stdParams.getWorkflowElement().setName(workflowExecution.getName());
      stdParams.getWorkflowElement().setUuid(workflowExecution.getUuid());
      stdParams.getWorkflowElement().setUrl(workflowUrl);
    }
    WingsDeque<ContextElement> elements = new WingsDeque<>();
    elements.push(stdParams);
    if (contextElements != null) {
      for (ContextElement contextElement : contextElements) {
        elements.push(contextElement);
      }
    }
    stateExecutionInstance.setContextElements(elements);
    stateExecutionInstance = stateMachineExecutor.queue(stateMachine, stateExecutionInstance);
    boolean templatized = false;
    if (stateMachine.getOrchestrationWorkflow() != null && stateMachine.getOrchestrationWorkflow().isTemplatized()) {
      templatized = true;
    }
    if (templatized || workflowExecution.getWorkflowType() != ORCHESTRATION) {
      stateMachineExecutor.startExecution(stateMachine, stateExecutionInstance);
      updateStartStatus(workflowExecution, RUNNING);
    } else {
      // create queue event
      executionEventQueue.send(anExecutionEvent()
                                   .withAppId(workflowExecution.getAppId())
                                   .withWorkflowId(workflowExecution.getWorkflowId())
                                   .build());
    }
    return wingsPersistence.get(WorkflowExecution.class, workflowExecution.getAppId(), workflowExecution.getUuid());
  }

  @Override
  public void startQueuedExecution(String appId, String workflowId) {
    PageRequest pageRequest = aPageRequest()
                                  .addFilter("appId", EQ, appId)
                                  .addFilter("workflowId", EQ, workflowId)
                                  .addFilter("status", EQ, QUEUED)
                                  .addOrder("createdAt", OrderType.DESC)
                                  .build();

    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, pageRequest);
    if (workflowExecution == null) {
      return;
    }
    try (AcquiredLock lock = persistentLocker.tryToAcquireLock(
             Workflow.class, workflowExecution.getWorkflowId(), Duration.ofMinutes(1))) {
      if (lock == null) {
        return;
      }

      List<WorkflowExecution> runningWorkflowExecutions =
          getRunningWorkflowExecutions(ORCHESTRATION, workflowExecution.getAppId(), workflowExecution.getWorkflowId());
      if (isNotEmpty(runningWorkflowExecutions)) {
        return;
      }
      boolean started = stateMachineExecutor.startQueuedExecution(appId, workflowExecution.getUuid());
      ExecutionStatus status = RUNNING;
      if (!started) {
        status = ERROR;
      }
      logger.error("WorkflowExecution could not be started from QUEUED state- appId:{}, WorkflowExecution:{}", appId,
          workflowExecution.getUuid());
      updateStartStatus(workflowExecution, status);
    }
  }

  private void updateStartStatus(WorkflowExecution workflowExecution, ExecutionStatus status) {
    // TODO: findAndModify
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter("appId", workflowExecution.getAppId())
                                         .filter(ID_KEY, workflowExecution.getUuid())
                                         .field("status")
                                         .in(asList(NEW, QUEUED));
    UpdateOperations<WorkflowExecution> updateOps = wingsPersistence.createUpdateOperations(WorkflowExecution.class)
                                                        .set("status", status)
                                                        .set("startTs", System.currentTimeMillis());
    wingsPersistence.update(query, updateOps);

    notifyWorkflowExecution(workflowExecution);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WorkflowExecution triggerEnvExecution(String appId, String envId, ExecutionArgs executionArgs) {
    return triggerEnvExecution(appId, envId, executionArgs, null);
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
  WorkflowExecution triggerEnvExecution(
      String appId, String envId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    switch (executionArgs.getWorkflowType()) {
      case PIPELINE: {
        logger.debug("Received an pipeline execution request");
        if (executionArgs.getPipelineId() == null) {
          logger.error("pipelineId is null for an pipeline execution");
          throw new WingsException(ErrorCode.INVALID_REQUEST)
              .addParam("message", "pipelineId is null for an pipeline execution");
        }
        return triggerPipelineExecution(appId, executionArgs.getPipelineId(), executionArgs);
      }

      case ORCHESTRATION: {
        logger.debug("Received an orchestrated execution request");
        if (executionArgs.getOrchestrationId() == null) {
          logger.error("workflowId is null for an orchestrated execution");
          throw new WingsException(ErrorCode.INVALID_REQUEST)
              .addParam("message", "workflowId is null for an orchestrated execution");
        }
        return triggerOrchestrationExecution(appId, envId, executionArgs.getOrchestrationId(), executionArgs);
      }

      case SIMPLE: {
        logger.debug("Received an simple execution request");
        if (executionArgs.getServiceId() == null) {
          logger.error("serviceId is null for a simple execution");
          throw new WingsException(ErrorCode.INVALID_REQUEST)
              .addParam("message", "serviceId is null for a simple execution");
        }
        if (isEmpty(executionArgs.getServiceInstances())) {
          logger.error("serviceInstances are empty for a simple execution");
          throw new WingsException(ErrorCode.INVALID_REQUEST)
              .addParam("message", "serviceInstances are empty for a simple execution");
        }
        return triggerSimpleExecution(appId, envId, executionArgs, workflowExecutionUpdate);
      }

      default:
        throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "workflowType");
    }
  }

  /**
   * Trigger simple execution workflow execution.
   *
   * @param appId         the app id
   * @param envId         the env id
   * @param executionArgs the execution args
   * @return the workflow execution
   */
  private WorkflowExecution triggerSimpleExecution(
      String appId, String envId, ExecutionArgs executionArgs, WorkflowExecutionUpdate workflowExecutionUpdate) {
    Workflow workflow = workflowService.readLatestSimpleWorkflow(appId, envId);
    String workflowId = workflow.getUuid();

    StateMachine stateMachine = workflowService.readLatestStateMachine(appId, workflowId);
    if (stateMachine == null) {
      throw new WingsException("No stateMachine associated with " + workflowId);
    }

    WorkflowExecution workflowExecution = new WorkflowExecution();
    workflowExecution.setAppId(appId);
    workflowExecution.setEnvId(envId);
    workflowExecution.setEnvIds(asList(envId));
    workflowExecution.setWorkflowType(WorkflowType.SIMPLE);
    workflowExecution.setStateMachineId(stateMachine.getUuid());
    workflowExecution.setTotal(executionArgs.getServiceInstances().size());
    Service service = serviceResourceService.get(appId, executionArgs.getServiceId(), false);
    workflowExecution.setServiceIds(asList(executionArgs.getServiceId()));
    workflowExecution.setName(service.getName() + "/" + executionArgs.getCommandName());
    workflowExecution.setWorkflowId(workflow.getUuid());
    workflowExecution.setExecutionArgs(executionArgs);

    WorkflowStandardParams stdParams = new WorkflowStandardParams();
    stdParams.setAppId(appId);
    stdParams.setEnvId(envId);
    if (isNotEmpty(executionArgs.getArtifacts())) {
      stdParams.setArtifactIds(executionArgs.getArtifacts().stream().map(Artifact::getUuid).collect(toList()));
    }
    stdParams.setExecutionCredential(executionArgs.getExecutionCredential());

    SimpleWorkflowParam simpleOrchestrationParams = new SimpleWorkflowParam();
    simpleOrchestrationParams.setServiceId(executionArgs.getServiceId());
    if (executionArgs.getServiceInstances() != null) {
      simpleOrchestrationParams.setInstanceIds(
          executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(toList()));
    }
    simpleOrchestrationParams.setExecutionStrategy(executionArgs.getExecutionStrategy());
    simpleOrchestrationParams.setCommandName(executionArgs.getCommandName());
    return triggerExecution(
        workflowExecution, stateMachine, workflowExecutionUpdate, stdParams, simpleOrchestrationParams);
  }

  private List<WorkflowExecution> getRunningWorkflowExecutions(
      WorkflowType workflowType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("workflowId", EQ, workflowId)
                                                     .addFilter("workflowType", EQ, workflowType)
                                                     .addFilter("status", Operator.IN, NEW, QUEUED, RUNNING, PAUSED)
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
        wingsPersistence.get(WorkflowExecution.class, executionInterrupt.getAppId(), executionUuid);
    if (workflowExecution == null) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "No WorkflowExecution for executionUuid:" + executionUuid);
    }

    if (workflowExecution.getStatus().isFinalStatus()) {
      // There is a race between the workflow progress and request coming from the user.
      // It is completely normal the workflow to finish while interrupt request is coming.
      // Therefore there is nothing alarming when this occurs.
      throw new WingsException(ErrorCode.INVALID_REQUEST, HARMLESS)
          .addParam("args", "Workflow execution already completed. executionUuid:" + executionUuid);
    }

    if (workflowExecution.getWorkflowType() != PIPELINE) {
      executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      return executionInterrupt;
    }

    if (!(executionInterrupt.getExecutionInterruptType() == PAUSE_ALL
            || executionInterrupt.getExecutionInterruptType() == RESUME_ALL
            || executionInterrupt.getExecutionInterruptType() == ABORT_ALL)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST)
          .addParam("message", "Invalid ExecutionInterrupt: " + executionInterrupt);
    }

    try {
      executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
    } catch (WingsException exception) {
      exception.logProcessedMessages(logger);
    } catch (RuntimeException exception) {
      logger.error(format("Error in interrupting workflowExecution - uuid: %s, executionInterruptType: %s",
                       workflowExecution.getUuid(), executionInterrupt.getExecutionInterruptType()),
          exception);
    }

    List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(workflowExecution);
    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
      StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
      if (stateExecutionData == null || !(stateExecutionData instanceof EnvStateExecutionData)) {
        continue;
      }
      EnvStateExecutionData envStateExecutionData = (EnvStateExecutionData) stateExecutionData;
      WorkflowExecution workflowExecution2 =
          getWorkflowExecution(workflowExecution.getAppId(), envStateExecutionData.getWorkflowExecutionId());

      if (workflowExecution2 == null
          || (workflowExecution2.getStatus() != null && workflowExecution2.getStatus().isFinalStatus())) {
        continue;
      }

      try {
        ExecutionInterrupt executionInterruptClone = KryoUtils.clone(executionInterrupt);
        executionInterruptClone.setExecutionUuid(workflowExecution2.getUuid());
        executionInterruptManager.registerExecutionInterrupt(executionInterruptClone);
      } catch (WingsException exception) {
        exception.logProcessedMessages(logger);
      } catch (RuntimeException exception) {
        logger.error(format("Error in interrupting workflowExecution - uuid: %s, executionInterruptType: %s",
                         workflowExecution.getUuid(), executionInterrupt.getExecutionInterruptType()),
            exception);
      }
    }
    return executionInterrupt;
  }

  @Override
  public RequiredExecutionArgs getRequiredExecutionArgs(String appId, String envId, ExecutionArgs executionArgs) {
    notNullCheck("workflowType", executionArgs.getWorkflowType());

    if (executionArgs.getWorkflowType() == ORCHESTRATION || executionArgs.getWorkflowType() == ORCHESTRATION) {
      logger.debug("Received an orchestrated execution request");
      notNullCheck("orchestrationId", executionArgs.getOrchestrationId());

      Workflow workflow = workflowService.readWorkflow(appId, executionArgs.getOrchestrationId());
      if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
        throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", "OrchestrationWorkflow not found");
      }

      StateMachine stateMachine =
          workflowService.readStateMachine(appId, executionArgs.getOrchestrationId(), workflow.getDefaultVersion());
      if (stateMachine == null) {
        throw new WingsException(ErrorCode.INVALID_REQUEST).addParam("message", "Associated state machine not found");
      }

      RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
      requiredExecutionArgs.setEntityTypes(workflow.getOrchestrationWorkflow().getRequiredEntityTypes());
      return requiredExecutionArgs;

    } else if (executionArgs.getWorkflowType() == WorkflowType.SIMPLE) {
      logger.debug("Received an simple execution request");
      if (executionArgs.getServiceId() == null) {
        logger.error("serviceId is null for a simple execution");
        throw new WingsException(ErrorCode.INVALID_REQUEST)
            .addParam("message", "serviceId is null for a simple execution");
      }
      if (isEmpty(executionArgs.getServiceInstances())) {
        logger.error("serviceInstances are empty for a simple execution");
        throw new WingsException(ErrorCode.INVALID_REQUEST)
            .addParam("message", "serviceInstances are empty for a simple execution");
      }
      RequiredExecutionArgs requiredExecutionArgs = new RequiredExecutionArgs();
      if (isNotBlank(executionArgs.getCommandName())) {
        ServiceCommand command = serviceResourceService.getCommandByName(
            appId, executionArgs.getServiceId(), envId, executionArgs.getCommandName());
        if (command.getCommand().isArtifactNeeded()) {
          requiredExecutionArgs.getEntityTypes().add(ARTIFACT);
        }
      }
      List<String> serviceInstanceIds =
          executionArgs.getServiceInstances().stream().map(ServiceInstance::getUuid).collect(toList());
      Set<EntityType> infraReqEntityTypes =
          stateMachineExecutionSimulator.getInfrastructureRequiredEntityType(appId, serviceInstanceIds);
      if (infraReqEntityTypes != null) {
        requiredExecutionArgs.getEntityTypes().addAll(infraReqEntityTypes);
      }
      return requiredExecutionArgs;
    }

    return null;
  }

  @Override
  public boolean workflowExecutionsRunning(WorkflowType workflowType, String appId, String workflowId) {
    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("workflowId", EQ, workflowId)
                                                     .addFilter("workflowType", EQ, workflowType)
                                                     .addFilter("status", Operator.IN, NEW, RUNNING)
                                                     .addFieldsIncluded("uuid")
                                                     .build();

    PageResponse<WorkflowExecution> pageResponse = wingsPersistence.query(WorkflowExecution.class, pageRequest);
    if (isEmpty(pageResponse)) {
      return false;
    }
    return true;
  }

  private void notifyWorkflowExecution(WorkflowExecution workflowExecution) {
    EntityType entityType = ORCHESTRATED_DEPLOYMENT;
    if (workflowExecution.getWorkflowType() == WorkflowType.SIMPLE) {
      entityType = SIMPLE_DEPLOYMENT;
    }
  }

  @Override
  public CountsByStatuses getBreakdown(String appId, String workflowExecutionId) {
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
    refreshBreakdown(workflowExecution);
    return workflowExecution.getBreakdown();
  }

  @Override
  public GraphNode getExecutionDetailsForNode(
      String appId, String workflowExecutionId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    return graphRenderer.convertToNode(stateExecutionInstance);
  }

  @Override
  public List<StateExecutionData> getExecutionHistory(
      String appId, String workflowExecutionId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    return stateExecutionInstance.getStateExecutionDataHistory();
  }

  @Override
  public long getExecutionInterruptCount(String stateExecutionInstanceId) {
    return wingsPersistence.createQuery(ExecutionInterrupt.class)
        .filter("stateExecutionInstanceId", stateExecutionInstanceId)
        .count();
  }

  @Override
  public List<StateExecutionInterrupt> getExecutionInterrupts(String appId, String stateExecutionInstanceId) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    Validator.notNullCheck("stateExecutionInstance", stateExecutionInstance);

    Map<String, ExecutionInterruptEffect> map = new HashMap<>();
    stateExecutionInstance.getInterruptHistory().stream().forEach(effect -> map.put(effect.getInterruptId(), effect));

    List<StateExecutionInterrupt> interrupts = wingsPersistence.createQuery(ExecutionInterrupt.class)
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
          .field(ID_KEY)
          .in(map.keySet())
          .asList()
          .stream()
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
        wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
    Validator.notNullCheck("stateExecutionInstance", stateExecutionInstance);

    StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
    Validator.notNullCheck("stateExecutionData", stateExecutionData);
    if (!(stateExecutionData instanceof RepeatStateExecutionData)) {
      throw new InvalidRequestException(
          "Request for elements of instance that is not repeated", WingsException.ReportTarget.USER);
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

    final StateMachine stateMachine = wingsPersistence.get(
        StateMachine.class, stateExecutionInstance.getAppId(), stateExecutionInstance.getStateMachineId());

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
    MorphiaIterator<StateExecutionInstance, StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstance.APP_ID_KEY, appId)
            .filter(StateExecutionInstance.EXECUTION_UUID_KEY, stateExecutionInstance.getExecutionUuid())
            .filter(StateExecutionInstance.PARENT_INSTANCE_ID_KEY, stateExecutionInstanceId)
            .project(StateExecutionInstance.ID_KEY, true)
            .project(StateExecutionInstance.STATUS_KEY, true)
            .project(StateExecutionInstance.PREV_INSTANCE_ID_KEY, true)
            .project(StateExecutionInstance.CONTEXT_ELEMENT_KEY, true)
            .fetch();

    try (DBCursor cursor = stateExecutionInstances.getCursor()) {
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
    return wingsPersistence.get(StateExecutionInstance.class, appId, stateExecutionInstanceId);
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
        wingsPersistence.query(StateExecutionInstance.class, pageRequest, true);
    return query.getResponse();
  }

  @Override
  public void deleteByWorkflow(String appId, String workflowId) {
    wingsPersistence.createQuery(WorkflowExecution.class)
        .filter("appId", appId)
        .filter("workflowId", workflowId)
        .asList()
        .forEach(workflowExecution -> {
          wingsPersistence.delete(workflowExecution);
          wingsPersistence.createQuery(StateExecutionInstance.class)
              .filter("appId", appId)
              .filter("stateMachineId", workflowExecution.getStateMachineId())
              .forEach(stateExecutionInstance -> {
                wingsPersistence.delete(stateExecutionInstance);
                wingsPersistence.delete(wingsPersistence.createQuery(ExecutionInterrupt.class)
                                            .filter("appId", appId)
                                            .filter("stateExecutionInstanceId", stateExecutionInstance.getUuid()));
              });
        });
  }

  private void refreshSummaries(WorkflowExecution workflowExecution) {
    if (workflowExecution.getServiceExecutionSummaries() != null) {
      return;
    }
    List<ElementExecutionSummary> serviceExecutionSummaries = new ArrayList<>();
    // TODO : version should also be captured as part of the WorkflowExecution
    Workflow workflow = workflowService.readWorkflow(workflowExecution.getAppId(), workflowExecution.getWorkflowId());
    if (workflow != null && workflow.getOrchestrationWorkflow() != null) {
      List<Service> services;
      if (isServiceTemplatized(workflow)) {
        Map<String, String> workflowVariables = workflowExecution.getExecutionArgs() != null
            ? workflowExecution.getExecutionArgs().getWorkflowVariables()
            : null;
        services = workflowService.resolveServices(workflow, workflowVariables);
      } else {
        services = workflow.getServices();
      }
      if (workflow.getWorkflowType() == WorkflowType.SIMPLE) {
        services = asList(serviceResourceService.get(
            workflow.getAppId(), workflowExecution.getExecutionArgs().getServiceId(), false));
      }
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
                infraMappingSummaries.add(anInfraMappingSummary()
                                              .withInframappingId(infraMapping.getUuid())
                                              .withInfraMappingType(infraMapping.getInfraMappingType())
                                              .withComputerProviderName(infraMapping.getComputeProviderName())
                                              .withDisplayName(infraMapping.getName())
                                              .withDeploymentType(infraMapping.getDeploymentType())
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

      if (workflowExecution.getStatus() == ExecutionStatus.SUCCESS
          || workflowExecution.getStatus() == ExecutionStatus.FAILED
          || workflowExecution.getStatus() == ExecutionStatus.ERROR
          || workflowExecution.getStatus() == ExecutionStatus.ABORTED) {
        wingsPersistence.updateField(WorkflowExecution.class, workflowExecution.getUuid(), "serviceExecutionSummaries",
            workflowExecution.getServiceExecutionSummaries());
      }
    }
  }

  @Override
  public List<InfrastructureMapping> getResolvedInfraMappings(Workflow workflow, WorkflowExecution workflowExecution) {
    if (isInfraMappingTemplatized(workflow)) {
      return resolveTemplateInfraMappings(workflow, workflowExecution);
    } else {
      return getInfrastructureMappings(workflow, workflow.getOrchestrationWorkflow().getInfraMappingIds());
    }
  }

  private List<InfrastructureMapping> resolveTemplateInfraMappings(
      Workflow workflow, WorkflowExecution workflowExecution) {
    // Lookup service
    List<String> workflowInframappingIds = workflow.getOrchestrationWorkflow().getInfraMappingIds();
    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow =
        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
    List<Variable> userVariables = canaryOrchestrationWorkflow.getUserVariables();
    List<String> infraMappingNames = new ArrayList<>();
    if (userVariables != null) {
      infraMappingNames =
          userVariables.stream()
              .filter(variable
                  -> variable.getEntityType() != null && variable.getEntityType().equals(INFRASTRUCTURE_MAPPING))
              .map(Variable::getName)
              .collect(toList());
    }
    List<String> infraMappingIds = new ArrayList<>();
    Map<String, String> workflowVariables = workflowExecution.getExecutionArgs() != null
        ? workflowExecution.getExecutionArgs().getWorkflowVariables()
        : null;
    if (workflowVariables != null) {
      Set<String> workflowVariableNames = workflowVariables.keySet();
      for (String variableName : workflowVariableNames) {
        if (infraMappingNames.contains(variableName)) {
          infraMappingIds.add(workflowVariables.get(variableName));
        }
      }
    }
    List<String> templatizedInfraMappingIdsIds = canaryOrchestrationWorkflow.getTemplatizedInfraMappingIds();
    if (workflowInframappingIds != null) {
      for (String workflowServiceId : workflowInframappingIds) {
        if (!templatizedInfraMappingIdsIds.contains(workflowServiceId)) {
          infraMappingIds.add(workflowServiceId);
        }
      }
    }
    if (!infraMappingIds.isEmpty()) {
      return getInfrastructureMappings(workflow, infraMappingIds);
    } else {
      logger.info("No inframappings resolved for templatized workflow id {} and workflow execution {}",
          workflow.getUuid(), workflowExecution);
      return new ArrayList<>();
    }
  }

  private List<InfrastructureMapping> getInfrastructureMappings(Workflow workflow, List<String> infraMappingIds) {
    if (isNotEmpty(infraMappingIds)) {
      PageRequest<InfrastructureMapping> pageRequest = aPageRequest()
                                                           .withLimit(PageRequest.UNLIMITED)
                                                           .addFilter("appId", EQ, workflow.getAppId())
                                                           .addFilter("uuid", IN, infraMappingIds.toArray())
                                                           .build();
      return infrastructureMappingService.list(pageRequest);
    }
    return new ArrayList<>();
  }

  /**
   * Checks if service templatized or not
   * @param workflow
   * @return
   */
  private boolean isServiceTemplatized(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow != null) {
      return orchestrationWorkflow.isServiceTemplatized();
    }
    return false;
  }

  /**
   * Checks if service templatized or not
   * @param workflow
   * @return
   */
  private boolean isInfraMappingTemplatized(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
    if (orchestrationWorkflow != null) {
      return orchestrationWorkflow.isInfraMappingTemplatized();
    }
    return false;
  }

  private void populateServiceSummary(
      Map<String, ElementExecutionSummary> serviceSummaryMap, WorkflowExecution workflowExecution) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(UNLIMITED)
            .addFilter("appId", EQ, workflowExecution.getAppId())
            .addFilter("executionUuid", EQ, workflowExecution.getUuid())
            .addFilter("stateType", Operator.IN, StateType.REPEAT.name(), StateType.FORK.name(),
                StateType.SUB_WORKFLOW.name(), StateType.PHASE.name(), StateType.PHASE_STEP.name())
            .addFilter("parentInstanceId", Operator.NOT_EXISTS)
            .addFilter("createdAt", GE, workflowExecution.getCreatedAt())
            .addOrder("createdAt", OrderType.ASC)
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
    if (workflowExecution.getStatus().isFinalStatus() && workflowExecution.getBreakdown() != null) {
      return;
    }
    CountsByStatuses breakdown = null;
    int total;

    if (workflowExecution.getOrchestrationType() == OrchestrationWorkflowType.ROLLING) {
      breakdown = new CountsByStatuses();
      total = workflowExecution.getTotal();
      if (total == 0) {
        total = refreshTotal(workflowExecution);
      }
      breakdown = getBreakdownFromPhases(workflowExecution);
      breakdown.setQueued(total - (breakdown.getFailed() + breakdown.getSuccess() + breakdown.getInprogress()));
    } else {
      StateMachine sm =
          wingsPersistence.get(StateMachine.class, workflowExecution.getAppId(), workflowExecution.getStateMachineId());
      PageRequest<StateExecutionInstance> req =
          aPageRequest()
              .withReadPref(CRITICAL)
              .withLimit(PageRequest.UNLIMITED)
              .addFilter("appId", EQ, workflowExecution.getAppId())
              .addFilter("executionUuid", EQ, workflowExecution.getUuid())
              .addFilter("createdAt", GE, workflowExecution.getCreatedAt())
              .addFieldsIncluded("uuid", "displayName", "contextElement", "parentInstanceId", "status")
              .build();

      List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(req);

      breakdown = stateMachineExecutionSimulator.getStatusBreakdown(
          workflowExecution.getAppId(), workflowExecution.getEnvId(), sm, allStateExecutionInstances);
      total = breakdown.getFailed() + breakdown.getSuccess() + breakdown.getInprogress() + breakdown.getQueued();
    }

    workflowExecution.setBreakdown(breakdown);
    workflowExecution.setTotal(total);
    logger.info("Got the breakdown workflowExecution: {}, status: {}, breakdown: {}", workflowExecution.getUuid(),
        workflowExecution.getStatus(), breakdown);

    if (workflowExecution.getStatus().isFinalStatus()) {
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
    PageRequest<StateExecutionInstance> req = aPageRequest()
                                                  .withReadPref(CRITICAL)
                                                  .withLimit(PageRequest.UNLIMITED)
                                                  .addFilter("appId", EQ, workflowExecution.getAppId())
                                                  .addFilter("executionUuid", EQ, workflowExecution.getUuid())
                                                  .addFilter("createdAt", GE, workflowExecution.getCreatedAt())
                                                  .addFilter("stateType", EQ, PHASE.name())
                                                  .addFilter("rollback", EQ, false)
                                                  .build();

    List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(req);
    if (isEmpty(allStateExecutionInstances)) {
      breakdown.setSuccess(0);
    }
    for (StateExecutionInstance stateExecutionInstance : allStateExecutionInstances) {
      StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
      if (stateExecutionData == null || !(stateExecutionData instanceof PhaseExecutionData)) {
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
    List<InfrastructureMapping> resolvedInfraMappings = getResolvedInfraMappings(workflow, workflowExecution);
    if (isEmpty(resolvedInfraMappings)) {
      return 0;
    }
    return infrastructureMappingService.listHosts(workflowExecution.getAppId(), resolvedInfraMappings.get(0).getUuid())
        .size();
  }

  private List<StateExecutionInstance> getAllStateExecutionInstances(PageRequest<StateExecutionInstance> req) {
    return wingsPersistence.queryAll(StateExecutionInstance.class, req);
  }

  @Override
  public List<ElementExecutionSummary> getElementsSummary(
      String appId, String executionUuid, String parentStateExecutionInstanceId) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .addFilter("parentInstanceId", Operator.IN, parentStateExecutionInstanceId)
            .addOrder("createdAt", OrderType.ASC)
            .addFieldsExcluded("contextElements")
            .build();

    List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(pageRequest);
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
        if (nextStateType == null) {
          continue;
        }
        if ((nextStateType == StateType.REPEAT || nextStateType == StateType.FORK || nextStateType == StateType.PHASE
                || nextStateType == StateType.PHASE_STEP || nextStateType == StateType.SUB_WORKFLOW)
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
    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .addFilter("parentInstanceId", Operator.IN, stateExecutionInstanceId)
            .addFilter("stateType", EQ, StateType.PHASE_STEP.name())
            .addFieldsIncluded(
                "uuid", "parentInstanceId", "contextElement", "status", "stateType", "displayName", "stateExecutionMap")
            .build();

    List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(pageRequest);
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

    List<String> parentInstanceIds = asList(stateExecutionInstanceId);
    while (isNotEmpty(parentInstanceIds)) {
      PageRequest<StateExecutionInstance> pageRequest =
          aPageRequest()
              .withReadPref(CRITICAL)
              .withLimit(PageRequest.UNLIMITED)
              .addFilter("appId", EQ, appId)
              .addFilter("executionUuid", EQ, executionUuid)
              .addFilter("parentInstanceId", Operator.IN, parentInstanceIds.toArray())
              .addFieldsIncluded("uuid", "parentInstanceId", "contextElement", "status", "stateType", "displayName",
                  "stateExecutionMap")
              .build();

      List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(pageRequest);
      if (isEmpty(allStateExecutionInstances)) {
        return null;
      }

      allStateExecutionInstances.stream()
          .filter(instance -> !StateType.REPEAT.name().equals(instance.getStateType()))
          .filter(instance -> !StateType.FORK.name().equals(instance.getStateType()))
          .forEach(
              instance -> stepExecutionSummaryList.add(instance.getStateExecutionData().getStepExecutionSummary()));

      parentInstanceIds = allStateExecutionInstances.stream()
                              .filter(instance
                                  -> StateType.REPEAT.name().equals(instance.getStateType())
                                      || StateType.FORK.name().equals(instance.getStateType()))
                              .map(StateExecutionInstance::getUuid)
                              .collect(toList());
    }

    return phaseStepExecutionSummary;
  }

  public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Map<Object, Boolean> seen = new ConcurrentHashMap<>();
    return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
  }

  public List<Artifact> getArtifactsCollected(String appId, String executionUuid) {
    PageRequest<StateExecutionInstance> pageRequest =
        aPageRequest()
            .withReadPref(CRITICAL)
            .withLimit(UNLIMITED)
            .addFilter("appId", EQ, appId)
            .addFilter("executionUuid", EQ, executionUuid)
            .addFilter("stateType", EQ, StateType.ARTIFACT_COLLECTION.name())
            .build();

    List<StateExecutionInstance> allStateExecutionInstances = getAllStateExecutionInstances(pageRequest);
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
        wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId, CRITICAL);
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
    WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
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

      case SIMPLE:
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
            wingsPersistence.createQuery(StateExecutionInstance.class).filter("executionUuid", executionUuid).asList();

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
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams.getEnv().getUuid();
    PhaseElement phaseElement = executionContext.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    PageRequest<WorkflowExecutionBaseline> pageRequest =
        aPageRequest()
            .addFilter("workflowExecutionId", Operator.EQ, workflowExecutionId)
            .addFilter("envId", Operator.EQ, envId)
            .addFilter("serviceId", Operator.EQ, serviceId)
            .addFilter("appId", Operator.EQ, appId)
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
}
