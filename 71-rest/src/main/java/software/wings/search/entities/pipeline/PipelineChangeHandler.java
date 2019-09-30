package software.wings.search.entities.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import io.harness.beans.WorkflowType;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.workflow.WorkflowView.WorkflowViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.Map;
import java.util.Optional;

/**
 * The handler which will maintain the pipeline
 * document in the search engine database.
 *
 * @author utkarsh
 */

@Slf4j
@Singleton
public class PipelineChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private PipelineViewBuilder pipelineViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleAuditRelatedChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType().equals(ChangeType.UPDATE)) {
      boolean result = true;
      if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
        AuditHeader auditHeader = (AuditHeader) changeEvent.getFullDocument();
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getAffectedResourceType().equals(EntityType.PIPELINE.name())) {
            String fieldToUpdate = PipelineViewKeys.audits;
            String filterId = entityAuditRecord.getAffectedResourceId();
            String auditTimestampField = PipelineViewKeys.auditTimestamps;
            Map<String, Object> auditRelatedEntityViewMap =
                relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
            result &= searchDao.addTimestamp(PipelineSearchEntity.TYPE, auditTimestampField, filterId, DAYS_TO_RETAIN);
            result &= searchDao.appendToListInSingleDocument(
                PipelineSearchEntity.TYPE, fieldToUpdate, filterId, auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
          }
        }
      }
      return result;
    }
    return true;
  }

  private boolean handleWorkflowExecutionInsert(ChangeEvent<?> changeEvent) {
    boolean result = true;
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    if (workflowExecution.getWorkflowType().equals(WorkflowType.ORCHESTRATION)) {
      String filterId = workflowExecution.getWorkflowId();
      String fieldToUpdate = WorkflowViewKeys.deployments;
      Map<String, Object> deploymentRelatedEntityViewMap =
          relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
      String deploymentTimestampsField = PipelineViewKeys.deploymentTimestamps;
      result = searchDao.addTimestamp(PipelineSearchEntity.TYPE, deploymentTimestampsField, filterId, DAYS_TO_RETAIN);

      result = result
          && searchDao.appendToListInSingleDocument(PipelineSearchEntity.TYPE, fieldToUpdate, filterId,
                 deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
    }
    return result;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    if (workflowExecution.getWorkflowType().equals(WorkflowType.ORCHESTRATION)) {
      DBObject changes = changeEvent.getChanges();
      if (changes.containsField(WorkflowExecutionKeys.status)) {
        String entityType = WorkflowViewKeys.deployments;
        String newNameValue = workflowExecution.getStatus().toString();
        String filterId = changeEvent.getUuid();
        String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
        return searchDao.updateListInMultipleDocuments(
            PipelineSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowExecution(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowExecutionInsert(changeEvent);
      case UPDATE:
        return handleWorkflowExecutionUpdate(changeEvent);
      default:
        return true;
    }
  }

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ApplicationKeys.name)) {
        String keyToUpdate = PipelineViewKeys.appName;
        String newValue = document.get(ApplicationKeys.name).toString();
        String filterKey = PipelineViewKeys.appId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            PipelineSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleServiceChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = PipelineViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String filterId = changeEvent.getUuid();
        String fieldToUpdate = ServiceKeys.name;
        return searchDao.updateListInMultipleDocuments(
            PipelineSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(WorkflowKeys.name)) {
        String entityType = PipelineViewKeys.workflows;
        String newNameValue = document.get(WorkflowKeys.name).toString();
        String filterId = changeEvent.getUuid();
        String fieldToUpdate = WorkflowKeys.name;
        return searchDao.updateListInMultipleDocuments(
            PipelineSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handlePipelineInsert(ChangeEvent<?> changeEvent) {
    Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
    PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline);
    if (pipelineView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
      }
    }
    return true;
  }

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
    PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline, changeDocument);
    if (pipelineView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
      }
    }
    return true;
  }

  private boolean handlePipelineChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handlePipelineInsert(changeEvent);
      case UPDATE:
        return handlePipelineUpdate(changeEvent);
      case DELETE:
        return searchDao.deleteDocument(PipelineSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(Pipeline.class)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Application.class)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Workflow.class)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Service.class)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.isChangeFor(WorkflowExecution.class)) {
      isChangeHandled = handleWorkflowExecution(changeEvent);
    }
    if (changeEvent.isChangeFor(AuditHeader.class)) {
      isChangeHandled = handleAuditRelatedChange(changeEvent);
    }
    return isChangeHandled;
  }
}
