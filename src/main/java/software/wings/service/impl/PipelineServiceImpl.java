package software.wings.service.impl;

import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.PipelineStageExecution.Builder.aPipelineStageExecution;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;

import java.util.Arrays;
import javax.inject.Inject;

/**
 * Created by anubhaw on 10/26/16.
 */
public class PipelineServiceImpl implements PipelineService {
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<PipelineExecution> listPipelineExecutions(PageRequest<PipelineExecution> pageRequest) {
    return wingsPersistence.query(PipelineExecution.class, pageRequest);
  }

  @Override
  public void updatePipelineExecutionData(
      String appId, String pipelineExecutionId, WorkflowExecution workflowExecution) {
    Query<PipelineExecution> query = wingsPersistence.createQuery(PipelineExecution.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field("workflowExecutionId")
                                         .equal(pipelineExecutionId);

    PipelineStageExecution stageExecution = aPipelineStageExecution()
                                                .withWorkflowExecutions(Arrays.asList(workflowExecution))
                                                .withStartTs(workflowExecution.getStartTs())
                                                .withEndTs(workflowExecution.getEndTs())
                                                .withStatus(workflowExecution.getStatus())
                                                .build();

    UpdateOperations<PipelineExecution> operations =
        wingsPersistence.createUpdateOperations(PipelineExecution.class).add("pipelineStageExecutions", stageExecution);
    wingsPersistence.update(query, operations);
  }

  @Override
  public void updatePipelineExecutionData(String appId, String pipelineExecutionId, Artifact artifact) {
    Query<PipelineExecution> query = wingsPersistence.createQuery(PipelineExecution.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field("workflowExecutionId")
                                         .equal(pipelineExecutionId);
    UpdateOperations<PipelineExecution> operations = wingsPersistence.createUpdateOperations(PipelineExecution.class)
                                                         .set("artifactId", artifact.getUuid())
                                                         .set("artifactName", artifact.getDisplayName());
    wingsPersistence.update(query, operations);
  }

  @Override
  public void updatePipelineExecutionData(String appId, String workflowExecutionId, ExecutionStatus status) {
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
    wingsPersistence.save(pipelineExecution);
    return workflowExecution;
  }
}
