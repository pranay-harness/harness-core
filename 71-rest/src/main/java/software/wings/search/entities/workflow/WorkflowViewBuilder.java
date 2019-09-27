package software.wings.search.entities.workflow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Sort;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityInfo;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class WorkflowViewBuilder {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final String TEMPLATIZED = "Templatized - ";
  private WorkflowView workflowView;
  private static final int DAYS_TO_RETAIN = 7;
  private static final int MAX_RELATED_ENTITIES_COUNT = 3;

  private void populateServicesInWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestration();
    if (orchestrationWorkflow != null) {
      workflow.setServices(
          serviceResourceService.fetchServicesByUuids(workflow.getAppId(), orchestrationWorkflow.getServiceIds()));
    }
  }

  public List<String> populateWorkflowIds(Pipeline pipeline) {
    List<String> workflowIds = new ArrayList<>();
    if (pipeline.getPipelineStages() != null) {
      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        if (pipelineStage.getPipelineStageElements() != null) {
          for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
            if (pipelineStageElement.getProperties().containsKey("workflowId")) {
              workflowIds.add(pipelineStageElement.getProperties().get("workflowId").toString());
            }
          }
        }
      }
    }
    return workflowIds;
  }

  private void setDeploymentsAndDeploymentTimestamps(Workflow workflow) {
    long startTimestamp = System.currentTimeMillis() - DAYS_TO_RETAIN * 86400 * 1000;
    List<Long> deploymentTimestamps = new ArrayList<>();
    List<RelatedDeploymentView> deployments = new ArrayList<>();
    HIterator<WorkflowExecution> iterator =
        new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class)
                            .filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())
                            .field(WorkflowExecutionKeys.createdAt)
                            .greaterThanOrEq(startTimestamp)
                            .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                            .fetch());

    while (iterator.hasNext()) {
      final WorkflowExecution workflowExecution = iterator.next();
      if (deployments.size() < MAX_RELATED_ENTITIES_COUNT) {
        deployments.add(new RelatedDeploymentView(workflowExecution));
      }
      deploymentTimestamps.add(TimeUnit.MILLISECONDS.toSeconds(workflowExecution.getCreatedAt()));
    }
    Collections.reverse(deployments);
    Collections.reverse(deploymentTimestamps);
    workflowView.setDeployments(deployments);
    workflowView.setDeploymentTimestamps(deploymentTimestamps);
  }

  public void createBaseView(Workflow workflow) {
    this.workflowView = new WorkflowView(workflow.getUuid(), workflow.getName(), workflow.getDescription(),
        workflow.getAccountId(), workflow.getCreatedAt(), workflow.getLastUpdatedAt(), EntityType.WORKFLOW,
        workflow.getCreatedBy(), workflow.getLastUpdatedBy(), workflow.getAppId(),
        workflow.getOrchestration().getOrchestrationWorkflowType().name());
  }

  public void setApplicationName(Workflow workflow) {
    if (workflow.getAppId() != null) {
      Application application = wingsPersistence.get(Application.class, workflow.getAppId());
      workflowView.setAppName(application.getName());
    }
  }

  private void setEnvironment(Workflow workflow) {
    boolean isEnvironmentTemplatized = false;
    if (workflow.getTemplateExpressions() != null) {
      for (TemplateExpression templateExpression : workflow.getTemplateExpressions()) {
        if (templateExpression.getFieldName().equals(WorkflowKeys.envId)) {
          workflowView.setEnvironmentName(TEMPLATIZED + templateExpression.getExpression());
          isEnvironmentTemplatized = true;
        }
      }
    }
    if (!isEnvironmentTemplatized && workflow.getEnvId() != null) {
      Environment environment = wingsPersistence.get(Environment.class, workflow.getEnvId());
      workflowView.setEnvironmentId(environment.getUuid());
      workflowView.setEnvironmentName(environment.getName());
    }
  }

  private void setServices(Workflow workflow) {
    Set<EntityInfo> serviceInfos = new HashSet<>();
    populateServicesInWorkflow(workflow);
    boolean isServiceTemplatized = false;
    if (workflow.getTemplateExpressions() != null) {
      for (TemplateExpression templateExpression : workflow.getTemplateExpressions()) {
        if (templateExpression.getFieldName().equals(WorkflowKeys.serviceId)) {
          EntityInfo entityInfo = new EntityInfo(null, TEMPLATIZED + templateExpression.getExpression());
          serviceInfos.add(entityInfo);
          isServiceTemplatized = true;
        }
      }
    }
    if (workflow.getServices() != null && !isServiceTemplatized) {
      for (Service service : workflow.getServices()) {
        EntityInfo serviceInfo = new EntityInfo(service.getUuid(), service.getName());
        serviceInfos.add(serviceInfo);
      }
    }
    workflowView.setServices(serviceInfos);
  }

  public void setAuditsAndAuditTimestamps(Workflow workflow) {
    long startTimestamp = System.currentTimeMillis() - DAYS_TO_RETAIN * 86400 * 1000;
    List<RelatedAuditView> audits = new ArrayList<>();
    List<Long> auditTimestamps = new ArrayList<>();
    HIterator<AuditHeader> iterator = new HIterator<>(wingsPersistence.createQuery(AuditHeader.class)
                                                          .field("entityAuditRecords.entityId")
                                                          .equal(workflow.getUuid())
                                                          .field(WorkflowKeys.createdAt)
                                                          .greaterThanOrEq(startTimestamp)
                                                          .order(Sort.descending(AuditHeaderKeys.createdAt))
                                                          .fetch());

    while (iterator.hasNext()) {
      final AuditHeader auditHeader = iterator.next();
      if (auditHeader.getEntityAuditRecords() != null) {
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getAffectedResourceType().equals(EntityType.WORKFLOW.name())
              && entityAuditRecord.getAffectedResourceId().equals(workflow.getUuid())) {
            if (audits.size() < MAX_RELATED_ENTITIES_COUNT) {
              audits.add(relatedAuditViewBuilder.getAuditRelatedEntityView(auditHeader, entityAuditRecord));
            }
            auditTimestamps.add(TimeUnit.MILLISECONDS.toSeconds(auditHeader.getCreatedAt()));
          }
        }
      }
    }
    Collections.reverse(audits);
    Collections.reverse(auditTimestamps);
    workflowView.setAudits(audits);
    workflowView.setAuditTimestamps(auditTimestamps);
  }

  public void setPipelines(Workflow workflow) {
    Set<EntityInfo> pipelines = new HashSet<>();
    HIterator<Pipeline> iterator =
        new HIterator<>(wingsPersistence.createQuery(Pipeline.class)
                            .field("pipelineStages.pipelineStageElements.properties.workflowId")
                            .equal(workflow.getUuid())
                            .fetch());

    while (iterator.hasNext()) {
      final Pipeline pipeline = iterator.next();
      if (pipeline.getPipelineStages() != null) {
        for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
          if (pipelineStage.getPipelineStageElements() != null) {
            for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
              if (pipelineStageElement.getProperties() != null) {
                if (pipelineStageElement.getProperties().get("workflowId") != null) {
                  if (pipelineStageElement.getProperties().get("workflowId").toString().equals(workflow.getUuid())) {
                    pipelines.add(new EntityInfo(pipeline.getUuid(), pipeline.getName()));
                  }
                }
              }
            }
          }
        }
      }
    }
    workflowView.setPipelines(pipelines);
  }

  public WorkflowView createWorkflowView(Workflow workflow) {
    if (wingsPersistence.get(Application.class, workflow.getAppId()) != null) {
      createBaseView(workflow);
      setApplicationName(workflow);
      setEnvironment(workflow);
      setServices(workflow);
      setPipelines(workflow);
      setDeploymentsAndDeploymentTimestamps(workflow);
      setAuditsAndAuditTimestamps(workflow);
      return workflowView;
    }
    return null;
  }

  public WorkflowView createWorkflowView(Workflow workflow, DBObject changeDocument) {
    if (wingsPersistence.get(Application.class, workflow.getAppId()) != null) {
      createBaseView(workflow);
      if (changeDocument.containsField(WorkflowKeys.appId)) {
        setApplicationName(workflow);
      }
      if (changeDocument.containsField(WorkflowKeys.envId)
          || changeDocument.containsField(WorkflowKeys.templateExpressions)) {
        setEnvironment(workflow);
      }
      if (changeDocument.containsField(WorkflowKeys.orchestration)
          || changeDocument.containsField(WorkflowKeys.templateExpressions)) {
        setServices(workflow);
      }
      return workflowView;
    }
    return null;
  }
}
