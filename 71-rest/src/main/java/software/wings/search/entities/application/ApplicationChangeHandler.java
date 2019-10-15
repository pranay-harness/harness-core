package software.wings.search.entities.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.search.entities.application.ApplicationView.ApplicationViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.EntityInfo.EntityInfoKeys;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.Map;
import java.util.Optional;

/**
 * The handler which will maintain the application document
 * in the search engine database.
 *
 * @author ujjawal
 */

@Slf4j
@Singleton
public class ApplicationChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private ApplicationViewBuilder applicationViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleAuditRelatedChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType().equals(ChangeType.UPDATE) && changeEvent.getChanges() != null) {
      boolean result = true;
      AuditHeader auditHeader = (AuditHeader) changeEvent.getFullDocument();
      if (changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
        for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
          if (entityAuditRecord.getEntityType().equals(EntityType.APPLICATION.name())
              && !entityAuditRecord.getAffectedResourceOperation().equals(Type.DELETE.name())) {
            String fieldToUpdate = ApplicationViewKeys.audits;
            String filterId = entityAuditRecord.getAffectedResourceId();
            Map<String, Object> auditRelatedEntityViewMap =
                relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
            String auditTimestampField = ApplicationViewKeys.auditTimestamps;
            result &=
                searchDao.addTimestamp(ApplicationSearchEntity.TYPE, auditTimestampField, filterId, DAYS_TO_RETAIN);
            result &= searchDao.appendToListInSingleDocument(
                ApplicationSearchEntity.TYPE, fieldToUpdate, filterId, auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
          }
        }
      }
      return result;
    }
    return true;
  }

  private boolean handleWorkflowInsert(ChangeEvent<?> changeEvent) {
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
    Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);
    return searchDao.appendToListInSingleDocument(
        ApplicationSearchEntity.TYPE, ApplicationViewKeys.workflows, workflow.getAppId(), newElement);
  }

  private boolean handleWorkflowUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    if (changeDocument.get(WorkflowKeys.name) != null) {
      return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.workflows,
          changeDocument.get(WorkflowKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
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
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.workflows, changeEvent.getUuid());
      default:
        return true;
    }
  }

  private boolean handleEnvironmentInsert(ChangeEvent<?> changeEvent) {
    Environment environment = (Environment) changeEvent.getFullDocument();
    EntityInfo entityInfo = new EntityInfo(environment.getUuid(), environment.getName());
    Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);
    return searchDao.appendToListInSingleDocument(
        ApplicationSearchEntity.TYPE, ApplicationViewKeys.environments, environment.getAppId(), newElement);
  }

  private boolean handleEnvironmentUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    if (changeDocument.get(EnvironmentKeys.name) != null) {
      return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.environments,
          changeDocument.get(EnvironmentKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
    }
    return true;
  }

  private boolean handleEnvironmentChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleEnvironmentInsert(changeEvent);
      case UPDATE:
        return handleEnvironmentUpdate(changeEvent);
      case DELETE:
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.environments, changeEvent.getUuid());
      default:
        return true;
    }
  }

  private boolean handlePipelineInsert(ChangeEvent<?> changeEvent) {
    Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
    EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
    Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);
    return searchDao.appendToListInSingleDocument(
        ApplicationSearchEntity.TYPE, ApplicationViewKeys.pipelines, pipeline.getAppId(), newElement);
  }

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    if (changeDocument.get(PipelineKeys.name) != null) {
      return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.pipelines,
          changeDocument.get(PipelineKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
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
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.pipelines, changeEvent.getUuid());
      default:
        return true;
    }
  }

  private boolean handleServiceInsert(ChangeEvent<?> changeEvent) {
    Service service = (Service) changeEvent.getFullDocument();
    EntityInfo entityInfo = new EntityInfo(service.getUuid(), service.getName());
    Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);
    return searchDao.appendToListInSingleDocument(
        ApplicationSearchEntity.TYPE, ApplicationViewKeys.services, service.getAppId(), newElement);
  }

  private boolean handleServiceUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    if (changeDocument.get(ServiceKeys.name) != null) {
      return searchDao.updateListInMultipleDocuments(ApplicationSearchEntity.TYPE, ApplicationViewKeys.services,
          changeDocument.get(ServiceKeys.name).toString(), changeEvent.getUuid(), EntityInfoKeys.name);
    }
    return true;
  }

  private boolean handleServiceChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleServiceInsert(changeEvent);
      case UPDATE:
        return handleServiceUpdate(changeEvent);
      case DELETE:
        return searchDao.removeFromListInMultipleDocuments(
            ApplicationSearchEntity.TYPE, ApplicationViewKeys.services, changeEvent.getUuid());
      default:
        return true;
    }
  }

  private boolean handleApplicationInsert(ChangeEvent<?> changeEvent) {
    Application application = (Application) changeEvent.getFullDocument();
    ApplicationView applicationView = applicationViewBuilder.createApplicationView(application, false);
    Optional<String> applicationViewJson = SearchEntityUtils.convertToJson(applicationView);
    if (applicationViewJson.isPresent()) {
      return searchDao.upsertDocument(ApplicationSearchEntity.TYPE, applicationView.getId(), applicationViewJson.get());
    }
    return false;
  }

  private boolean handleApplicationUpdate(ChangeEvent<?> changeEvent) {
    Application application = (Application) changeEvent.getFullDocument();
    ApplicationView applicationView = applicationViewBuilder.createApplicationView(application, true);
    Optional<String> applicationViewJson = SearchEntityUtils.convertToJson(applicationView);
    if (applicationViewJson.isPresent()) {
      return searchDao.upsertDocument(ApplicationSearchEntity.TYPE, applicationView.getId(), applicationViewJson.get());
    }
    return false;
  }

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleApplicationInsert(changeEvent);
      case UPDATE:
        return handleApplicationUpdate(changeEvent);
      case DELETE:
        return searchDao.deleteDocument(ApplicationSearchEntity.TYPE, changeEvent.getUuid());

      default:
        return true;
    }
  }

  public boolean handleChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.isChangeFor(Application.class)) {
      return handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(AuditHeader.class)) {
      return handleAuditRelatedChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Workflow.class)) {
      return handleWorkflowChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Service.class)) {
      return handleServiceChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Pipeline.class)) {
      return handlePipelineChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Environment.class)) {
      return handleEnvironmentChange(changeEvent);
    }
    return true;
  }
}
