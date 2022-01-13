/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.workflow;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import io.harness.data.structure.EmptyPredicate;
import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
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
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.workflow.WorkflowView.WorkflowViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * The handler which will maintain the workflow document
 * in the search engine database.
 *
 * @author ujjawal
 */

@OwnedBy(PL)
@Slf4j
@Singleton
public class WorkflowChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private WorkflowViewBuilder workflowViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(PipelineKeys.pipelineStages)) {
      Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
      String fieldToUpdate = WorkflowViewKeys.pipelines;

      Set<String> incomingWorkflowIds = Sets.newHashSet(workflowViewBuilder.populateWorkflowIds(pipeline));
      Set<String> currentWorkflowIds = Sets.newHashSet(
          searchDao.nestedQuery(WorkflowSearchEntity.TYPE, WorkflowViewKeys.pipelines, pipeline.getUuid()));

      List<String> toBeAddedWorkflowIds = new ArrayList<>(
          Sets.difference(incomingWorkflowIds, Sets.intersection(incomingWorkflowIds, currentWorkflowIds)));
      List<String> toBeDeletedWorkflowIds = new ArrayList<>(
          Sets.difference(currentWorkflowIds, Sets.intersection(incomingWorkflowIds, currentWorkflowIds)));

      EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (EmptyPredicate.isNotEmpty(toBeAddedWorkflowIds)) {
        result = searchDao.appendToListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, fieldToUpdate, toBeAddedWorkflowIds, newElement);
      }
      if (EmptyPredicate.isNotEmpty(toBeDeletedWorkflowIds)) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                WorkflowSearchEntity.TYPE, fieldToUpdate, toBeDeletedWorkflowIds, pipeline.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = WorkflowViewKeys.pipelines;
      String newValue = document.get(WorkflowKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = WorkflowKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
              WorkflowSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handlePipelineDelete(ChangeEvent<?> changeEvent) {
    String fieldToUpdate = WorkflowViewKeys.pipelines;
    List<String> toBeDeletedWorkflowIds =
        searchDao.nestedQuery(WorkflowSearchEntity.TYPE, WorkflowViewKeys.pipelines, changeEvent.getUuid());
    return searchDao.removeFromListInMultipleDocuments(
        WorkflowSearchEntity.TYPE, fieldToUpdate, toBeDeletedWorkflowIds, changeEvent.getUuid());
  }

  private boolean handlePipelineChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case UPDATE:
        return handlePipelineUpdate(changeEvent);
      case DELETE:
        return handlePipelineDelete(changeEvent);
      default:
        return true;
    }
  }

  private boolean handleAuditRelatedChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE && changeEvent.getChanges() != null
        && changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
      boolean result = true;
      AuditHeader auditHeader = (AuditHeader) changeEvent.getFullDocument();
      Map<String, Boolean> isAffectedResourceHandled = new HashMap<>();
      for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
        if (entityAuditRecord.getAffectedResourceType() != null
            && entityAuditRecord.getAffectedResourceType().equals(EntityType.WORKFLOW.name())
            && entityAuditRecord.getAffectedResourceId() != null
            && !entityAuditRecord.getAffectedResourceOperation().equals(Type.DELETE.name())
            && !isAffectedResourceHandled.containsKey(entityAuditRecord.getAffectedResourceId())) {
          String fieldToUpdate = WorkflowViewKeys.audits;
          String documentToUpdate = entityAuditRecord.getAffectedResourceId();
          String auditTimestampField = WorkflowViewKeys.auditTimestamps;
          Map<String, Object> auditRelatedEntityViewMap =
              relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
          result = result
              && searchDao.addTimestamp(WorkflowSearchEntity.TYPE, auditTimestampField, documentToUpdate,
                  auditHeader.getCreatedAt(), DAYS_TO_RETAIN);
          result = result
              && searchDao.appendToListInSingleDocument(WorkflowSearchEntity.TYPE, fieldToUpdate, documentToUpdate,
                  auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
          isAffectedResourceHandled.put(entityAuditRecord.getAffectedResourceId(), true);
        }
      }
      return result;
    }
    return true;
  }

  private boolean handleWorkflowExecutionInsert(ChangeEvent<?> changeEvent) {
    boolean result = true;
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    if (workflowExecution.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      String fieldToUpdate = WorkflowViewKeys.deployments;
      List<String> documentsToUpdate = workflowExecution.getWorkflowIds();
      Map<String, Object> deploymentRelatedEntityViewMap =
          relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
      String deploymentTimestampsField = WorkflowViewKeys.deploymentTimestamps;
      result = searchDao.addTimestamp(WorkflowSearchEntity.TYPE, deploymentTimestampsField, documentsToUpdate,
          workflowExecution.getCreatedAt(), DAYS_TO_RETAIN);

      result = result
          && searchDao.appendToListInMultipleDocuments(WorkflowSearchEntity.TYPE, fieldToUpdate, documentsToUpdate,
              deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
    }
    return result;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    DBObject changes = changeEvent.getChanges();
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    boolean result = true;
    if (workflowExecution.getWorkflowType() == WorkflowType.ORCHESTRATION) {
      if (changes.containsField(WorkflowExecutionKeys.status)) {
        String entityType = WorkflowViewKeys.deployments;
        String newNameValue = workflowExecution.getStatus().toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
        result = searchDao.updateListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
      if (changes.containsField(WorkflowExecutionKeys.pipelineExecutionId)) {
        String entityType = WorkflowViewKeys.deployments;
        String newNameValue = workflowExecution.getPipelineExecutionId();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = DeploymentRelatedEntityViewKeys.pipelineExecutionId;
        result = result
            && searchDao.updateListInMultipleDocuments(
                WorkflowSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return result;
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

  private boolean handleServiceChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = WorkflowViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = ServiceKeys.name;
        return searchDao.updateListInMultipleDocuments(
            WorkflowSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleEnvironmentChange(ChangeEvent<?> changeEvent) {
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

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
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

  private boolean handleWorkflowInsert(ChangeEvent<?> changeEvent) {
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    WorkflowView workflowView = workflowViewBuilder.createWorkflowView(workflow);
    if (workflowView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
      }
      return false;
    }
    return true;
  }

  private boolean handleWorkflowUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    WorkflowView workflowView = workflowViewBuilder.createWorkflowView(workflow, changeDocument);
    if (workflowView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(workflowView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(WorkflowSearchEntity.TYPE, workflowView.getId(), jsonString.get());
      }
      return false;
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
        return searchDao.deleteDocument(WorkflowSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(Workflow.class)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Application.class)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Service.class)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Environment.class)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.isChangeFor(WorkflowExecution.class)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
    }
    if (changeEvent.isChangeFor(AuditHeader.class)) {
      isChangeHandled = handleAuditRelatedChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Pipeline.class)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    return isChangeHandled;
  }
}
