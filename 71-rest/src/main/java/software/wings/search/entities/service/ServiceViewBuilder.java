package software.wings.search.entities.service;

import com.google.inject.Inject;

import com.mongodb.DBObject;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Sort;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.entities.service.ServiceView.ServiceViewKeys;
import software.wings.search.framework.EntityInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ServiceViewBuilder {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private ServiceView serviceView;
  private static final int DAYS_TO_RETAIN = 7;
  private static final int MAX_RELATED_ENTITIES_COUNT = 3;

  public List<String> getServiceIds(Workflow workflow) {
    List<String> serviceIds = new ArrayList<>();
    if (workflow.getOrchestration() != null && workflow.getOrchestration().getServiceIds() != null) {
      serviceIds.addAll(workflow.getOrchestration().getServiceIds());
    }
    return serviceIds;
  }

  public List<String> getServiceIds(Pipeline pipeline) {
    Set<String> uniqueServiceIds = new HashSet<>();
    if (pipeline.getPipelineStages() != null) {
      for (PipelineStage pipelineStage : pipeline.getPipelineStages()) {
        if (pipelineStage.getPipelineStageElements() != null) {
          for (PipelineStageElement pipelineStageElement : pipelineStage.getPipelineStageElements()) {
            if (pipelineStageElement.getProperties().containsKey("workflowId")) {
              Workflow workflow = wingsPersistence.get(
                  Workflow.class, pipelineStageElement.getProperties().get("workflowId").toString());
              uniqueServiceIds.addAll(getServiceIds(workflow));
            }
          }
        }
      }
    }
    return new ArrayList<>(uniqueServiceIds);
  }

  private void createBaseView(Service service) {
    this.serviceView =
        new ServiceView(service.getUuid(), service.getName(), service.getDescription(), service.getAccountId(),
            service.getCreatedAt(), service.getLastUpdatedAt(), EntityType.SERVICE, service.getCreatedBy(),
            service.getLastUpdatedBy(), service.getAppId(), service.getArtifactType(), service.getDeploymentType());
  }

  private void setAuditsAndAuditTimestamps(Service service) {
    long startTimestamp = System.currentTimeMillis() - DAYS_TO_RETAIN * 86400 * 1000;
    List<RelatedAuditView> audits = new ArrayList<>();
    List<Long> auditTimestamps = new ArrayList<>();
    try (HIterator<AuditHeader> iterator = new HIterator<>(wingsPersistence.createQuery(AuditHeader.class)
                                                               .field(AuditHeaderKeys.accountId)
                                                               .equal(service.getAccountId())
                                                               .field("entityAuditRecords.entityId")
                                                               .equal(service.getUuid())
                                                               .field(ServiceKeys.createdAt)
                                                               .greaterThanOrEq(startTimestamp)
                                                               .order(Sort.descending(AuditHeaderKeys.createdAt))
                                                               .fetch())) {
      while (iterator.hasNext()) {
        final AuditHeader auditHeader = iterator.next();
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getAffectedResourceType().equals(EntityType.SERVICE.name())
              && entityAuditRecord.getAppId().equals(service.getUuid())) {
            if (audits.size() < MAX_RELATED_ENTITIES_COUNT) {
              audits.add(relatedAuditViewBuilder.getAuditRelatedEntityView(auditHeader, entityAuditRecord));
            }
            auditTimestamps.add(TimeUnit.MILLISECONDS.toSeconds(auditHeader.getCreatedAt()));
            break;
          }
        }
      }
    }

    Collections.reverse(audits);
    Collections.reverse(auditTimestamps);
    serviceView.setAuditTimestamps(auditTimestamps);
    serviceView.setAudits(audits);
  }

  private void setDeploymentsAndDeploymentTimestamps(Service service) {
    long startTimestamp = System.currentTimeMillis() - DAYS_TO_RETAIN * 86400 * 1000;
    List<Long> deploymentTimestamps = new ArrayList<>();
    List<RelatedDeploymentView> deployments = new ArrayList<>();
    try (HIterator<WorkflowExecution> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class)
                                 .field(WorkflowExecutionKeys.appId)
                                 .equal(service.getAppId())
                                 .field(WorkflowExecutionKeys.serviceIds)
                                 .equal(service.getUuid())
                                 .field(ServiceKeys.createdAt)
                                 .greaterThanOrEq(startTimestamp)
                                 .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                 .fetch())) {
      while (iterator.hasNext()) {
        final WorkflowExecution workflowExecution = iterator.next();
        if (deployments.size() < MAX_RELATED_ENTITIES_COUNT) {
          deployments.add(new RelatedDeploymentView(workflowExecution));
        }
        deploymentTimestamps.add(TimeUnit.MILLISECONDS.toSeconds(workflowExecution.getCreatedAt()));
      }
    }
    Collections.reverse(deployments);
    Collections.reverse(deploymentTimestamps);
    serviceView.setDeployments(deployments);
    serviceView.setDeploymentTimestamps(deploymentTimestamps);
  }

  private Set<Pipeline> populatePipelines(Workflow workflow) {
    Set<Pipeline> pipelines = new HashSet<>();
    try (HIterator<Pipeline> iterator =
             new HIterator<>(wingsPersistence.createQuery(Pipeline.class)
                                 .field(PipelineKeys.appId)
                                 .equal(workflow.getAppId())
                                 .field("pipelineStages.pipelineStageElements.properties.workflowId")
                                 .equal(workflow.getUuid())
                                 .fetch())) {
      while (iterator.hasNext()) {
        final Pipeline pipeline = iterator.next();
        pipelines.add(pipeline);
      }
    }

    return pipelines;
  }

  private void setWorkflowsAndPipelines(Service service) {
    Set<EntityInfo> workflows = new HashSet<>();
    Set<EntityInfo> pipelines = new HashSet<>();
    try (
        HIterator<Workflow> iterator = new HIterator<>(
            wingsPersistence.createQuery(Workflow.class).field(WorkflowKeys.appId).equal(service.getAppId()).fetch())) {
      while (iterator.hasNext()) {
        final Workflow workflow = iterator.next();
        if (workflow.getOrchestration() != null && workflow.getOrchestration().getServiceIds() != null
            && workflow.getOrchestration().getServiceIds().stream().parallel().anyMatch(service.getUuid()::equals)) {
          workflows.add(new EntityInfo(workflow.getUuid(), workflow.getName()));
          for (Pipeline pipeline : populatePipelines(workflow)) {
            pipelines.add(new EntityInfo(pipeline.getUuid(), pipeline.getName()));
          }
        }
      }
    }
    serviceView.setWorkflows(workflows);
    serviceView.setPipelines(pipelines);
  }

  private void setApplicationName(Service service) {
    if (service.getAppId() != null) {
      Application application = wingsPersistence.get(Application.class, service.getAppId());
      serviceView.setAppName(application.getName());
    }
  }

  ServiceView createServiceView(Service service) {
    if (wingsPersistence.get(Application.class, service.getAppId()) != null) {
      createBaseView(service);
      setAuditsAndAuditTimestamps(service);
      setDeploymentsAndDeploymentTimestamps(service);
      setApplicationName(service);
      setWorkflowsAndPipelines(service);
      return serviceView;
    }
    return null;
  }

  ServiceView createServiceView(Service service, DBObject changeDocument) {
    if (wingsPersistence.get(Application.class, service.getAppId()) != null) {
      createBaseView(service);
      if (changeDocument.containsField(ServiceViewKeys.appId)) {
        setApplicationName(service);
      }
      return serviceView;
    }
    return null;
  }
}