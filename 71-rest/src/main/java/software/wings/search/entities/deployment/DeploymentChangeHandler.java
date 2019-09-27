package software.wings.search.entities.deployment;

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
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.deployment.DeploymentView.DeploymentViewKeys;
import software.wings.search.entities.environment.EnvironmentSearchEntity;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.entities.service.ServiceSearchEntity;
import software.wings.search.entities.workflow.WorkflowSearchEntity;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.util.Optional;

@Slf4j
@Singleton
public class DeploymentChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DeploymentViewBuilder deploymentViewBuilder;
  private static final Mapper mapper = SearchEntityUtils.getMapper();
  private static final EntityCache entityCache = SearchEntityUtils.getEntityCache();

  private boolean handleApplicationChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ApplicationKeys.name)) {
        String keyToUpdate = DeploymentViewKeys.appName;
        String newValue = document.get(ApplicationKeys.name).toString();
        String filterKey = PipelineViewKeys.appId;
        String filterValue = changeEvent.getUuid();
        return searchDao.updateKeyInMultipleDocuments(
            DeploymentSearchEntity.TYPE, keyToUpdate, newValue, filterKey, filterValue);
      }
    }
    return true;
  }

  private boolean handleEnvironmentChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(EnvironmentKeys.name)) {
        String entityType = DeploymentViewKeys.environments;
        String newNameValue = document.get(EnvironmentKeys.name).toString();
        String filterId = changeEvent.getUuid();
        String fieldToUpdate = EnvironmentKeys.name;
        return searchDao.updateListInMultipleDocuments(
            DeploymentSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleServiceChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = DeploymentViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String filterId = changeEvent.getUuid();
        String fieldToUpdate = ServiceKeys.name;
        return searchDao.updateListInMultipleDocuments(
            DeploymentSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(WorkflowKeys.name)) {
        String entityType = DeploymentViewKeys.workflows;
        String newNameValue = document.get(WorkflowKeys.name).toString();
        String filterId = changeEvent.getUuid();
        String fieldToUpdate = WorkflowKeys.name;
        return searchDao.updateListInMultipleDocuments(
            DeploymentSearchEntity.TYPE, entityType, newNameValue, filterId, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowExecutionChange(ChangeEvent changeEvent) {
    AdvancedDatastore advancedDatastore = wingsPersistence.getDatastore(changeEvent.getEntityType());
    switch (changeEvent.getChangeType()) {
      case INSERT: {
        DBObject fullDocument = changeEvent.getFullDocument();
        WorkflowExecution workflowExecution = (WorkflowExecution) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
        workflowExecution.setUuid(changeEvent.getUuid());
        DeploymentView deploymentView = deploymentViewBuilder.createDeploymentView(workflowExecution);
        Optional<String> jsonString = SearchEntityUtils.convertToJson(deploymentView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(DeploymentSearchEntity.TYPE, deploymentView.getId(), jsonString.get());
        }
        return false;
      }
      case UPDATE: {
        DBObject fullDocument = changeEvent.getFullDocument();
        DBObject changeDocument = changeEvent.getChanges();
        WorkflowExecution workflowExecution = (WorkflowExecution) mapper.fromDBObject(
            advancedDatastore, changeEvent.getEntityType(), fullDocument, entityCache);
        workflowExecution.setUuid(changeEvent.getUuid());
        DeploymentView deploymentView = deploymentViewBuilder.createDeploymentView(workflowExecution, changeDocument);
        Optional<String> jsonString = SearchEntityUtils.convertToJson(deploymentView);
        if (jsonString.isPresent()) {
          return searchDao.upsertDocument(DeploymentSearchEntity.TYPE, deploymentView.getId(), jsonString.get());
        }
        return false;
      }
      case DELETE: {
        return searchDao.deleteDocument(DeploymentSearchEntity.TYPE, changeEvent.getUuid());
      }
      default: { break; }
    }
    return true;
  }

  public boolean handleChange(ChangeEvent changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.getEntityType().equals(DeploymentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ApplicationSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(EnvironmentSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(ServiceSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.getEntityType().equals(WorkflowSearchEntity.SOURCE_ENTITY_CLASS)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    return isChangeHandled;
  }
}
