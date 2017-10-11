package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.PIPELINE_EXECUTION_IN_PROGRESS;
import static software.wings.beans.OrchestrationWorkflowType.BASIC;
import static software.wings.beans.OrchestrationWorkflowType.CANARY;
import static software.wings.beans.OrchestrationWorkflowType.MULTI_SERVICE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.WorkflowDetails.Builder.aWorkflowDetails;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.inject.Singleton;

import de.danielbechler.util.Collections;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.MultiServiceOrchestrationWorkflow;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowDetails;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateTypeScope;
import software.wings.stencils.Stencil;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 10/26/16.
 */
@Singleton
@ValidateOnExecution
public class PipelineServiceImpl implements PipelineService {
  @Inject private WorkflowService workflowService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private ArtifactService artifactService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private YamlDirectoryService yamlDirectoryService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

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
    // we need this method if we are supporting individual file or sub-directory git sync
    /*
    EntityUpdateListEvent eule = new EntityUpdateListEvent();

    // see if we need to perform any Git Sync operations for the pipeline
    eule.addEntityUpdateEvent(entityUpdateService.pipelineListUpdate(pipeline, SourceType.ENTITY_UPDATE));

    entityUpdateService.queueEntityUpdateList(eule);
    */

    Application app = appService.get(pipeline.getAppId());
    yamlDirectoryService.pushDirectory(app.getAccountId(), false);
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
    boolean deleted;

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
}
