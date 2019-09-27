package software.wings.search.entities.workflow;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import io.harness.beans.WorkflowType;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.EntityType;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.service.ServiceSearchEntity;
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

@Slf4j
@Singleton
public class WorkflowChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowViewBuilder workflowViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handlePipelineChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        return true;
      }
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges().containsField(PipelineKeys.pipelineStages)) {
          DBObject fullDocument = changeEvent.getFullDocument();
          Pipeline pipeline =
              (Pipeline) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
          String fieldToUpdate = WorkflowViewKeys.pipelines;

          Set<String> incomingWorkflowIds = Sets.newHashSet(workflowViewBuilder.populateWorkflowIds(pipeline));
          Set<String> currentWorkflowIds = Sets.newHashSet(
              searchDao.nestedQuery(WorkflowSearchEntity.TYPE, WorkflowViewKeys.pipelines, pipeline.getUuid()));

          List<String> toBeAddedWorkflowIds = new ArrayList<>(
              Sets.difference(incomingWorkflowIds, Sets.intersection(incomingWorkflowIds, currentWorkflowIds)));
          List<String> toBeDeletedWorkflowIds = new ArrayList<>(
              Sets.difference(currentWorkflowIds, Sets.intersection(incomingWorkflowIds, currentWorkflowIds)));

          EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
          Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);

          if (toBeAddedWorkflowIds.size() > 0) {
            result &= searchDao.appendToListInMultipleDocuments(
                WorkflowSearchEntity.TYPE, fieldToUpdate, toBeAddedWorkflowIds, newElement);
          }
          if (toBeDeletedWorkflowIds.size() > 0) {
            result &= searchDao.removeFromListInMultipleDocuments(
                WorkflowSearchEntity.TYPE, fieldToUpdate, toBeDeletedWorkflowIds, pipeline.getUuid());
          }
        }
        if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
          DBObject document = changeEvent.getChanges();
          String entityType = WorkflowViewKeys.pipelines;
          String newValue = document.get(WorkflowKeys.name).toString();
          String filterId = changeEvent.getUuid();
          String fieldToUpdate = WorkflowKeys.name;
          result &= searchDao.updateListInMultipleDocuments(
              WorkflowSearchEntity.TYPE, entityType, newValue, filterId, fieldToUpdate);
        }
        return result;
      }
      case DELETE: {
        String fieldToUpdate = WorkflowViewKeys.pipelines;
        List<String> toBeDeletedWorkflowIds =
            searchDao.nestedQuery(WorkflowSearchEntity.TYPE, WorkflowViewKeys.pipelines, changeEvent.getUuid());
        return searchDao.removeFromListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, fieldToUpdate, toBeDeletedWorkflowIds, changeEvent.getUuid());
      }
      default: { break; }
    }
    return false;
  }

  private boolean handleAuditRelatedChange(ChangeEvent changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        boolean result = true;
        DBObject fullDocument = changeEvent.getFullDocument();
        AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
        if (changeEvent.getChanges() != null) {
          if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
            AuditHeader auditHeader = (AuditHeader) mapper.fromDBObject(
                advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
            for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
              if (entityAuditRecord.getAffectedResourceType().equals(EntityType.WORKFLOW.name())) {
                String fieldToUpdate = WorkflowViewKeys.audits;
                String filterId = entityAuditRecord.getAffectedResourceId();
                String auditTimestampField = WorkflowViewKeys.auditTimestamps;
                Map<String, Object> auditRelatedEntityViewMap =
                    relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
                result &=
                    searchDao.addTimestamp(WorkflowSearchEntity.TYPE, auditTimestampField, filterId, DAYS_TO_RETAIN);
                result &= searchDao.appendToListInSingleDocument(WorkflowSearchEntity.TYPE, fieldToUpdate, filterId,
                    auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
              }
            }
          }
        }
        return result;
      }
      case DELETE: {
        return true;
      }
      default: { break; }
    }
    return false;
  }

  private boolean handleWorkflowExecutionChange(ChangeEvent changeEvent) {
    DBObject fullDocument = changeEvent.getFullDocument();
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        boolean result = true;
        if ((fullDocument.get(WorkflowExecutionKeys.workflowType).toString())
                .equals(WorkflowType.ORCHESTRATION.name())) {
          WorkflowExecution workflowExecution = (WorkflowExecution) mapper.fromDBObject(
              advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);

          String fieldToUpdate = WorkflowViewKeys.deployments;
          String filterId = fullDocument.get(WorkflowExecutionKeys.workflowId).toString();
          Map<String, Object> deploymentRelatedEntityViewMap =
              relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
          String deploymentTimestampsField = WorkflowViewKeys.deploymentTimestamps;
          result &=
              searchDao.addTimestamp(WorkflowSearchEntity.TYPE, deploymentTimestampsField, filterId, DAYS_TO_RETAIN);

          result &= searchDao.appendToListInSingleDocument(
              WorkflowSearchEntity.TYPE, fieldToUpdate, filterId, deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
        }
        return result;
      }
      case UPDATE: {
        DBObject changes = changeEvent.getChanges();
        boolean result = true;
        if ((fullDocument.get(WorkflowExecutionKeys.workflowType).toString())
                .equals(WorkflowType.ORCHESTRATION.name())) {
          if (changes.containsField(WorkflowExecutionKeys.status)) {
            String entityType = WorkflowViewKeys.deployments;
            String newNameValue = fullDocument.get(WorkflowExecutionKeys.status).toString();
            String filterId = changeEvent.getUuid();
            String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
            result = searchDao.updateListInMultipleDocuments(
                WorkflowSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
          }
          if (changes.containsField(WorkflowExecutionKeys.pipelineExecutionId)) {
            String entityType = WorkflowViewKeys.deployments;
            String newNameValue = fullDocument.get(WorkflowExecutionKeys.pipelineExecutionId).toString();
            String filterId = changeEvent.getUuid();
            String fieldToUpdate = DeploymentRelatedEntityViewKeys.pipelineExecutionId;
            result = result
                & searchDao.updateListInMultipleDocuments(
                      PipelineSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
          }
        }
        return result;
      }
      case DELETE: {
        return true;
      }
      default: { break; }
    }
    return false;
  }

  private boolean handleServiceChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = WorkflowViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String filterId = changeEvent.getUuid();
        String fieldToUpdate = ServiceKeys.name;
        return searchDao.updateListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleEnvironmentChange(ChangeEvent changeEvent) {
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

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
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

  private boolean handleWorkflowChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        DBObject fullDocument = changeEvent.getFullDocument();
        Workflow workflow =
            (Workflow) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
        workflow.setUuid(changeEvent.getUuid());
        WorkflowView workflowView = workflowViewBuilder.createWorkflowView(workflow);
        if (workflowView != null) {
          Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
          if (jsonString.isPresent()) {
            return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
          }
        }
        return false;
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        DBObject fullDocument = changeEvent.getFullDocument();
        Workflow workflow =
            (Workflow) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
        workflow.setUuid(changeEvent.getUuid());
        WorkflowView workflowView = workflowViewBuilder.createWorkflowView(workflow, changeDocument);
        if (workflowView != null) {
          Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
          if (jsonString.isPresent()) {
            return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
          }
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(WorkflowSearchEntity.TYPE, changeEvent.getUuid());
      }
      default: { break; }
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.getEntityType().equals(WorkflowSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ServiceSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(EnvironmentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(DeploymentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(RelatedAuditSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleAuditRelatedChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(PipelineSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    return isChangeHandled;
  }
}
