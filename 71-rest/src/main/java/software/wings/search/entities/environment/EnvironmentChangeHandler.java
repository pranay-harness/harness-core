package software.wings.search.entities.environment;

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
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.environment.EnvironmentView.EnvironmentViewKeys;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
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
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EnvironmentViewBuilder environmentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleWorkflowChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        if (changeEvent.getFullDocument().containsField(WorkflowKeys.envId)) {
          DBObject fullDocument = changeEvent.getFullDocument();
          Workflow workflow =
              (Workflow) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
          String fieldToUpdate = EnvironmentViewKeys.workflows;
          String filterId = fullDocument.get(WorkflowKeys.envId).toString();
          EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
          Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);
          return searchDao.appendToListInSingleDocument(
              EnvironmentSearchEntity.TYPE, fieldToUpdate, filterId, newElement);
        }
        return true;
      }
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges().containsField(WorkflowKeys.envId)) {
          DBObject fullDocument = changeEvent.getFullDocument();
          Workflow workflow =
              (Workflow) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
          workflow.setUuid(changeEvent.getUuid());
          String fieldToUpdate = EnvironmentViewKeys.workflows;
          String presentEnvId =
              searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.workflows, changeEvent.getUuid())
                  .get(0);
          String incomingEnvId = fullDocument.get(WorkflowKeys.envId).toString();

          EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
          Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);

          if (incomingEnvId != null) {
            result &= searchDao.appendToListInSingleDocument(
                EnvironmentSearchEntity.TYPE, fieldToUpdate, incomingEnvId, newElement);
          }
          if (presentEnvId != null) {
            result &= searchDao.removeFromListInMultipleDocuments(
                EnvironmentSearchEntity.TYPE, fieldToUpdate, presentEnvId, changeEvent.getUuid());
          }
        }
        if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
          DBObject document = changeEvent.getChanges();
          String entityType = EnvironmentViewKeys.workflows;
          String newValue = document.get(WorkflowKeys.name).toString();
          String filterId = changeEvent.getUuid();
          String fieldToUpdate = WorkflowKeys.name;
          result &= searchDao.updateListInMultipleDocuments(
              EnvironmentSearchEntity.TYPE, entityType, newValue, filterId, fieldToUpdate);
        }
        return result;
      }
      case DELETE: {
        String fieldToUpdate = EnvironmentViewKeys.workflows;
        String toBeDeletedEnvId =
            searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.workflows, changeEvent.getUuid())
                .get(0);
        return searchDao.removeFromListInMultipleDocuments(
            EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedEnvId, changeEvent.getUuid());
      }
      default: { break; }
    }
    return false;
  }

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
          String fieldToUpdate = EnvironmentViewKeys.pipelines;
          Set<String> incomingEnvIds = Sets.newHashSet(environmentViewBuilder.populateEnvIds(pipeline));
          Set<String> currentEnvIds = Sets.newHashSet(
              searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.pipelines, pipeline.getUuid()));
          List<String> toBeAddedEnvIds =
              new ArrayList<>(Sets.difference(incomingEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));
          List<String> toBeDeletedEnvIds =
              new ArrayList<>(Sets.difference(currentEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));
          EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
          Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);

          if (toBeAddedEnvIds.size() > 0) {
            result &= searchDao.appendToListInMultipleDocuments(
                EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeAddedEnvIds, newElement);
          }
          if (toBeDeletedEnvIds.size() > 0) {
            result &= searchDao.removeFromListInMultipleDocuments(
                EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedEnvIds, pipeline.getUuid());
          }
        }
        if (changeEvent.getChanges().containsField(PipelineKeys.name)) {
          DBObject document = changeEvent.getChanges();
          String entityType = EnvironmentViewKeys.pipelines;
          String newValue = document.get(PipelineKeys.name).toString();
          String filterId = changeEvent.getUuid();
          String fieldToUpdate = PipelineKeys.name;
          result &= searchDao.updateListInMultipleDocuments(
              EnvironmentSearchEntity.TYPE, entityType, newValue, filterId, fieldToUpdate);
        }
        return result;
      }
      case DELETE: {
        String fieldToUpdate = EnvironmentViewKeys.pipelines;
        List<String> toBeDeletedPipelineIds =
            searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.pipelines, changeEvent.getUuid());
        return searchDao.removeFromListInMultipleDocuments(
            EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedPipelineIds, changeEvent.getUuid());
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
        if (fullDocument.containsField(WorkflowExecutionKeys.envIds)) {
          WorkflowExecution workflowExecution = (WorkflowExecution) mapper.fromDBObject(
              advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
          String fieldToUpdate = EnvironmentViewKeys.deployments;
          String filterId = workflowExecution.getEnvId();
          Map<String, Object> deploymentRelatedEntityViewMap =
              relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
          String deploymentTimestampsField = EnvironmentViewKeys.deploymentTimestamps;
          result &=
              searchDao.addTimestamp(EnvironmentSearchEntity.TYPE, deploymentTimestampsField, filterId, DAYS_TO_RETAIN);
          result &= searchDao.appendToListInSingleDocument(EnvironmentSearchEntity.TYPE, fieldToUpdate, filterId,
              deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
        }
        return result;
      }
      case UPDATE: {
        if ((fullDocument.get(WorkflowExecutionKeys.workflowType).toString())
                .equals(WorkflowType.ORCHESTRATION.name())) {
          DBObject changes = changeEvent.getChanges();
          if (changes.containsField(WorkflowExecutionKeys.status)) {
            String entityType = EnvironmentViewKeys.deployments;
            String newNameValue = fullDocument.get(WorkflowExecutionKeys.status).toString();
            String filterId = changeEvent.getUuid();
            String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
            return searchDao.updateListInMultipleDocuments(
                EnvironmentSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
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

  private boolean handleAuditRelatedChangeHandler(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges() != null) {
          if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
            DBObject fullDocument = changeEvent.getFullDocument();
            AuditHeader auditHeader = (AuditHeader) mapper.fromDBObject(
                advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
            for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
              if (entityAuditRecord.getAffectedResourceType().equals(EntityType.ENVIRONMENT.name())) {
                String fieldToUpdate = EnvironmentViewKeys.audits;
                String filterId = entityAuditRecord.getAffectedResourceId();
                String auditTimestampField = EnvironmentViewKeys.auditTimestamps;
                Map<String, Object> auditRelatedEntityViewMap =
                    relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
                result &=
                    searchDao.addTimestamp(EnvironmentSearchEntity.TYPE, auditTimestampField, filterId, DAYS_TO_RETAIN);
                result &= searchDao.appendToListInSingleDocument(EnvironmentSearchEntity.TYPE, fieldToUpdate, filterId,
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

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
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

  private boolean handleEnvironmentChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        DBObject document = changeEvent.getFullDocument();
        Environment environment =
            (Environment) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), document, entityCache);
        environment.setUuid(changeEvent.getUuid());
        EnvironmentView environmentView =
            environmentViewBuilder.createEnvironmentView(environment, changeEvent.getChanges());

        if (environmentView.getAppId() != null && changeEvent.getChangeType().equals(ChangeType.INSERT)) {
          Application application = wingsPersistence.get(Application.class, environment.getAppId());
          if (application != null) {
            environmentView.setAppName(application.getName());
          }
        }
        Optional<String> jsonString = SearchEntityUtils.convertToJson(environmentView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(EnvironmentSearchEntity.TYPE, environmentView.getId(), jsonString.get());
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(EnvironmentSearchEntity.TYPE, changeEvent.getUuid());
      }
      default: { break; }
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.getEntityType().equals(EnvironmentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(RelatedAuditSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleAuditRelatedChangeHandler(changeEvent);
    }
    if (changeEvent.getEntityType().equals(DeploymentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowExecutionChangeHandler(changeEvent);
    }
    if (changeEvent.getEntityType().equals(PipelineSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(WorkflowSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }

    return isChangeHandled;
  }
}
