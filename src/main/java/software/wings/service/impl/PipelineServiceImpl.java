package software.wings.service.impl;

import static java.util.Arrays.asList;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.PipelineStageExecution.Builder.aPipelineStageExecution;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.api.BuildStateExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 10/26/16.
 */
public class PipelineServiceImpl implements PipelineService {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<PipelineExecution> listPipelineExecutions(PageRequest<PipelineExecution> pageRequest) {
    PageResponse<PipelineExecution> pageResponse = wingsPersistence.query(PipelineExecution.class, pageRequest);
    pageResponse.getResponse().forEach(this ::refreshPipelineExecution);
    return pageResponse;
  }

  private void refreshPipelineExecution(PipelineExecution pipelineExecution) {
    if (Arrays.asList(SUCCESS, FAILED, ERROR, ABORTED).contains(pipelineExecution.getStatus())) {
      return;
    }
    StateMachine stateMachine =
        workflowService.readLatest(pipelineExecution.getAppId(), pipelineExecution.getPipelineId(), null);
    ImmutableMap<String, StateExecutionInstance> stateExecutionInstanceMap =
        getStateExecutionInstanceMap(pipelineExecution);
    List<PipelineStageExecution> stateExecutionDataList = new ArrayList<>();

    State currState = stateMachine.getInitialState();

    while (currState != null) {
      StateExecutionInstance stateExecutionInstance = stateExecutionInstanceMap.get(currState.getName());

      if (currState.getStateType().equals("BUILD") && stateExecutionInstance != null) {
        BuildStateExecutionData buildStateExecutionData =
            (BuildStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(currState.getName());
        pipelineExecution.setArtifactId(buildStateExecutionData.getArtifactId());
        pipelineExecution.setArtifactName(buildStateExecutionData.getArtifactName());
      } else if (stateExecutionInstance == null) {
        stateExecutionDataList.add(aPipelineStageExecution()
                                       .withStateType(currState.getStateType())
                                       .withStateName(currState.getName())
                                       .withStatus(ExecutionStatus.QUEUED)
                                       .build());
      } else if (stateExecutionInstance != null && APPROVAL.name().equals(stateExecutionInstance.getStateType())) {
        stateExecutionDataList.add(aPipelineStageExecution()
                                       .withStateType(currState.getStateType())
                                       .withStatus(stateExecutionInstance.getStatus())
                                       .build());
      } else if (stateExecutionInstance != null && ENV_STATE.name().equals(stateExecutionInstance.getStateType())) {
        PipelineStageExecution stageExecution = aPipelineStageExecution()
                                                    .withStateType(currState.getStateType())
                                                    .withStateName(currState.getName())
                                                    .withStatus(ExecutionStatus.QUEUED)
                                                    .build();

        EnvStateExecutionData envStateExecutionData =
            (EnvStateExecutionData) stateExecutionInstance.getStateExecutionMap().get(currState.getName());

        if (envStateExecutionData != null) {
          WorkflowExecution workflowExecution = workflowExecutionService.getExecutionDetails(
              pipelineExecution.getAppId(), envStateExecutionData.getWorkflowExecutionId());

          stageExecution = aPipelineStageExecution()
                               .withStateType(currState.getStateType())
                               .withStateName(currState.getName())
                               .withStatus(stateExecutionInstance.getStatus())
                               .withWorkflowExecutions(asList(workflowExecution))
                               .withStartTs(workflowExecution.getStartTs())
                               .withEndTs(workflowExecution.getEndTs())
                               .withStatus(workflowExecution.getStatus())
                               .build();
        }

        stateExecutionDataList.add(stageExecution);
      } else {
        throw new WingsException(
            ErrorCodes.UNKNOWN_ERROR, "message", "Unknow stateType " + stateExecutionInstance.getStateType());
      }
      List<State> nextStates = stateMachine.getNextStates(currState.getName());
      currState = nextStates != null ? nextStates.get(0) : null;
    }
    UpdateResults updateResults = wingsPersistence.update(pipelineExecution,
        wingsPersistence.createUpdateOperations(PipelineExecution.class)
            .set("artifactId", pipelineExecution.getArtifactId())
            .set("artifactName", pipelineExecution.getArtifactName())
            .set("pipelineStageExecutions", stateExecutionDataList)); // TODO:: version based update
    if (updateResults.getUpdatedCount() > 0) {
      WorkflowExecution executionDetails = workflowExecutionService.getExecutionDetails(
          pipelineExecution.getAppId(), pipelineExecution.getWorkflowExecutionId());
      updatePipelineExecutionStatus(
          executionDetails.getAppId(), executionDetails.getUuid(), executionDetails.getStatus());
    }
  }

  private void updatePipelineExecutionStatus(String appId, String workflowExecutionId, ExecutionStatus status) {
    Query<PipelineExecution> query = wingsPersistence.createQuery(PipelineExecution.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field("workflowExecutionId")
                                         .equal(workflowExecutionId);
    UpdateOperations<PipelineExecution> operations = wingsPersistence.createUpdateOperations(PipelineExecution.class)
                                                         .set("status", status)
                                                         .set("endTs", System.currentTimeMillis());
    wingsPersistence.update(query, operations);
  }

  private ImmutableMap<String, StateExecutionInstance> getStateExecutionInstanceMap(
      PipelineExecution pipelineExecution) {
    PageRequest<StateExecutionInstance> req =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", Operator.EQ, pipelineExecution.getAppId())
            .addFilter("executionUuid", Operator.EQ, pipelineExecution.getWorkflowExecutionId())
            .build();
    List<StateExecutionInstance> stateExecutionInstances =
        wingsPersistence.query(StateExecutionInstance.class, req).getResponse();
    return Maps.uniqueIndex(stateExecutionInstances, v -> v.getStateName());
  }

  @Override
  public void updatePipelineExecutionData(String appId, String workflowExecutionId, ExecutionStatus status) {
    refreshPipelineExecution(appId, workflowExecutionId);
  }

  private void refreshPipelineExecution(String appId, String workflowExecutionId) {
    PipelineExecution pipelineExecution = wingsPersistence.createQuery(PipelineExecution.class)
                                              .field("appId")
                                              .equal(appId)
                                              .field("workflowExecutionId")
                                              .equal(workflowExecutionId)
                                              .get();
    refreshPipelineExecution(pipelineExecution);
  }

  @Override
  public WorkflowExecution execute(String appId, String pipelineId) {
    WorkflowExecution workflowExecution = workflowExecutionService.triggerPipelineExecution(appId, pipelineId);
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, appId, pipelineId);
    Application application = appService.get(appId);
    PipelineExecution pipelineExecution = aPipelineExecution()
                                              .withAppId(appId)
                                              .withAppName(application.getName())
                                              .withPipelineId(pipelineId)
                                              .withPipeline(pipeline)
                                              .withWorkflowExecutionId(workflowExecution.getUuid())
                                              .withWorkflowType(WorkflowType.PIPELINE)
                                              .withStatus(workflowExecution.getStatus())
                                              .withStartTs(System.currentTimeMillis())
                                              .build();
    pipelineExecution = wingsPersistence.saveAndGet(PipelineExecution.class, pipelineExecution);
    refreshPipelineExecution(pipelineExecution);
    return workflowExecution;
  }
}
