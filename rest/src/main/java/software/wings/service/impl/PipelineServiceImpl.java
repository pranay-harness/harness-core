package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.api.ApprovalStateExecutionData.Builder.anApprovalStateExecutionData;
import static software.wings.beans.ApprovalDetails.Action.APPROVE;
import static software.wings.beans.ApprovalDetails.Action.REJECT;
import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.PIPELINE_EXECUTION_IN_PROGRESS;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.PipelineStageExecution.Builder.aPipelineStageExecution;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.WorkflowDetails.Builder.aWorkflowDetails;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.sm.ExecutionInterruptType.ABORT_ALL;
import static software.wings.sm.ExecutionInterruptType.PAUSE_ALL;
import static software.wings.sm.ExecutionInterruptType.RESUME_ALL;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.PAUSING;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.ExecutionStatus.WAITING;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import de.danielbechler.util.Collections;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ApprovalStateExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.User;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowDetails;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.Builder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;
import software.wings.utils.KryoUtils;
import software.wings.utils.Validator;
import software.wings.waitnotify.WaitNotifyEngine;
import software.wings.yaml.gitSync.EntityUpdateEvent.SourceType;
import software.wings.yaml.gitSync.EntityUpdateListEvent;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 10/26/16.
 */
@Singleton
@ValidateOnExecution
public class PipelineServiceImpl implements PipelineService {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private ArtifactService artifactService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ExecutionInterruptManager executionInterruptManager;
  @Inject private EntityUpdateService entityUpdateService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public PageResponse<PipelineExecution> listPipelineExecutions(PageRequest<PipelineExecution> pageRequest) {
    PageResponse<PipelineExecution> pageResponse = wingsPersistence.query(PipelineExecution.class, pageRequest);
    for (PipelineExecution pipelineExecution : pageResponse.getResponse()) {
      try {
        refreshPipelineExecution(pipelineExecution);
        setPipelineUuid(pipelineExecution);
      } catch (Exception e) {
        logger.error("Failed to refresh pipeline execution {} ", pipelineExecution, e);
      }
    }
    return pageResponse;
  }

  /**
   * Replacing embedded pipeline id with the real pipeline id
   * @param pipelineExecution
   */
  private void setPipelineUuid(PipelineExecution pipelineExecution) {
    if (pipelineExecution == null || pipelineExecution.getPipeline() == null) {
      return;
    }
    pipelineExecution.getPipeline().setUuid(pipelineExecution.getPipelineId());
  }

  private void refreshPipelineExecution(PipelineExecution pipelineExecution) {
    if (pipelineExecution == null || pipelineExecution.getStatus().isFinalStatus()) {
      return;
    }
    Pipeline pipeline = pipelineExecution.getPipeline();

    ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap =
        getStateExecutionInstanceMap(pipelineExecution);
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
                                                        .withStateName(stateExecutionInstance.getStateName())
                                                        .withStartTs(stateExecutionInstance.getStartTs())
                                                        .withEndTs(stateExecutionInstance.getEndTs())
                                                        .build();
            StateExecutionData stateExecutionData =
                stateExecutionInstance.getStateExecutionMap().get(pipelineStageElement.getName());

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

            StateExecutionData stateExecutionData =
                stateExecutionInstance.getStateExecutionMap().get(pipelineStageElement.getName());

            if (stateExecutionData != null && stateExecutionData instanceof EnvStateExecutionData) {
              EnvStateExecutionData envStateExecutionData = (EnvStateExecutionData) stateExecutionData;
              WorkflowExecution workflowExecution = workflowExecutionService.getExecutionDetails(
                  pipelineExecution.getAppId(), envStateExecutionData.getWorkflowExecutionId());
              stageExecution.setWorkflowExecutions(asList(workflowExecution));
            }

            stageExecutionDataList.add(stageExecution);
          } else {
            throw new WingsException(
                ErrorCode.INVALID_REQUEST, "message", "Unknown stateType " + stateExecutionInstance.getStateType());
          }
        });

    WorkflowExecution executionDetails = workflowExecutionService.getExecutionDetailsWithoutGraph(
        pipelineExecution.getAppId(), pipelineExecution.getWorkflowExecutionId());
    pipelineExecution.setPipelineStageExecutions(stageExecutionDataList);

    boolean allStatesFinishedExecution = stateExecutionInstanceMap.values().stream().allMatch(
        stateExecutionInstance -> stateExecutionInstance.getStatus().isFinalStatus());

    if (allStatesFinishedExecution) { // do not change pipeExecution status from Running until all state finish
      pipelineExecution.setStatus(executionDetails.getStatus());
      pipelineExecution.setEndTs(executionDetails.getEndTs());
    } else {
      if (stateExecutionInstanceMap.values().stream().anyMatch(stateExecutionInstance
              -> stateExecutionInstance.getStatus() == PAUSED || stateExecutionInstance.getStatus() == PAUSING)) {
        pipelineExecution.setStatus(PAUSED);
      } else if (stateExecutionInstanceMap.values().stream().anyMatch(
                     stateExecutionInstance -> stateExecutionInstance.getStatus() == WAITING)) {
        pipelineExecution.setStatus(WAITING);
      } else {
        // Verify if any workflow execution is in Paused state
        if (pipelineExecution.getPipelineStageExecutions()
                .stream()
                .flatMap(executions -> executions.getWorkflowExecutions().stream())
                .anyMatch(workflowExecution
                    -> workflowExecution.getStatus() == PAUSED || workflowExecution.getStatus() == PAUSING)) {
          pipelineExecution.setStatus(PAUSED);
        } else if (pipelineExecution.getPipelineStageExecutions()
                       .stream()
                       .flatMap(executions -> executions.getWorkflowExecutions().stream())
                       .anyMatch(workflowExecution -> workflowExecution.getStatus() == WAITING)) {
          pipelineExecution.setStatus(WAITING);
        } else {
          pipelineExecution.setStatus(ExecutionStatus.RUNNING);
        }
      }
    }

    try {
      wingsPersistence.merge(pipelineExecution);
      executorService.submit(() -> updatePipelineEstimates(pipelineExecution));
    } catch (ConcurrentModificationException cex) {
      // do nothing as it gets refreshed in next fetch
      logger.warn("Pipeline execution update failed ", cex); // TODO: add retry
    }
  }

  private void updatePipelineEstimates(PipelineExecution pipelineExecution) {
    if (pipelineExecution.getStatus().isFinalStatus()) {
      PageRequest pageRequest = aPageRequest()
                                    .addFilter("appId", EQ, pipelineExecution.getAppId())
                                    .addFilter("pipelineId", EQ, pipelineExecution.getPipelineId())
                                    .addFilter("status", EQ, SUCCESS)
                                    .addOrder("endTs", OrderType.DESC)
                                    .withLimit("5")
                                    .build();
      List<PipelineExecution> pipelineExecutions = listPipelineExecutions(pageRequest).getResponse();

      Map<String, LongSummaryStatistics> stateEstimatesSum =
          pipelineExecutions.stream()
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
      wingsPersistence.update(pipelineExecution.getPipeline(),
          wingsPersistence.createUpdateOperations(Pipeline.class).set("stateEtaMap", newEstimates));
    }
  }

  private Long getEstimate(PipelineStageExecution pipelineStageExecution) {
    if (pipelineStageExecution.getEndTs() != null && pipelineStageExecution.getStartTs() != null
        && pipelineStageExecution.getEndTs() > pipelineStageExecution.getStartTs()) {
      return pipelineStageExecution.getEndTs() - pipelineStageExecution.getStartTs();
    }
    return null;
  }

  private ImmutableMap<String, StateExecutionInstance> getStateExecutionInstanceMap(
      PipelineExecution pipelineExecution) {
    List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(pipelineExecution);
    return Maps.uniqueIndex(stateExecutionInstances, v -> v.getStateName());
  }

  private List<StateExecutionInstance> getStateExecutionInstances(PipelineExecution pipelineExecution) {
    PageRequest<StateExecutionInstance> req =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("appId", EQ, pipelineExecution.getAppId())
            .addFilter("executionUuid", EQ, pipelineExecution.getWorkflowExecutionId())
            .build();
    return wingsPersistence.query(StateExecutionInstance.class, req).getResponse();
  }

  @Override
  public void refreshPipelineExecutionAsync(String appId, String workflowExecutionId) {
    executorService.submit(() -> refreshPipelineExecution(appId, workflowExecutionId));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Pipeline> listPipelines(PageRequest<Pipeline> pageRequest) {
    PageResponse<Pipeline> res = wingsPersistence.query(Pipeline.class, pageRequest);
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pipeline updatePipeline(Pipeline pipeline) {
    validatePipeline(pipeline);
    UpdateOperations<Pipeline> ops = wingsPersistence.createUpdateOperations(Pipeline.class);
    setUnset(ops, "description", pipeline.getDescription());
    setUnset(ops, "name", pipeline.getName());
    setUnset(ops, "pipelineStages", pipeline.getPipelineStages());

    wingsPersistence.update(wingsPersistence.createQuery(Pipeline.class)
                                .field("appId")
                                .equal(pipeline.getAppId())
                                .field(ID_KEY)
                                .equal(pipeline.getUuid()),
        ops);

    //-------------------
    EntityUpdateListEvent eule = new EntityUpdateListEvent();

    // see if we need to perform any Git Sync operations for the pipeline
    eule.addEntityUpdateEvent(entityUpdateService.pipelineListUpdate(pipeline, SourceType.ENTITY_UPDATE));

    entityUpdateService.queueEntityUpdateList(eule);
    //-------------------

    wingsPersistence.saveAndGet(StateMachine.class, new StateMachine(pipeline, workflowService.stencilMap()));
    return pipeline;
  }

  @Override
  public boolean deletePipeline(String appId, String pipelineId) {
    return deletePipeline(appId, pipelineId, false);
  }

  private boolean deletePipeline(String appId, String pipelineId, boolean forceDelete) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    if (pipeline == null) {
      return true;
    }
    boolean deleted = false;

    if (forceDelete) {
      deleted = wingsPersistence.delete(pipeline);
    } else {
      PageRequest<PipelineExecution> pageRequest =
          aPageRequest().addFilter("appId", EQ, appId).addFilter("pipelineId", EQ, pipelineId).build();
      PageResponse<PipelineExecution> pageResponse = wingsPersistence.query(PipelineExecution.class, pageRequest);
      if (pageResponse == null || CollectionUtils.isEmpty(pageResponse.getResponse())
          || pageResponse.getResponse().stream().allMatch(
                 pipelineExecution -> pipelineExecution.getStatus().isFinalStatus())) {
        deleted = wingsPersistence.delete(pipeline);
      } else {
        String message = String.format("Pipeline:[%s] couldn't be deleted", pipeline.getName());
        throw new WingsException(PIPELINE_EXECUTION_IN_PROGRESS, "message", message);
      }
    }
    if (deleted) {
      executorService.submit(() -> artifactStreamService.deleteStreamActionForWorkflow(appId, pipelineId));
    }
    return deleted;
  }

  @Override
  public boolean deletePipelineByApplication(String appId) {
    List<Key<Pipeline>> pipelineKeys =
        wingsPersistence.createQuery(Pipeline.class).field("appId").equal(appId).asKeyList();
    for (Key key : pipelineKeys) {
      deletePipeline(appId, (String) key.getId(), true);
    }
    return false;
  }

  @Override
  public Pipeline clonePipeline(String originalPipelineId, Pipeline pipeline) {
    Pipeline originalPipeline = readPipeline(pipeline.getAppId(), originalPipelineId, false);
    Pipeline clonedPipleline = originalPipeline.clone();
    clonedPipleline.setName(pipeline.getName());
    clonedPipleline.setDescription(pipeline.getDescription());
    return createPipeline(clonedPipleline);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Pipeline readPipeline(String appId, String pipelineId, boolean withServices) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    if (withServices) {
      populateAssociatedWorkflowServices(pipeline);
    }
    return pipeline;
  }

  private void populateAssociatedWorkflowServices(Pipeline pipeline) {
    List<Service> services = new ArrayList<>();
    Set<String> serviceIds = new HashSet();
    List<WorkflowDetails> workflowDetails = new ArrayList<>();
    pipeline.getPipelineStages()
        .stream()
        .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
        .filter(pipelineStageElement -> ENV_STATE.name().equals(pipelineStageElement.getType()))
        .forEach(pse -> {
          try {
            Workflow workflow =
                workflowService.readWorkflow(pipeline.getAppId(), (String) pse.getProperties().get("workflowId"));
            OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
            List<Variable> variables = new ArrayList<>();
            if (orchestrationWorkflow != null) {
              if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(BASIC)) {
                variables = ((BasicOrchestrationWorkflow) orchestrationWorkflow).getUserVariables();
              } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(CANARY)) {
                variables = ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getUserVariables();
              } else if (orchestrationWorkflow.getOrchestrationWorkflowType().equals(MULTI_SERVICE)) {
                variables = ((MultiServiceOrchestrationWorkflow) orchestrationWorkflow).getUserVariables();
              }
              if (variables.size() > 0) {
                WorkflowDetails workflowDetail = aWorkflowDetails()
                                                     .withWorkflowId(workflow.getUuid())
                                                     .withWorkflowName(workflow.getName())
                                                     .withPipelineStageName(pse.getName())
                                                     .withVariables(variables)
                                                     .build();
                workflowDetails.add(workflowDetail);
              }
            }
            workflow.getServices().forEach(service -> {
              if (!serviceIds.contains(service.getUuid())) {
                services.add(service);
                serviceIds.add(service.getUuid());
              }
            });
          } catch (Exception ex) {
            logger.warn("Exception occurred while reading workflow associated to the pipeline {}", pipeline);
          }
        });

    pipeline.setServices(services);
    pipeline.setWorkflowDetails(workflowDetails);
  }

  @Override
  public Pipeline createPipeline(Pipeline pipeline) {
    validatePipeline(pipeline);
    pipeline = wingsPersistence.saveAndGet(Pipeline.class, pipeline);
    Map<StateTypeScope, List<Stencil>> stencils = workflowService.stencils(null, null, null);
    wingsPersistence.saveAndGet(StateMachine.class, new StateMachine(pipeline, workflowService.stencilMap()));
    return pipeline;
  }

  private void validatePipeline(Pipeline pipeline) {
    if (Collections.isEmpty(pipeline.getPipelineStages())) {
      throw new WingsException(INVALID_ARGUMENT, "args", "At least one pipeline stage required");
    }
    for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
      if (pipelineStage.getPipelineStageElements() == null || pipelineStage.getPipelineStageElements().size() == 0) {
        throw new WingsException(INVALID_ARGUMENT, "args", "Invalid pipeline stage");
      }

      for (PipelineStageElement stageElement : pipelineStage.getPipelineStageElements()) {
        if (ENV_STATE.name().equals(stageElement.getType())
            && (isNullOrEmpty((String) stageElement.getProperties().get("envId"))
                   || isNullOrEmpty((String) stageElement.getProperties().get("workflowId")))) {
          throw new WingsException(
              INVALID_ARGUMENT, "args", "Workflow or Environment can not be null for Environment state");
        }
      }
    }
  }

  @Override
  public void refreshPipelineExecution(String appId, String workflowExecutionId) {
    PipelineExecution pipelineExecution = wingsPersistence.get(PipelineExecution.class, appId, workflowExecutionId);
    refreshPipelineExecution(pipelineExecution);
  }

  @Override
  public WorkflowExecution execute(String appId, String pipelineId, ExecutionArgs executionArgs) {
    WorkflowExecution workflowExecution =
        workflowExecutionService.triggerPipelineExecution(appId, pipelineId, executionArgs);
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);

    // Do not remove this. Morphia referencing it by id and one object getting overridden by the other
    pipeline.setUuid(UUIDGenerator.getUuid() + "_embedded");

    Application application = appService.get(appId);
    List<Artifact> artifacts = validateAndFetchArtifact(appId, executionArgs.getArtifacts());
    executionArgs.setArtifacts(artifacts);
    Artifact artifact = artifacts.get(0);
    PipelineExecution pipelineExecution = aPipelineExecution()
                                              .withAppId(appId)
                                              .withAppName(application.getName())
                                              .withPipelineId(pipelineId)
                                              .withPipeline(pipeline)
                                              .withWorkflowExecutionId(workflowExecution.getUuid())
                                              .withWorkflowType(PIPELINE)
                                              .withStatus(workflowExecution.getStatus())
                                              .withStartTs(System.currentTimeMillis())
                                              .withArtifactId(artifact.getUuid())
                                              .withArtifactName(artifact.getDisplayName())
                                              .withStateMachineId(workflowExecution.getStateMachineId())
                                              .withExecutionArgs(executionArgs)
                                              .build();
    pipelineExecution = wingsPersistence.saveAndGet(PipelineExecution.class, pipelineExecution);
    refreshPipelineExecution(appId, pipelineExecution.getWorkflowExecutionId());
    return workflowExecution;
  }

  @Override
  public boolean approveOrRejectExecution(String appId, String pipelineExecutionId, ApprovalDetails approvalDetails) {
    Validator.notNullCheck("ApprovalDetails", approvalDetails);
    String approvalId = approvalDetails.getApprovalId();
    if (!isPipelineWaitingApproval(appId, pipelineExecutionId, approvalId)) {
      throw new WingsException(INVALID_ARGUMENT, "args",
          "No Pipeline execution [" + pipelineExecutionId
              + "] waiting for approval id: " + approvalDetails.getApprovalId());
    }
    User user = UserThreadLocal.get();
    if (user != null) {
      approvalDetails.setApprovedBy(anEmbeddedUser().withEmail(user.getEmail()).withName(user.getName()).build());
    }
    ApprovalStateExecutionData executionData = null;
    if (approvalDetails.getAction() == null || approvalDetails.getAction().equals(APPROVE)) {
      logger.debug("Notifying to approve the pipeline execution {} for approval id {} ", pipelineExecutionId,
          approvalDetails.getApprovalId());
      executionData = anApprovalStateExecutionData()
                          .withStatus(ExecutionStatus.SUCCESS)
                          .withApprovedBy(approvalDetails.getApprovedBy())
                          .withComments(approvalDetails.getComments())
                          .build();
    } else if (approvalDetails.getAction().equals(REJECT)) {
      logger.debug("Notifying to reject the pipeline execution {} for approval id {} ", pipelineExecutionId,
          approvalDetails.getApprovalId());
      executionData = anApprovalStateExecutionData()
                          .withStatus(ExecutionStatus.ABORTED)
                          .withApprovedBy(approvalDetails.getApprovedBy())
                          .withComments(approvalDetails.getComments())
                          .build();
    }
    waitNotifyEngine.notify(approvalDetails.getApprovalId(), executionData);
    return true;
  }

  private boolean isPipelineWaitingApproval(String appId, String executionUuid, String approvalId) {
    PageRequest<StateExecutionInstance> req = aPageRequest()
                                                  .withLimit(UNLIMITED)
                                                  .addFilter("appId", EQ, appId)
                                                  .addFilter("executionUuid", EQ, executionUuid)
                                                  .addFilter("stateType", EQ, APPROVAL)
                                                  .build();
    List<StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.query(StateExecutionInstance.class, req).getResponse();
    if (CollectionUtils.isEmpty(stateExecutionInstances)) {
      throw new WingsException(
          INVALID_ARGUMENT, "args", "Pipeline execution [" + executionUuid + "] does not exist for approval");
    }

    long count = stateExecutionInstances.stream()
                     .map(StateExecutionInstance::getStateExecutionMap)
                     .flatMap(stringStateExecutionDataMap
                         -> stringStateExecutionDataMap.values().stream().filter(stateExecutionData
                             -> stateExecutionData instanceof ApprovalStateExecutionData
                                 && ((ApprovalStateExecutionData) stateExecutionData).getApprovalId().equals(approvalId)
                                 && stateExecutionData.getStatus().equals(PAUSED)))
                     .count();
    if (count == 0) {
      return false;
    }
    return true;
  }

  @Override
  public List<PipelineExecution> getPipelineExecutionHistory(String serviceId, int limit) {
    PageRequest pageRequest =
        Builder.aPageRequest()
            .addFilter("pipelineStageExecutions.workflowExecutions.serviceExecutionSummaries.contextElement.className",
                Operator.EQ, "software.wings.api.ServiceElement")
            .addFilter("pipelineStageExecutions.workflowExecutions.serviceExecutionSummaries.contextElement.uuid",
                Operator.EQ, serviceId)
            .addOrder("startTs", OrderType.DESC)
            .withLimit(String.valueOf(limit))
            .build();

    return wingsPersistence.query(PipelineExecution.class, pageRequest, true);
  }

  @Override
  public ExecutionInterrupt triggerExecutionInterrupt(ExecutionInterrupt executionInterrupt) {
    String appId = executionInterrupt.getAppId();
    String pipelineExecutionId = executionInterrupt.getExecutionUuid();
    PipelineExecution pipelineExecution = wingsPersistence.get(PipelineExecution.class, appId, pipelineExecutionId);
    if (pipelineExecution == null || pipelineExecution.getStatus().isFinalStatus()) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Pipeline Execution Already Completed");
    }

    if (executionInterrupt == null || executionInterrupt.getExecutionInterruptType() == null
        || !(executionInterrupt.getExecutionInterruptType() == PAUSE_ALL
               || executionInterrupt.getExecutionInterruptType() == RESUME_ALL
               || executionInterrupt.getExecutionInterruptType() == ABORT_ALL)) {
      throw new WingsException(
          ErrorCode.INVALID_REQUEST, "message", "Invalid ExecutionInterrupt: " + executionInterrupt);
    }

    executionInterrupt.setAppId(appId);
    executionInterrupt.setExecutionUuid(pipelineExecution.getWorkflowExecutionId());
    try {
      executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
    } catch (Exception e) {
      logger.warn("Error in interrupting workflowExecution - uuid: {}, executionInterruptType: {}",
          pipelineExecution.getWorkflowExecutionId(), executionInterrupt.getExecutionInterruptType());
      logger.warn(e.getMessage(), e);
    }

    List<StateExecutionInstance> stateExecutionInstances = getStateExecutionInstances(pipelineExecution);
    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
      StateExecutionData stateExecutionData = stateExecutionInstance.getStateExecutionData();
      if (stateExecutionData == null || !(stateExecutionData instanceof EnvStateExecutionData)) {
        continue;
      }
      EnvStateExecutionData envStateExecutionData = (EnvStateExecutionData) stateExecutionData;
      WorkflowExecution workflowExecution = workflowExecutionService.getExecutionDetails(
          pipelineExecution.getAppId(), envStateExecutionData.getWorkflowExecutionId());

      if (workflowExecution == null
          || (workflowExecution.getStatus() != null && workflowExecution.getStatus().isFinalStatus())) {
        continue;
      }

      try {
        ExecutionInterrupt executionInterruptClone = KryoUtils.clone(executionInterrupt);
        executionInterruptClone.setExecutionUuid(workflowExecution.getUuid());
        executionInterruptManager.registerExecutionInterrupt(executionInterruptClone);
      } catch (Exception e) {
        logger.warn("Error in interrupting workflowExecution - uuid: {}, executionInterruptType: {}",
            workflowExecution.getUuid(), executionInterrupt.getExecutionInterruptType());
        logger.warn(e.getMessage(), e);
      }
    }
    return executionInterrupt;
  }

  private List<Artifact> validateAndFetchArtifact(String appId, List<Artifact> artifacts) {
    notNullCheck("artifacts", artifacts);
    List<Artifact> validatedArtifacts = new ArrayList<>();
    artifacts.forEach(artifact -> {
      notNullCheck("artifact", artifact);
      validatedArtifacts.add(artifactService.get(appId, artifact.getUuid()));
    });
    return validatedArtifacts;
  }
}
