package software.wings.search.entities.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationView.ApplicationViewKeys;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.pipeline.PipelineSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditSearchEntity;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.EntityInfo.EntityInfoKeys;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.util.Map;
import java.util.Optional;

/**
 * The handler which will maintain the application document
 * in the search engine database.
 *
 * @author utkarsh
 */

@Slf4j
@Singleton
public class ApplicationChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ApplicationViewBuilder applicationViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleAuditRelatedChange(ChangeEvent changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
      case UPDATE: {
        boolean result = true;
        if (changeEvent.getChanges() != null) {
          DBObject document = changeEvent.getFullDocument();
          AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
          if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
            AuditHeader auditHeader = (AuditHeader) mapper.fromDBObject(
                advancedDatastore, changeEvent.getEntityType(), document, entityCache);
            for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
              if (entityAuditRecord.getAffectedResourceType().equals(EntityType.APPLICATION.name())) {
                String fieldToUpdate = ApplicationViewKeys.audits;
                String filterId = entityAuditRecord.getAffectedResourceId();
                Map<String, Object> auditRelatedEntityViewMap =
                    relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
                String auditTimestampField = ApplicationViewKeys.auditTimestamps;
                result &=
                    searchDao.addTimestamp(ApplicationSearchEntity.TYPE, auditTimestampField, filterId, DAYS_TO_RETAIN);
                result &= searchDao.appendToListInSingleDocument(ApplicationSearchEntity.TYPE, fieldToUpdate, filterId,
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

  private boolean handleWorkflowChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        Workflow workflow = (Workflow) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), changeEvent.getFullDocument(), entityCache);
        workflow.setUuid(changeEvent.getUuid());
        EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
        Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);
        return searchDao.appendToListInSingleDocument(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.workflows, workflow.getAppId(), newElement);
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        if (changeDocument.get(WorkflowKeys.name) != null) {
          return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.workflows,
              changeDocument.get(WorkflowKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
        }
        return true;
      }
      case DELETE: {
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.workflows, changeEvent.getUuid());
      }
      default: { break; }
    }
    return true;
  }

  private boolean handleEnvironmentChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        Environment environment = (Environment) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), changeEvent.getFullDocument(), entityCache);
        environment.setUuid(changeEvent.getUuid());
        EntityInfo entityInfo = new EntityInfo(environment.getUuid(), environment.getName());
        Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);
        return searchDao.appendToListInSingleDocument(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.environments, environment.getAppId(), newElement);
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        if (changeDocument.get(EnvironmentKeys.name) != null) {
          return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.environments,
              changeDocument.get(EnvironmentKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
        }
        return true;
      }
      case DELETE: {
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.environments, changeEvent.getUuid());
      }
      default:
    }
    return true;
  }

  private boolean handlePipelineChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        Pipeline pipeline = (Pipeline) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), changeEvent.getFullDocument(), entityCache);
        pipeline.setUuid(changeEvent.getUuid());
        EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
        Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);
        return searchDao.appendToListInSingleDocument(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.pipelines, pipeline.getAppId(), newElement);
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        if (changeDocument.get(PipelineKeys.name) != null) {
          return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.pipelines,
              changeDocument.get(PipelineKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
        }
        return true;
      }
      case DELETE: {
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.pipelines, changeEvent.getUuid());
      }
      default:
    }
    return true;
  }

  private boolean handleServiceChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        Service service = (Service) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), changeEvent.getFullDocument(), entityCache);
        service.setUuid(changeEvent.getUuid());
        EntityInfo entityInfo = new EntityInfo(service.getUuid(), service.getName());
        Map<String, Object> newElement = new ObjectMapper().convertValue(entityInfo, Map.class);
        return searchDao.appendToListInSingleDocument(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.services, service.getAppId(), newElement);
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        if (changeDocument.get(ServiceKeys.name) != null) {
          return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.services,
              changeDocument.get(ServiceKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
        }
        return true;
      }
      case DELETE: {
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.services, changeEvent.getUuid());
      }
      default:
    }
    return true;
  }

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        Application application = (Application) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), changeEvent.getFullDocument(), entityCache);
        application.setUuid(changeEvent.getUuid());
        ApplicationView applicationView = applicationViewBuilder.createApplicationView(application);
        Optional<String> applicationViewJson = SearchEntityUtils.convertToJson(applicationView);
        if (applicationViewJson.isPresent()) {
          return searchDao.upsertDocument(
              ApplicationSearchEntity.TYPE, applicationView.getId(), applicationViewJson.get());
        }
        return false;
      }
      case UPDATE: {
        DBObject changeDocument = changeEvent.getChanges();
        Application application = (Application) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), changeEvent.getFullDocument(), entityCache);
        application.setUuid(changeEvent.getUuid());
        ApplicationView applicationView = applicationViewBuilder.createApplicationView(application, changeDocument);
        Optional<String> applicationViewJson = SearchEntityUtils.convertToJson(applicationView);
        if (applicationViewJson.isPresent()) {
          return searchDao.upsertDocument(
              ApplicationSearchEntity.TYPE, applicationView.getId(), applicationViewJson.get());
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(ApplicationSearchEntity.TYPE, changeEvent.getUuid());
      }
      default:
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    if (changeEvent.getEntityType().getSimpleName().equals(
            ApplicationSearchEntity.SOURCE_ENTITY_CLASS.getSimpleName())) {
      return handleApplicationChange(changeEvent);
    }
    if (changeEvent.getEntityType().getSimpleName().equals(
            RelatedAuditSearchEntity.SOURCE_ENTITY_CLASS.getSimpleName())) {
      return handleAuditRelatedChange(changeEvent);
    }
    if (changeEvent.getEntityType().getSimpleName().equals(WorkflowSearchEntity.SOURCE_ENTITY_CLASS.getSimpleName())) {
      return handleWorkflowChange(changeEvent);
    }
    if (changeEvent.getEntityType().getSimpleName().equals(ServiceSearchEntity.SOURCE_ENTITY_CLASS.getSimpleName())) {
      return handleServiceChange(changeEvent);
    }
    if (changeEvent.getEntityType().getSimpleName().equals(PipelineSearchEntity.SOURCE_ENTITY_CLASS.getSimpleName())) {
      return handlePipelineChange(changeEvent);
    }
    if (changeEvent.getEntityType().getSimpleName().equals(
            EnvironmentSearchEntity.SOURCE_ENTITY_CLASS.getSimpleName())) {
      return handleEnvironmentChange(changeEvent);
    }
    return true;
  }
}
