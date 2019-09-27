package software.wings.search.entities.pipeline;

import com.google.inject.Inject;
import com.google.inject.Singleton;

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
import software.wings.beans.Pipeline;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
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
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PipelineViewBuilder pipelineViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleAuditRelatedChangeHandler(ChangeEvent changeEvent) {
    DBObject document = changeEvent.getFullDocument();
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges() != null) {
          if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
            AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
            AuditHeader auditHeader = (AuditHeader) mapper.fromDBObject(
                advancedDatastore, changeEvent.getEntityType(), document, entityCache);
            for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
              if (entityAuditRecord.getAffectedResourceType().equals(EntityType.PIPELINE.name())) {
                String fieldToUpdate = PipelineViewKeys.audits;
                String filterId = entityAuditRecord.getAffectedResourceId();
                String auditTimestampField = PipelineViewKeys.auditTimestamps;
                Map<String, Object> auditRelatedEntityViewMap =
                    relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
                result &=
                    searchDao.addTimestamp(PipelineSearchEntity.TYPE, auditTimestampField, filterId, DAYS_TO_RETAIN);
                result &= searchDao.appendToListInSingleDocument(PipelineSearchEntity.TYPE, fieldToUpdate, filterId,
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

  private boolean handleWorkflowExecutionChangeHandler(ChangeEvent changeEvent) {
    DBObject fullDocument = changeEvent.getFullDocument();
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        boolean result = true;
        if ((fullDocument.get(WorkflowExecutionKeys.workflowType).toString()).equals(WorkflowType.PIPELINE.name())) {
          WorkflowExecution workflowExecution = (WorkflowExecution) mapper.fromDBObject(
              advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);

          String filterId = fullDocument.get(WorkflowExecutionKeys.workflowId).toString();
          String fieldToUpdate = WorkflowViewKeys.deployments;

          Map<String, Object> deploymentRelatedEntityViewMap =
              relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
          String deploymentTimestampsField = PipelineViewKeys.deploymentTimestamps;
          result &=
              searchDao.addTimestamp(PipelineSearchEntity.TYPE, deploymentTimestampsField, filterId, DAYS_TO_RETAIN);

          result &= searchDao.appendToListInSingleDocument(
              PipelineSearchEntity.TYPE, fieldToUpdate, filterId, deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
        }
        return result;
      }
      case UPDATE: {
        if ((fullDocument.get(WorkflowExecutionKeys.workflowType).toString()).equals(WorkflowType.PIPELINE.name())) {
          DBObject changes = changeEvent.getChanges();
          if (changes.containsField(WorkflowExecutionKeys.status)) {
            String entityType = WorkflowViewKeys.deployments;
            String newNameValue = fullDocument.get(DeploymentRelatedEntityViewKeys.status).toString();
            String filterId = changeEvent.getUuid();
            String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
            return searchDao.updateListInMultipleDocuments(
                PipelineSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
          }
        }
        return true;
      }
      case DELETE: {
        return true;
      }
      default: { break; }
    }
    return false;
  }

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
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

  private boolean handleServiceChange(ChangeEvent changeEvent) {
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

  private boolean handleWorkflowChange(ChangeEvent changeEvent) {
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

  private boolean handlePipelineChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        DBObject document = changeEvent.getFullDocument();
        Pipeline pipeline =
            (Pipeline) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), document, entityCache);
        pipeline.setUuid(changeEvent.getUuid());
        PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline);
        if (pipelineView != null) {
          Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
          if (jsonString.isPresent()) {
            return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
          }
        }
        return false;
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        DBObject fullDocument = changeEvent.getFullDocument();
        Pipeline pipeline =
            (Pipeline) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
        pipeline.setUuid(changeEvent.getUuid());
        PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline, changeDocument);
        if (pipelineView != null) {
          Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
          if (jsonString.isPresent()) {
            return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
          }
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(PipelineSearchEntity.TYPE, changeEvent.getUuid());
      }
      default: { break; }
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.getEntityType().equals(PipelineSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(WorkflowSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ServiceSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(DeploymentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowExecutionChangeHandler(changeEvent);
    }
    if (changeEvent.getEntityType().equals(RelatedAuditSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleAuditRelatedChangeHandler(changeEvent);
    }
    return isChangeHandled;
  }
}
