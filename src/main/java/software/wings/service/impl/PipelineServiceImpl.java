package software.wings.service.impl;

import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 10/26/16.
 */
public class PipelineServiceImpl implements PipelineService {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ArtifactService artifactService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Override
  public PageResponse<PipelineExecution> listPipelineExecutions(
      PageRequest<PipelineExecution> pageRequest, String appId) {
    PageRequest<WorkflowExecution> wflPageRequest =
        aPageRequest()
            .addFilter("appId", Operator.EQ, appId)
            .addFilter("workflowType", Operator.EQ, WorkflowType.ORCHESTRATION)
            .withLimit(PageRequest.UNLIMITED)
            .build();
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(wflPageRequest, false, true, true).getResponse();

    WorkflowExecution prodExecution = workflowExecutions.stream()
                                          .filter(wex -> wex.getEnvType().equals(EnvironmentType.PROD))
                                          .findFirst()
                                          .orElse(null);
    WorkflowExecution nonProdExecution = workflowExecutions.stream()
                                             .filter(wex -> !wex.getEnvType().equals(EnvironmentType.PROD))
                                             .findFirst()
                                             .orElse(null);
    WorkflowExecution execution = prodExecution != null ? prodExecution : nonProdExecution; // non null

    Artifact artifacts =
        (Artifact) artifactService.list(aPageRequest().addFilter("appId", Operator.EQ, appId).build(), false)
            .getResponse()
            .get(0);

    Pipeline pipeline = wingsPersistence.createQuery(Pipeline.class).field("appId").equal(appId).get();
    if (pipeline == null) {
      pipeline = new Pipeline();
    }

    PipelineStageExecution pipelineStageExecution = new PipelineStageExecution();
    pipelineStageExecution.setWorkflowExecutions(Arrays.asList(nonProdExecution, prodExecution));

    PipelineExecution pipelineExecution = aPipelineExecution()
                                              .withAppId(appId)
                                              .withAppName(execution.getAppName())
                                              .withArtifact(artifacts)
                                              .withName("UI Pipeline")
                                              .withStartTs(execution.getStartTs())
                                              .withEndTs(execution.getEndTs())
                                              .withCreatedAt(execution.getCreatedAt())
                                              .withLastUpdatedAt(execution.getLastUpdatedAt())
                                              .withPipeline(pipeline)
                                              .withPipelineId(pipeline.getUuid())
                                              .withStatus(ExecutionStatus.SUCCESS)
                                              .withUuid("a1b2c3")
                                              .withWorkflowType(WorkflowType.PIPELINE)
                                              .withPipelineStageExecutions(Arrays.asList(pipelineStageExecution))
                                              .build();

    return PageResponse.Builder.aPageResponse().withResponse(Arrays.asList(pipelineExecution)).build();
  }

  @Override
  public void updatePipelineExecutionData(String appId, String workflowExecutionId, ExecutionStatus status) {
    WorkflowExecution executionDetails = workflowExecutionService.getExecutionDetails(appId, workflowExecutionId);
    StateMachine stateMachine = workflowService.readLatest(appId, executionDetails.getWorkflowId(), null);
  }
}
