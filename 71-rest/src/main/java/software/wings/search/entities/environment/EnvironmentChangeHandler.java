package software.wings.search.entities.environment;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.search.entities.environment.EnvironmentView.EnvironmentViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
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

@Slf4j
@Singleton
public class EnvironmentChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private EnvironmentViewBuilder environmentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleWorkflowInsert(ChangeEvent<?> changeEvent) {
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    if (workflow.getEnvId() != null) {
      String fieldToUpdate = EnvironmentViewKeys.workflows;
      String documentToUpdate = workflow.getEnvId();
      EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);
      return searchDao.appendToListInSingleDocument(
          EnvironmentSearchEntity.TYPE, fieldToUpdate, documentToUpdate, newElement);
    }
    return true;
  }

  private boolean handleWorkflowUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(WorkflowKeys.envId)) {
      Workflow workflow = (Workflow) changeEvent.getFullDocument();
      String fieldToUpdate = EnvironmentViewKeys.workflows;
      String presentEnvId =
          searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.workflows, changeEvent.getUuid())
              .get(0);
      String incomingEnvId = workflow.getEnvId();

      EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (incomingEnvId != null) {
        result = searchDao.appendToListInSingleDocument(
            EnvironmentSearchEntity.TYPE, fieldToUpdate, incomingEnvId, newElement);
      }
      if (presentEnvId != null) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                   EnvironmentSearchEntity.TYPE, fieldToUpdate, presentEnvId, changeEvent.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = EnvironmentViewKeys.workflows;
      String newValue = document.get(WorkflowKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = WorkflowKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
                 EnvironmentSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handleWorkflowDelete(ChangeEvent<?> changeEvent) {
    String fieldToUpdate = EnvironmentViewKeys.workflows;
    String toBeDeletedEnvId =
        searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.workflows, changeEvent.getUuid())
            .get(0);
    return searchDao.removeFromListInMultipleDocuments(
        EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedEnvId, changeEvent.getUuid());
  }

  private boolean handleWorkflowChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowInsert(changeEvent);
      case UPDATE:
        return handleWorkflowUpdate(changeEvent);
      case DELETE:
        return handleWorkflowDelete(changeEvent);
      default:
        return true;
    }
  }

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(PipelineKeys.pipelineStages)) {
      Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
      String fieldToUpdate = EnvironmentViewKeys.pipelines;
      Set<String> incomingEnvIds = Sets.newHashSet(environmentViewBuilder.populateEnvIds(pipeline));
      Set<String> currentEnvIds = Sets.newHashSet(
          searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.pipelines, pipeline.getUuid()));
      List<String> toBeAddedEnvIds =
          new ArrayList<>(Sets.difference(incomingEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));
      List<String> toBeDeletedEnvIds =
          new ArrayList<>(Sets.difference(currentEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));
      EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (!toBeAddedEnvIds.isEmpty()) {
        result = searchDao.appendToListInMultipleDocuments(
            EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeAddedEnvIds, newElement);
      }
      if (!toBeDeletedEnvIds.isEmpty()) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                   EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedEnvIds, pipeline.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(PipelineKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = EnvironmentViewKeys.pipelines;
      String newValue = document.get(PipelineKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = PipelineKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
                 EnvironmentSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handlePiplineDelete(ChangeEvent<?> changeEvent) {
    String fieldToUpdate = EnvironmentViewKeys.pipelines;
    List<String> toBeDeletedPipelineIds =
        searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.pipelines, changeEvent.getUuid());
    return searchDao.removeFromListInMultipleDocuments(
        EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedPipelineIds, changeEvent.getUuid());
  }

  private boolean handlePipelineChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case UPDATE:
        return handlePipelineUpdate(changeEvent);
      case DELETE:
        return handlePiplineDelete(changeEvent);
      default:
        return true;
    }
  }

  private boolean handleWorkflowExecutionInsert(ChangeEvent<?> changeEvent) {
    boolean result = true;
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    if (workflowExecution.getEnvIds() != null) {
      String fieldToUpdate = EnvironmentViewKeys.deployments;
      List<String> documentsToUpdate = workflowExecution.getEnvIds();
      Map<String, Object> deploymentRelatedEntityViewMap =
          relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
      String deploymentTimestampsField = EnvironmentViewKeys.deploymentTimestamps;
      result &= searchDao.addTimestamp(
          EnvironmentSearchEntity.TYPE, deploymentTimestampsField, documentsToUpdate, DAYS_TO_RETAIN);
      result &= searchDao.appendToListInMultipleDocuments(EnvironmentSearchEntity.TYPE, fieldToUpdate,
          documentsToUpdate, deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
    }
    return result;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    DBObject changes = changeEvent.getChanges();
    if (changes.containsField(WorkflowExecutionKeys.status)) {
      String entityType = EnvironmentViewKeys.deployments;
      String newNameValue = workflowExecution.getStatus().toString();
      String documentToUpdate = workflowExecution.getUuid();
      String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
      return searchDao.updateListInMultipleDocuments(
          EnvironmentSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
    }
    return true;
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

  private boolean handleAuditRelatedChangeHandler(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType().equals(ChangeType.UPDATE)) {
      boolean result = true;
      if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
        AuditHeader auditHeader = (AuditHeader) changeEvent.getFullDocument();
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getAffectedResourceType().equals(EntityType.ENVIRONMENT.name())) {
            String fieldToUpdate = EnvironmentViewKeys.audits;
            String documentToUpdate = entityAuditRecord.getAffectedResourceId();
            String auditTimestampField = EnvironmentViewKeys.auditTimestamps;
            Map<String, Object> auditRelatedEntityViewMap =
                relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
            result = result
                && searchDao.addTimestamp(
                       EnvironmentSearchEntity.TYPE, auditTimestampField, documentToUpdate, DAYS_TO_RETAIN);
            result = result
                && searchDao.appendToListInSingleDocument(EnvironmentSearchEntity.TYPE, fieldToUpdate, documentToUpdate,
                       auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
          }
        }
      }
      return result;
    }
    return true;
  }

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ApplicationKeys.name)) {
        String keyToUpdate = EnvironmentViewKeys.appName;
        String newValue = document.get(ApplicationKeys.name).toString();
        String filterKey = EnvironmentViewKeys.appId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            EnvironmentSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleEnvironmnetInsert(ChangeEvent<?> changeEvent) {
    Environment environment = (Environment) changeEvent.getFullDocument();
    EnvironmentView environmentView = environmentViewBuilder.createEnvironmentView(environment);
    Optional<String> jsonString = SearchEntityUtils.convertToJson(environmentView);
    if (jsonString.isPresent()) {
      return searchDao.upsertDocument(EnvironmentSearchEntity.TYPE, environmentView.getId(), jsonString.get());
    }
    return false;
  }

  private boolean handleEnvironmentUpdate(ChangeEvent<?> changeEvent) {
    Environment environment = (Environment) changeEvent.getFullDocument();
    EnvironmentView environmentView =
        environmentViewBuilder.createEnvironmentView(environment, changeEvent.getChanges());
    Optional<String> jsonString = SearchEntityUtils.convertToJson(environmentView);
    if (jsonString.isPresent()) {
      return searchDao.upsertDocument(EnvironmentSearchEntity.TYPE, environmentView.getId(), jsonString.get());
    }
    return false;
  }

  private boolean handleEnvironmentChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleEnvironmnetInsert(changeEvent);
      case UPDATE:
        return handleEnvironmentUpdate(changeEvent);
      case DELETE:
        return searchDao.deleteDocument(EnvironmentSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(Environment.class)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Application.class)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(AuditHeader.class)) {
      isChangeHandled = handleAuditRelatedChangeHandler(changeEvent);
    }
    if (changeEvent.isChangeFor(WorkflowExecution.class)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Pipeline.class)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Workflow.class)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }

    return isChangeHandled;
  }
}
