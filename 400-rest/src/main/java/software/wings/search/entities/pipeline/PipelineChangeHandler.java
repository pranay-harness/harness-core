/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;
import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.search.entities.pipeline.PipelineView.PipelineViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.workflow.WorkflowView.WorkflowViewKeys;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * The handler which will maintain the pipeline
 * document in the search engine database.
 *
 * @author ujjawal
 */

@OwnedBy(PL)
@Slf4j
@Singleton
public class PipelineChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private PipelineViewBuilder pipelineViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleAuditRelatedChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE && changeEvent.getChanges() != null
        && changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
      boolean result = true;
      AuditHeader auditHeader = (AuditHeader) changeEvent.getFullDocument();
      Map<String, Boolean> isAffectedResourceHandled = new HashMap<>();
      for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
        if (entityAuditRecord.getAffectedResourceType() != null
            && entityAuditRecord.getAffectedResourceType().equals(EntityType.PIPELINE.name())
            && entityAuditRecord.getAffectedResourceId() != null
            && !entityAuditRecord.getAffectedResourceOperation().equals(Type.DELETE.name())
            && !isAffectedResourceHandled.containsKey(entityAuditRecord.getAffectedResourceId())) {
          String fieldToUpdate = PipelineViewKeys.audits;
          String documentToUpdate = entityAuditRecord.getAffectedResourceId();
          String auditTimestampField = PipelineViewKeys.auditTimestamps;
          Map<String, Object> auditRelatedEntityViewMap =
              relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
          result &= searchDao.addTimestamp(PipelineSearchEntity.TYPE, auditTimestampField, documentToUpdate,
              auditHeader.getCreatedAt(), DAYS_TO_RETAIN);
          result &= searchDao.appendToListInSingleDocument(PipelineSearchEntity.TYPE, fieldToUpdate, documentToUpdate,
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
    if (workflowExecution.getWorkflowType() == WorkflowType.PIPELINE) {
      String documentToUpdate = workflowExecution.getWorkflowId();
      String fieldToUpdate = WorkflowViewKeys.deployments;
      Map<String, Object> deploymentRelatedEntityViewMap =
          relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
      String deploymentTimestampsField = PipelineViewKeys.deploymentTimestamps;
      result = searchDao.addTimestamp(PipelineSearchEntity.TYPE, deploymentTimestampsField, documentToUpdate,
          workflowExecution.getCreatedAt(), DAYS_TO_RETAIN);
      result = result
          && searchDao.appendToListInSingleDocument(PipelineSearchEntity.TYPE, fieldToUpdate, documentToUpdate,
              deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
    }
    return result;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    if (workflowExecution.getWorkflowType() == WorkflowType.PIPELINE) {
      DBObject changes = changeEvent.getChanges();
      if (changes.containsField(WorkflowExecutionKeys.status)) {
        String type = WorkflowViewKeys.deployments;
        String newNameValue = workflowExecution.getStatus().toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
        return searchDao.updateListInMultipleDocuments(
            PipelineSearchEntity.TYPE, type, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowExecution(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowExecutionInsert(changeEvent);
      case UPDATE:
        return handleWorkflowExecutionUpdate(changeEvent);
      default:
        return true;
    }
  }

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
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

  private boolean handleServiceChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(ServiceKeys.name)) {
        String entityType = PipelineViewKeys.services;
        String newNameValue = document.get(ServiceKeys.name).toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = ServiceKeys.name;
        return searchDao.updateListInMultipleDocuments(
            PipelineSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handleWorkflowChange(ChangeEvent<?> changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE) {
      DBObject document = changeEvent.getChanges();
      if (document.containsField(WorkflowKeys.name)) {
        String entityType = PipelineViewKeys.workflows;
        String newNameValue = document.get(WorkflowKeys.name).toString();
        String documentToUpdate = changeEvent.getUuid();
        String fieldToUpdate = WorkflowKeys.name;
        return searchDao.updateListInMultipleDocuments(
            PipelineSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
      }
    }
    return true;
  }

  private boolean handlePipelineInsert(ChangeEvent<?> changeEvent) {
    Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
    PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline);
    if (pipelineView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
      }
      return false;
    }
    return true;
  }

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    DBObject changeDocument = changeEvent.getChanges();
    Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
    PipelineView pipelineView = pipelineViewBuilder.createPipelineView(pipeline, changeDocument);
    if (pipelineView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(pipelineView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(PipelineSearchEntity.TYPE, pipelineView.getId(), jsonString.get());
      }
      return false;
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
        return searchDao.deleteDocument(PipelineSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(Pipeline.class)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Application.class)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Workflow.class)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Service.class)) {
      isChangeHandled = handleServiceChange(changeEvent);
    }
    if (changeEvent.isChangeFor(WorkflowExecution.class)) {
      isChangeHandled = handleWorkflowExecution(changeEvent);
    }
    if (changeEvent.isChangeFor(AuditHeader.class)) {
      isChangeHandled = handleAuditRelatedChange(changeEvent);
    }
    return isChangeHandled;
  }
}
