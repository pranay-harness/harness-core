/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.environment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
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
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.search.entities.environment.EnvironmentView.EnvironmentViewKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class EnvironmentChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private EnvironmentViewBuilder environmentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleWorkflowInsert(ChangeEvent<?> changeEvent) {
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    if (workflow.getEnvId() != null) {
      String fieldToUpdate = EnvironmentViewKeys.workflows;
      String documentToUpdate = workflow.getEnvId();
      EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);
      return searchDao.appendToListInSingleDocument(
          EnvironmentSearchEntity.TYPE, fieldToUpdate, documentToUpdate, newElement);
    }
    return true;
  }

  private boolean handleWorkflowUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(WorkflowKeys.envId)) {
      Workflow workflow = (Workflow) changeEvent.getFullDocument();
      String fieldToUpdate = EnvironmentViewKeys.workflows;
      Set<String> currentEnvIds = Sets.newHashSet(
          searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.workflows, changeEvent.getUuid()));
      Set<String> incomingEnvIds = Sets.newHashSet(Arrays.asList(workflow.getEnvId()));

      List<String> toBeAddedEnvIds =
          new ArrayList<>(Sets.difference(incomingEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));
      List<String> toBeDeletedEnvIds =
          new ArrayList<>(Sets.difference(currentEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));

      EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (EmptyPredicate.isNotEmpty(toBeAddedEnvIds)) {
        result = searchDao.appendToListInMultipleDocuments(
            EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeAddedEnvIds, newElement);
      }
      if (EmptyPredicate.isNotEmpty(toBeDeletedEnvIds)) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedEnvIds, changeEvent.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = EnvironmentViewKeys.workflows;
      String newValue = document.get(WorkflowKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = WorkflowKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
              EnvironmentSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handleWorkflowDelete(ChangeEvent<?> changeEvent) {
    String fieldToUpdate = EnvironmentViewKeys.workflows;
    List<String> toBeDeletedEnvIds =
        searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.workflows, changeEvent.getUuid());
    return searchDao.removeFromListInMultipleDocuments(
        EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedEnvIds, changeEvent.getUuid());
  }

  private boolean handleWorkflowChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowInsert(changeEvent);
      case UPDATE:
        return handleWorkflowUpdate(changeEvent);
      case DELETE:
        return handleWorkflowDelete(changeEvent);
      default:
        return true;
    }
  }

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(PipelineKeys.pipelineStages)) {
      Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
      String fieldToUpdate = EnvironmentViewKeys.pipelines;
      Set<String> incomingEnvIds = Sets.newHashSet(environmentViewBuilder.populateEnvIds(pipeline));
      Set<String> currentEnvIds = Sets.newHashSet(
          searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.pipelines, pipeline.getUuid()));
      List<String> toBeAddedEnvIds =
          new ArrayList<>(Sets.difference(incomingEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));
      List<String> toBeDeletedEnvIds =
          new ArrayList<>(Sets.difference(currentEnvIds, Sets.intersection(incomingEnvIds, currentEnvIds)));
      EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (!toBeAddedEnvIds.isEmpty()) {
        result = searchDao.appendToListInMultipleDocuments(
            EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeAddedEnvIds, newElement);
      }
      if (!toBeDeletedEnvIds.isEmpty()) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedEnvIds, pipeline.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(PipelineKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = EnvironmentViewKeys.pipelines;
      String newValue = document.get(PipelineKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = PipelineKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
              EnvironmentSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handlePipelineDelete(ChangeEvent<?> changeEvent) {
    String fieldToUpdate = EnvironmentViewKeys.pipelines;
    List<String> toBeDeletedPipelineIds =
        searchDao.nestedQuery(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.pipelines, changeEvent.getUuid());
    return searchDao.removeFromListInMultipleDocuments(
        EnvironmentSearchEntity.TYPE, fieldToUpdate, toBeDeletedPipelineIds, changeEvent.getUuid());
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

  private boolean handleWorkflowExecutionInsert(ChangeEvent<?> changeEvent) {
    boolean result = true;
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    if (workflowExecution.getEnvIds() != null) {
      String fieldToUpdate = EnvironmentViewKeys.deployments;
      List<String> documentsToUpdate = workflowExecution.getEnvIds();
      Map<String, Object> deploymentRelatedEntityViewMap =
          relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
      String deploymentTimestampsField = EnvironmentViewKeys.deploymentTimestamps;
      result = searchDao.addTimestamp(EnvironmentSearchEntity.TYPE, deploymentTimestampsField, documentsToUpdate,
          workflowExecution.getCreatedAt(), DAYS_TO_RETAIN);
      result = result
          && searchDao.appendToListInMultipleDocuments(EnvironmentSearchEntity.TYPE, fieldToUpdate, documentsToUpdate,
              deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
    }
    return result;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    DBObject changes = changeEvent.getChanges();
    if (changes.containsField(WorkflowExecutionKeys.status)) {
      String entityType = EnvironmentViewKeys.deployments;
      String newNameValue = workflowExecution.getStatus().toString();
      String documentToUpdate = workflowExecution.getUuid();
      String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
      return searchDao.updateListInMultipleDocuments(
          EnvironmentSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
    }
    return true;
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

  private boolean handleAuditRelatedChange(ChangeEvent changeEvent) {
    if (changeEvent.getChangeType() == ChangeType.UPDATE && changeEvent.getChanges() != null
        && changeEvent.getChanges().containsField(AuditHeaderKeys.entityAuditRecords)) {
      boolean result = true;
      AuditHeader auditHeader = (AuditHeader) changeEvent.getFullDocument();
      Map<String, Boolean> isAffectedResourceHandled = new HashMap<>();
      for (EntityAuditRecord entityAuditRecord : auditHeader.getEntityAuditRecords()) {
        if (entityAuditRecord.getAffectedResourceType() != null
            && entityAuditRecord.getAffectedResourceType().equals(EntityType.ENVIRONMENT.name())
            && entityAuditRecord.getAffectedResourceId() != null
            && !entityAuditRecord.getAffectedResourceOperation().equals(Type.DELETE.name())
            && !isAffectedResourceHandled.containsKey(entityAuditRecord.getAffectedResourceId())) {
          String fieldToUpdate = EnvironmentViewKeys.audits;
          String documentToUpdate = entityAuditRecord.getAffectedResourceId();
          String auditTimestampField = EnvironmentViewKeys.auditTimestamps;
          Map<String, Object> auditRelatedEntityViewMap =
              relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
          result = result
              && searchDao.addTimestamp(EnvironmentSearchEntity.TYPE, auditTimestampField, documentToUpdate,
                  auditHeader.getCreatedAt(), DAYS_TO_RETAIN);
          result = result
              && searchDao.appendToListInSingleDocument(EnvironmentSearchEntity.TYPE, fieldToUpdate, documentToUpdate,
                  auditRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
          isAffectedResourceHandled.put(entityAuditRecord.getAffectedResourceId(), true);
        }
      }
      return result;
    }
    return true;
  }

  private boolean handleApplicationChange(ChangeEvent<?> changeEvent) {
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

  private boolean handleEnvironmentInsert(ChangeEvent<?> changeEvent) {
    Environment environment = (Environment) changeEvent.getFullDocument();
    EnvironmentView environmentView = environmentViewBuilder.createEnvironmentView(environment);
    if (environmentView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(environmentView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(EnvironmentSearchEntity.TYPE, environmentView.getId(), jsonString.get());
      }
      return false;
    }
    return true;
  }

  private boolean handleEnvironmentUpdate(ChangeEvent<?> changeEvent) {
    Environment environment = (Environment) changeEvent.getFullDocument();
    EnvironmentView environmentView =
        environmentViewBuilder.createEnvironmentView(environment, changeEvent.getChanges());
    if (environmentView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(environmentView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(EnvironmentSearchEntity.TYPE, environmentView.getId(), jsonString.get());
      }
      return false;
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
        return searchDao.deleteDocument(EnvironmentSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(Environment.class)) {
      isChangeHandled = handleEnvironmentChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Application.class)) {
      isChangeHandled = handleApplicationChange(changeEvent);
    }
    if (changeEvent.isChangeFor(AuditHeader.class)) {
      isChangeHandled = handleAuditRelatedChange(changeEvent);
    }
    if (changeEvent.isChangeFor(WorkflowExecution.class)) {
      isChangeHandled = handleWorkflowExecutionChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Pipeline.class)) {
      isChangeHandled = handlePipelineChange(changeEvent);
    }
    if (changeEvent.isChangeFor(Workflow.class)) {
      isChangeHandled = handleWorkflowChange(changeEvent);
    }

    return isChangeHandled;
  }
}
