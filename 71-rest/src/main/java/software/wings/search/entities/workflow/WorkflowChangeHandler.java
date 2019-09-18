package software.wings.search.entities.workflow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.EntityCache;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowView.WorkflowViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.Optional;

@Slf4j
@Singleton
public class WorkflowChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowViewBuilder workflowViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();

  private boolean handleServiceChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = WorkflowViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String filterId = changeEvent.getUuid();
        return searchDao.updateListInMultipleDocuments(WorkflowSearchEntity.TYPE, entityType, newNameValue, filterId);
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
        Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
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
        Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
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
    return isChangeHandled;
  }
}
