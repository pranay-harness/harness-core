/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentKeys;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.search.entities.deployment.DeploymentView.DeploymentViewKeys;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DBObject;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * The handler which will maintain the deployment document
 * in the search engine database.
 *
 * @author ujjawal
 */

@OwnedBy(PL)
@Slf4j
@Singleton
public class DeploymentChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private DeploymentViewBuilder deploymentViewBuilder;

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
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

  private boolean handleEnvironmentChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(EnvironmentKeys.name)) {
        String entityType = DeploymentViewKeys.environments;
        String newNameValue = document.get(EnvironmentKeys.name).toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = EnvironmentKeys.name;
        return searchDao.updateListInMultipleDocuments(
            DeploymentSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleServiceChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = DeploymentViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = ServiceKeys.name;
        return searchDao.updateListInMultipleDocuments(
            DeploymentSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(WorkflowKeys.name)) {
        String entityType = DeploymentViewKeys.workflows;
        String newNameValue = document.get(WorkflowKeys.name).toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = WorkflowKeys.name;
        return searchDao.updateListInMultipleDocuments(
            DeploymentSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowExecutionInsert(ChangeEvent<?> changeEvent) {
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    DeploymentView deploymentView = deploymentViewBuilder.createDeploymentView(workflowExecution);
    Optional<String> jsonString = SearchEntityUtils.convertToJson(deploymentView);
    if (deploymentView != null) {
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(DeploymentSearchEntity.TYPE, deploymentView.getId(), jsonString.get());
      }
      return false;
    }
    return true;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    DBObject changeDocument = changeEvent.getChanges();
    DeploymentView deploymentView = deploymentViewBuilder.createDeploymentView(workflowExecution, changeDocument);
    Optional<String> jsonString = SearchEntityUtils.convertToJson(deploymentView);
    if (deploymentView != null) {
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(DeploymentSearchEntity.TYPE, deploymentView.getId(), jsonString.get());
      }
      return false;
    }
    return true;
  }

  private boolean handleWorkflowExecutionChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowExecutionInsert(changeEvent);
      case UPDATE:
        return handleWorkflowExecutionUpdate(changeEvent);
      case DELETE:
        return searchDao.deleteDocument(DeploymentSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(WorkflowExecution.class)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Application.class)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Environment.class)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Service.class)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Workflow.class)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    return isChangeHandled;
  }
}
