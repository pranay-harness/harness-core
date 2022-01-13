/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.service;

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
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.related.deployment.RelatedDeploymentView.DeploymentRelatedEntityViewKeys;
import software.wings.search.entities.related.deployment.RelatedDeploymentViewBuilder;
import software.wings.search.entities.service.ServiceView.ServiceViewKeys;
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

@OwnedBy(PL)
@Slf4j
@Singleton
public class ServiceChangeHandler implements ChangeHandler {
  @Inject private SearchDao searchDao;
  @Inject private ServiceViewBuilder serviceViewBuilder;
  @Inject private RelatedDeploymentViewBuilder relatedDeploymentViewBuilder;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  private static final int MAX_RUNTIME_ENTITIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  private boolean handleWorkflowInsert(ChangeEvent<?> changeEvent) {
    Workflow workflow = (Workflow) changeEvent.getFullDocument();
    String fieldToUpdate = ServiceViewKeys.workflows;
    List<String> incomingServiceIds = serviceViewBuilder.getServiceIds(workflow);

    EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
    Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

    if (incomingServiceIds != null) {
      return searchDao.appendToListInMultipleDocuments(
          ServiceSearchEntity.TYPE, fieldToUpdate, incomingServiceIds, newElement);
    }
    return true;
  }

  private boolean handleWorkflowUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(WorkflowKeys.orchestration)) {
      Workflow workflow = (Workflow) changeEvent.getFullDocument();
      String fieldToUpdate = ServiceViewKeys.workflows;

      Set<String> incomingServiceIds = Sets.newHashSet(serviceViewBuilder.getServiceIds(workflow));
      Set<String> currentServiceIds = Sets.newHashSet(
          searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.workflows, workflow.getUuid()));

      List<String> toBeAddedServiceIds = new ArrayList<>(
          Sets.difference(incomingServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));
      List<String> toBeDeletedServiceIds =
          new ArrayList<>(Sets.difference(currentServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));

      EntityInfo entityInfo = new EntityInfo(workflow.getUuid(), workflow.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (!toBeAddedServiceIds.isEmpty()) {
        result = searchDao.appendToListInMultipleDocuments(
            ServiceSearchEntity.TYPE, fieldToUpdate, toBeAddedServiceIds, newElement);
      }
      if (!toBeDeletedServiceIds.isEmpty()) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, workflow.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = ServiceViewKeys.workflows;
      String newValue = document.get(WorkflowKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = WorkflowKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
              ServiceSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handleWorkflowChange(ChangeEvent<?> changeEvent) {
    switch (changeEvent.getChangeType()) {
      case INSERT:
        return handleWorkflowInsert(changeEvent);
      case UPDATE:
        return handleWorkflowUpdate(changeEvent);
      case DELETE:
        String fieldToUpdate = ServiceViewKeys.workflows;
        List<String> toBeDeletedServiceIds =
            searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.workflows, changeEvent.getUuid());
        return searchDao.removeFromListInMultipleDocuments(
            ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, changeEvent.getUuid());
      default:
        return true;
    }
  }

  private boolean handlePipelineUpdate(ChangeEvent<?> changeEvent) {
    boolean result = true;
    if (changeEvent.getChanges().containsField(PipelineKeys.pipelineStages)) {
      Pipeline pipeline = (Pipeline) changeEvent.getFullDocument();
      String fieldToUpdate = ServiceViewKeys.pipelines;
      Set<String> incomingServiceIds = Sets.newHashSet(serviceViewBuilder.getServiceIds(pipeline));
      Set<String> currentServiceIds = Sets.newHashSet(
          searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.pipelines, pipeline.getUuid()));

      List<String> toBeAddedServiceIds = new ArrayList<>(
          Sets.difference(incomingServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));
      List<String> toBeDeletedServiceIds =
          new ArrayList<>(Sets.difference(currentServiceIds, Sets.intersection(incomingServiceIds, currentServiceIds)));

      EntityInfo entityInfo = new EntityInfo(pipeline.getUuid(), pipeline.getName());
      Map<String, Object> newElement = SearchEntityUtils.convertToMap(entityInfo);

      if (EmptyPredicate.isNotEmpty(toBeAddedServiceIds)) {
        result = searchDao.appendToListInMultipleDocuments(
            ServiceSearchEntity.TYPE, fieldToUpdate, toBeAddedServiceIds, newElement);
      }
      if (EmptyPredicate.isNotEmpty(toBeDeletedServiceIds)) {
        result = result
            && searchDao.removeFromListInMultipleDocuments(
                ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, pipeline.getUuid());
      }
    }
    if (changeEvent.getChanges().containsField(WorkflowKeys.name)) {
      DBObject document = changeEvent.getChanges();
      String entityType = ServiceViewKeys.pipelines;
      String newValue = document.get(PipelineKeys.name).toString();
      String documentToUpdate = changeEvent.getUuid();
      String fieldToUpdate = PipelineKeys.name;
      result = result
          && searchDao.updateListInMultipleDocuments(
              ServiceSearchEntity.TYPE, entityType, newValue, documentToUpdate, fieldToUpdate);
    }
    return result;
  }

  private boolean handlePipelineDelete(ChangeEvent<?> changeEvent) {
    String fieldToUpdate = ServiceViewKeys.pipelines;
    List<String> toBeDeletedServiceIds =
        searchDao.nestedQuery(ServiceSearchEntity.TYPE, ServiceViewKeys.pipelines, changeEvent.getUuid());
    return searchDao.removeFromListInMultipleDocuments(
        ServiceSearchEntity.TYPE, fieldToUpdate, toBeDeletedServiceIds, changeEvent.getUuid());
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
    String fieldToUpdate = ServiceViewKeys.deployments;
    if (workflowExecution.getServiceIds() != null) {
      List<String> documentsToUpdate = workflowExecution.getServiceIds();
      Map<String, Object> deploymentRelatedEntityViewMap =
          relatedDeploymentViewBuilder.getDeploymentRelatedEntityViewMap(workflowExecution);
      String deploymentTimestampsField = ServiceViewKeys.deploymentTimestamps;
      result = searchDao.addTimestamp(ServiceSearchEntity.TYPE, deploymentTimestampsField, documentsToUpdate,
          workflowExecution.getCreatedAt(), DAYS_TO_RETAIN);
      result = result
          && searchDao.appendToListInMultipleDocuments(ServiceSearchEntity.TYPE, fieldToUpdate, documentsToUpdate,
              deploymentRelatedEntityViewMap, MAX_RUNTIME_ENTITIES);
    }
    return result;
  }

  private boolean handleWorkflowExecutionUpdate(ChangeEvent<?> changeEvent) {
    WorkflowExecution workflowExecution = (WorkflowExecution) changeEvent.getFullDocument();
    DBObject changes = changeEvent.getChanges();
    if (changes.containsField(WorkflowExecutionKeys.status)) {
      String entityType = ServiceViewKeys.deployments;
      String newNameValue = workflowExecution.getStatus().toString();
      String documentToUpdate = workflowExecution.getUuid();
      String fieldToUpdate = DeploymentRelatedEntityViewKeys.status;
      return searchDao.updateListInMultipleDocuments(
          ServiceSearchEntity.TYPE, entityType, newNameValue, documentToUpdate, fieldToUpdate);
    }
    return true;
  }

  private boolean handleWorkflowExecutionChange(ChangeEvent changeEvent) {
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
            && entityAuditRecord.getAffectedResourceType().equals(EntityType.SERVICE.name())
            && entityAuditRecord.getAffectedResourceId() != null
            && !entityAuditRecord.getAffectedResourceOperation().equals(Type.DELETE.name())
            && !isAffectedResourceHandled.containsKey(entityAuditRecord.getAffectedResourceId())) {
          String fieldToUpdate = ServiceViewKeys.audits;
          String documentToUpdate = entityAuditRecord.getAffectedResourceId();
          String auditTimestampField = ServiceViewKeys.auditTimestamps;
          Map<String, Object> auditRelatedEntityViewMap =
              relatedAuditViewBuilder.getAuditRelatedEntityViewMap(auditHeader, entityAuditRecord);
          result = result
              && searchDao.addTimestamp(ServiceSearchEntity.TYPE, auditTimestampField, documentToUpdate,
                  auditHeader.getCreatedAt(), DAYS_TO_RETAIN);
          result = result
              && searchDao.appendToListInSingleDocument(ServiceSearchEntity.TYPE, fieldToUpdate, documentToUpdate,
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

  private boolean handleServiceInsert(ChangeEvent<?> changeEvent) {
    Service service = (Service) changeEvent.getFullDocument();
    ServiceView serviceView = serviceViewBuilder.createServiceView(service);
    if (serviceView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(serviceView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(ServiceSearchEntity.TYPE, serviceView.getId(), jsonString.get());
      }
      return false;
    }
    return true;
  }

  private boolean handleServiceUpdate(ChangeEvent<?> changeEvent) {
    Service service = (Service) changeEvent.getFullDocument();
    ServiceView serviceView = serviceViewBuilder.createServiceView(service, changeEvent.getChanges());
    if (serviceView != null) {
      Optional<String> jsonString = SearchEntityUtils.convertToJson(serviceView);
      if (jsonString.isPresent()) {
        return searchDao.upsertDocument(ServiceSearchEntity.TYPE, serviceView.getId(), jsonString.get());
      }
      return false;
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
        return searchDao.deleteDocument(ServiceSearchEntity.TYPE, changeEvent.getUuid());
      default:
        return true;
    }
  }

  @Override
  public boolean handleChange(ChangeEvent<?> changeEvent) {
    boolean isChangeHandled = true;
    if (changeEvent.isChangeFor(Service.class)) {
      isChangeHandled = handleServiceChange(changeEvent);
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
