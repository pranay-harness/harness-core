package software.wings.search.entities.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.EntityInfo;
import software.wings.service.intfc.ServiceResourceService;

import java.util.HashSet;
import java.util.Set;

@Singleton
@FieldNameConstants(innerTypeName = "PipelineViewBuilderKeys")
public class PipelineViewBuilder {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WingsPersistence wingsPersistence;
  private PipelineView pipelineView;

  private void populateServices(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestration();
    workflow.setServices(
        serviceResourceService.fetchServicesByUuids(workflow.getAppId(), orchestrationWorkflow.getServiceIds()));
    workflow.setTemplatizedServiceIds(orchestrationWorkflow.getTemplatizedServiceIds());
  }

  private void createBaseView(Pipeline pipeline) {
    this.pipelineView = new PipelineView(pipeline.getUuid(), pipeline.getName(), pipeline.getDescription(),
        pipeline.getAccountId(), pipeline.getCreatedAt(), pipeline.getLastUpdatedAt(), EntityType.PIPELINE,
        pipeline.getCreatedBy(), pipeline.getLastUpdatedBy(), pipeline.getAppId());
  }

  private void setApplicationName(Pipeline pipeline) {
    if (pipeline.getAppId() != null) {
      Application application = wingsPersistence.get(Application.class, pipeline.getAppId());
      pipelineView.setAppName(application.getName());
    }
  }

  private void setServicesAndWorkflows(Pipeline pipeline) {
    if (pipeline.getPipelineStages() != null) {
      Set<EntityInfo> workflows = new HashSet<>();
      Set<EntityInfo> services = new HashSet<>();

      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
          if (pipelineStageElement != null && pipelineStageElement.getProperties() != null
              && pipelineStageElement.getProperties().get("workflowId") != null) {
            Workflow workflow =
                wingsPersistence.get(Workflow.class, pipelineStageElement.getProperties().get("workflowId").toString());
            EntityInfo workflowInfo =
                new EntityInfo(pipelineStageElement.getProperties().get("workflowId").toString(), workflow.getName());
            workflows.add(workflowInfo);
            if (workflow.getOrchestration() != null) {
              populateServices(workflow);
              for (Service service : workflow.getServices()) {
                EntityInfo serviceInfo = new EntityInfo(service.getUuid(), service.getName());
                services.add(serviceInfo);
              }
            }
          }
        }
      }
      pipelineView.setServices(services);
      pipelineView.setWorkflows(workflows);
    }
  }

  public PipelineView createPipelineView(Pipeline pipeline) {
    createBaseView(pipeline);
    setApplicationName(pipeline);
    setServicesAndWorkflows(pipeline);
    return pipelineView;
  }

  public PipelineView createPipelineView(Pipeline pipeline, DBObject changeDocument) {
    createBaseView(pipeline);
    if (changeDocument.containsField(PipelineKeys.appId)) {
      setApplicationName(pipeline);
    }
    if (changeDocument.containsField(PipelineKeys.pipelineStages)) {
      setServicesAndWorkflows(pipeline);
    }
    return pipelineView;
  }
}
