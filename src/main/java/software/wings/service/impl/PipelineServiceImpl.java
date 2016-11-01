package software.wings.service.impl;

import static java.util.Arrays.asList;
import static software.wings.beans.PipelineExecution.Builder.aPipelineExecution;
import static software.wings.beans.PipelineStageExecution.Builder.aPipelineStageExecution;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
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
  public void updatePipelineStageExecutionData(
      String appId, String pipelineExecutionId, WorkflowExecution workflowExecution, boolean startingStageExecution) {
    Query<PipelineExecution> query = wingsPersistence.createQuery(PipelineExecution.class)
                                         .field("appId")
                                         .equal(appId)
                                         .field("workflowExecutionId")
                                         .equal(pipelineExecutionId);

    UpdateOperations<PipelineExecution> operations;

    if (startingStageExecution) {
      PipelineStageExecution stageExecution = aPipelineStageExecution()
                                                  .withWorkflowExecutions(asList(workflowExecution))
                                                  .withStartTs(workflowExecution.getStartTs())
                                                  .withEndTs(workflowExecution.getEndTs())
                                                  .withStatus(workflowExecution.getStatus())
                                                  .build();
      query.field("pipelineStageExecutions.workflowExecutions._id").notEqual(workflowExecution.getUuid());
      operations = wingsPersistence.createUpdateOperations(PipelineExecution.class)
                       .add("pipelineStageExecutions", stageExecution);
    } else {
      query.field("pipelineStageExecutions.workflowExecutions._id").equal(workflowExecution.getUuid());
      operations = wingsPersistence.createUpdateOperations(PipelineExecution.class)
                       .set("pipelineStageExecutions.$.workflowExecutions", asList(workflowExecution))
                       .set("pipelineStageExecutions.$.endTs", workflowExecution.getEndTs())
                       .set("pipelineStageExecutions.$.status", workflowExecution.getStatus());
    }

    UpdateResults update = wingsPersistence.update(query, operations);
    System.out.println(update.toString());
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
