package software.wings.search.entities.service;

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
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.service.ServiceView.ServiceViewKeys;
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
public class ServiceChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceViewBuilder serviceViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleWorkflowChange(ChangeEvent changeEvent) {
    DBObject fullDocument = changeEvent.getFullDocument();
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        Workflow workflow =
            (Workflow) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
        String fieldToUpdate = ServiceViewKeys.workflows;
        List<String> incomingServiceIds = serviceViewBuilder.getServiceIds(workflow);

        EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
        Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);

        if (incomingServiceIds != null) {
          return searchDao.appendToListInMultipleDocuments(
              ServiceSearchEntity.TYPE, fieldToUpdate, incomingServiceIds, newElement);
        }
        return true;
      }
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges().containsField(WorkflowKeys.orchestration)) {
          Workflow workflow =
              (Workflow) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
          String fieldToUpdate = ServiceViewKeys.workflows;

          Set<String> incomingServiceIds = Sets.newHashSet(serviceViewBuilder.getServiceIds(workflow));
          Set<String> currentServiceIds = Sets.newHashSet(
              searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.workflows, workflow.getUuid()));

          List<String> toBeAddedServiceIds = new ArrayList<>(
              Sets.difference(incomingServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));
          List<String> toBeDeletedServiceIds = new ArrayList<>(
              Sets.difference(currentServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));

          EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
          Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);

          if (toBeAddedServiceIds.size() > 0) {
            result &= searchDao.appendToListInMultipleDocuments(
                ServiceSearchEntity.TYPE, fieldToUpdate, toBeAddedServiceIds, newElement);
          }
          if (toBeDeletedServiceIds.size() > 0) {
            result &= searchDao.removeFromListInMultipleDocuments(
                ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, workflow.getUuid());
          }
        }
        if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
          DBObject document = changeEvent.getChanges();
          String entityType = ServiceViewKeys.workflows;
          String newValue = document.get(WorkflowKeys.name).toString();
          String filterId = changeEvent.getUuid();
          String fieldToUpdate = WorkflowKeys.name;
          result &= searchDao.updateListInMultipleDocuments(
              ServiceSearchEntity.TYPE, entityType, newValue, filterId, fieldToUpdate);
        }
        return result;
      }
      case DELETE: {
        String fieldToUpdate = ServiceViewKeys.workflows;
        List<String> toBeDeletedServiceIds =
            searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.workflows, changeEvent.getUuid());
        return searchDao.removeFromListInMultipleDocuments(
            ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, changeEvent.getUuid());
      }
      default: { break; }
    }
    return false;
  }

  private boolean handlePipelineChange(ChangeEvent changeEvent) {
    DBObject fullDocument = changeEvent.getFullDocument();
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        return true;
      }
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges().containsField(PipelineKeys.pipelineStages)) {
          Pipeline pipeline =
              (Pipeline) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
          String fieldToUpdate = ServiceViewKeys.pipelines;
          Set<String> incomingServiceIds = Sets.newHashSet(serviceViewBuilder.getServiceIds(pipeline));
          Set<String> currentServiceIds = Sets.newHashSet(
              searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.pipelines, pipeline.getUuid()));

          List<String> toBeAddedServiceIds = new ArrayList<>(
              Sets.difference(incomingServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));
          List<String> toBeDeletedServiceIds = new ArrayList<>(
              Sets.difference(currentServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));

          EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
          Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);

          if (toBeAddedServiceIds.size() > 0) {
            result &= searchDao.appendToListInMultipleDocuments(
                ServiceSearchEntity.TYPE, fieldToUpdate, toBeAddedServiceIds, newElement);
          }
          if (toBeDeletedServiceIds.size() > 0) {
            result &= searchDao.removeFromListInMultipleDocuments(
                ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, pipeline.getUuid());
          }
        }
        if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
          DBObject document = changeEvent.getChanges();
          String entityType = ServiceViewKeys.pipelines;
          String newValue = document.get(PipelineKeys.name).toString();
          String filterId = changeEvent.getUuid();
          String fieldToUpdate = PipelineKeys.name;
          result &= searchDao.updateListInMultipleDocuments(
              ServiceSearchEntity.TYPE, entityType, newValue, filterId, fieldToUpdate);
        }
        return result;
      }
      case DELETE: {
        String fieldToUpdate = ServiceViewKeys.pipelines;
        List<String> toBeDeletedServiceIds =
            searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.pipelines, changeEvent.getUuid());
        return searchDao.removeFromListInMultipleDocuments(
            ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, changeEvent.getUuid());
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
        if (fullDocument.containsField(WorkflowExecutionKeys.serviceIds)) {
          WorkflowExecution workflowExecution = (WorkflowExecution) mapper.fromDBObject(
              advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
          String fieldToUpdate = ServiceViewKeys.deployments;
          if (workflowExecution.getServiceIds() != null) {
            String filterId = workflowExecution.getServiceIds().get(0);
            Map<String, Object> deploymentRelatedEntityViewMap =
                relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
            String deploymentTimestampsField = ServiceViewKeys.deploymentTimestamps;
            result &=
                searchDao.addTimestamp(ServiceSearchEntity.TYPE, deploymentTimestampsField, filterId, DAYS_TO_RETAIN);
            result &= searchDao.appendToListInSingleDocument(ServiceSearchEntity.TYPE, fieldToUpdate, filterId,
                deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
          }
        }
        return result;
      }
      case UPDATE: {
        if ((fullDocument.get(WorkflowExecutionKeys.workflowType).toString())
                .equals(WorkflowType.ORCHESTRATION.name())) {
          DBObject changes = changeEvent.getChanges();
          if (changes.containsField(WorkflowExecutionKeys.status)) {
            String entityType = ServiceViewKeys.deployments;
            String newNameValue = fullDocument.get(WorkflowExecutionKeys.status).toString();
            String filterId = changeEvent.getUuid();
            String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
            return searchDao.updateListInMultipleDocuments(
                ServiceSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
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

  private boolean handleAuditRelatedChange(ChangeEvent changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges() != null) {
          if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
            DBObject fullDocument = changeEvent.getFullDocument();
            AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
            AuditHeader auditHeader = (AuditHeader) mapper.fromDBObject(
                advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
            for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
              if (entityAuditRecord.getAffectedResourceType().equals(EntityType.SERVICE.name())) {
                String fieldToUpdate = ServiceViewKeys.audits;
                String filterId = entityAuditRecord.getAffectedResourceId();
                String auditTimestampField = ServiceViewKeys.auditTimestamps;
                Map<String, Object> auditRelatedEntityViewMap =
                    relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
                result &=
                    searchDao.addTimestamp(ServiceSearchEntity.TYPE, auditTimestampField, filterId, DAYS_TO_RETAIN);
                result &= searchDao.appendToListInSingleDocument(
                    ServiceSearchEntity.TYPE, fieldToUpdate, filterId, auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
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
        String keyToUpdate = ServiceViewKeys.appName;
        String newValue = document.get(ApplicationKeys.name).toString();
        String filterKey = ServiceViewKeys.appId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            ServiceSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleServiceChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        DBObject document = changeEvent.getFullDocument();
        Service service =
            (Service) mapper.fromDBObject(advancedDatastore, changeEvent.getEntityType(), document, entityCache);
        service.setUuid(changeEvent.getUuid());
        ServiceView serviceView = serviceViewBuilder.createServiceView(service, changeEvent.getChanges());

        if (serviceView.getAppId() != null && changeEvent.getChangeType().equals(ChangeType.INSERT)) {
          Application application = wingsPersistence.get(Application.class, service.getAppId());
          if (application != null) {
            serviceView.setAppName(application.getName());
          }
        }

        Optional<String> jsonString = SearchEntityUtils.convertToJson(serviceView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(ServiceSearchEntity.TYPE, serviceView.getId(), jsonString.get());
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(ServiceSearchEntity.TYPE, changeEvent.getUuid());
      }
      default: { break; }
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.getEntityType().equals(ServiceSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(RelatedAuditSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleAuditRelatedChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(DeploymentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
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
