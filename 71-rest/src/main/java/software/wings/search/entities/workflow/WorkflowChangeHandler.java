package software.wings.search.entities.workflow;

import com.google.common.collect.Sets;
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
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.workflow.WorkflowView.WorkflowViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The handler which will maintain the workflow document
 * in the search engine database.
 *
 * @author ujjawal
 */

@Slf4j
@Singleton
public class WorkflowChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WorkflowViewBuilder workflowViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(PipelineKeys.pipelineStages)) {
      Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
      String fieldToUpdate = WorkflowViewKeys.pipelines;

      Set<String> incomingWorkflowIds = Sets.newHashSet(workflowViewBuilder.populateWorkflowIds(pipeline));
      Set<String> currentWorkflowIds = Sets.newHashSet(
          searchDao.nestedQuery(WorkflowSearchEntity.TYPE, WorkflowViewKeys.pipelines, pipeline.getUuid()));

      List<String> toBeAddedWorkflowIds = new ArrayList<>(
          Sets.difference(incomingWorkflowIds, Sets.intersection(incomingWorkflowIds, currentWorkflowIds)));
      List<String> toBeDeletedWorkflowIds = new ArrayList<>(
          Sets.difference(currentWorkflowIds, Sets.intersection(incomingWorkflowIds, currentWorkflowIds)));

      EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (!toBeAddedWorkflowIds.isEmpty()) {
        result = searchDao.appendToListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, fieldToUpdate, toBeAddedWorkflowIds, newElement);
      }
      if (!toBeDeletedWorkflowIds.isEmpty()) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                   WorkflowSearchEntity.TYPE, fieldToUpdate, toBeDeletedWorkflowIds, pipeline.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = WorkflowViewKeys.pipelines;
      String newValue = document.get(WorkflowKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = WorkflowKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
                 WorkflowSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handlePipelineDelete(ChangeEvent<?> changeEvent) {
    String fieldToUpdate = WorkflowViewKeys.pipelines;
    List<String> toBeDeletedWorkflowIds =
        searchDao.nestedQuery(WorkflowSearchEntity.TYPE, WorkflowViewKeys.pipelines, changeEvent.getUuid());
    return searchDao.removeFromListInMultipleDocuments(
        WorkflowSearchEntity.TYPE, fieldToUpdate, toBeDeletedWorkflowIds, changeEvent.getUuid());
  }

  private boolean handlePipelineChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case UPDATE:
        return handlePipelineUpdate(changeEvent);
      case DELETE:
        return handlePipelineDelete(changeEvent);
      default:
        return true;
    }
  }

  private boolean handleAuditRelatedChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType().equals(ChangeType.UPDATE)) {
      boolean result = true;
      AuditHeader auditHeader = (AuditHeader) changeEvent.getFullDocument();
      if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getAffectedResourceType().equals(EntityType.WORKFLOW.name())) {
            String fieldToUpdate = WorkflowViewKeys.audits;
            String documentToUpdate = entityAuditRecord.getAffectedResourceId();
            String auditTimestampField = WorkflowViewKeys.auditTimestamps;
            Map<String, Object> auditRelatedEntityViewMap =
                relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
            result = result
                && searchDao.addTimestamp(
                       WorkflowSearchEntity.TYPE, auditTimestampField, documentToUpdate, DAYS_TO_RETAIN);
            result = result
                && searchDao.appendToListInSingleDocument(WorkflowSearchEntity.TYPE, fieldToUpdate, documentToUpdate,
                       auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
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
      String fieldToUpdate = WorkflowViewKeys.deployments;
      List<String> documentsToUpdate = workflowExecution.getWorkflowIds();
      Map<String, Object> deploymentRelatedEntityViewMap =
          relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
      String deploymentTimestampsField = WorkflowViewKeys.deploymentTimestamps;
      result &= searchDao.addTimestamp(
          WorkflowSearchEntity.TYPE, deploymentTimestampsField, documentsToUpdate, DAYS_TO_RETAIN);

      result &= searchDao.appendToListInMultipleDocuments(WorkflowSearchEntity.TYPE, fieldToUpdate, documentsToUpdate,
          deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
    }
    return result;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    DBObject changes = changeEvent.getChanges();
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    boolean result = true;
    if (workflowExecution.getWorkflowType().equals(WorkflowType.ORCHESTRATION)) {
      if (changes.containsField(WorkflowExecutionKeys.status)) {
        String entityType = WorkflowViewKeys.deployments;
        String newNameValue = workflowExecution.getStatus().toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
        result = searchDao.updateListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
      if (changes.containsField(WorkflowExecutionKeys.pipelineExecutionId)) {
        String entityType = WorkflowViewKeys.deployments;
        String newNameValue = workflowExecution.getPipelineExecutionId();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = DeploymentRelatedEntityViewKeys.pipelineExecutionId;
        result = result
            && searchDao.updateListInMultipleDocuments(
                   PipelineSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return result;
  }

  private boolean handleWorkflowExecutionChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowExecutionInsert(changeEvent);
      case UPDATE:
        return handleWorkflowExecutionUpdate(changeEvent);
      default:
        return true;
    }
  }

  private boolean handleServiceChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = WorkflowViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = ServiceKeys.name;
        return searchDao.updateListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleEnvironmentChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(EnvironmentKeys.name)) {
        String keyToUpdate = WorkflowViewKeys.environmentName;
        String newValue = document.get(EnvironmentKeys.name).toString();
        String filterKey = WorkflowViewKeys.environmentId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            WorkflowSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ApplicationKeys.name)) {
        String keyToUpdate = WorkflowViewKeys.appName;
        String newValue = document.get(ApplicationKeys.name).toString();
        String filterKey = WorkflowViewKeys.appId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            WorkflowSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleWorkflowInsert(ChangeEvent<?> changeEvent) {
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    WorkflowView workflowView = workflowViewBuilder.createWorkflowView(workflow);
    if (workflowView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
      }
    }
    return true;
  }

  private boolean handleWorkflowUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    WorkflowView workflowView = workflowViewBuilder.createWorkflowView(workflow, changeDocument);
    if (workflowView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
      }
    }
    return true;
  }

  private boolean handleWorkflowChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowInsert(changeEvent);
      case UPDATE:
        return handleWorkflowUpdate(changeEvent);
      case DELETE:
        return searchDao.deleteDocument(WorkflowSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(Workflow.class)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Application.class)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Service.class)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Environment.class)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.isChangeFor(WorkflowExecution.class)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
    }
    if (changeEvent.isChangeFor(AuditHeader.class)) {
      isChangeHandled = handleAuditRelatedChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Pipeline.class)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    return isChangeHandled;
  }
}
