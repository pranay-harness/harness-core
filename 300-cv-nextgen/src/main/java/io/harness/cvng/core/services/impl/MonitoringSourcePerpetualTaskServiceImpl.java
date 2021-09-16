package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask.MonitoringSourcePerpetualTaskKeys;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask.VerificationType;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.encryption.Scope;
import io.harness.persistence.HPersistence;

import com.google.api.client.util.Charsets;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class MonitoringSourcePerpetualTaskServiceImpl
    implements MonitoringSourcePerpetualTaskService, DeleteEntityByHandler<MonitoringSourcePerpetualTask> {
  private static final String WORKER_ID_SEPARATOR = ":";
  @Inject private HPersistence hPersistence;
  @Inject private VerificationManagerService verificationManagerService;

  @Override
  public void createPerpetualTask(MonitoringSourcePerpetualTask monitoringSourcePerpetualTask) {
    String dataCollectionWorkerId = getWorkerId(monitoringSourcePerpetualTask);
    String perpetualTaskId =
        verificationManagerService.createDataCollectionTask(monitoringSourcePerpetualTask.getAccountId(),
            monitoringSourcePerpetualTask.getOrgIdentifier(), monitoringSourcePerpetualTask.getProjectIdentifier(),
            DataCollectionConnectorBundle.builder()
                .sourceIdentifier(monitoringSourcePerpetualTask.getMonitoringSourceIdentifier())
                .connectorIdentifier(monitoringSourcePerpetualTask.getConnectorIdentifier())
                .dataCollectionWorkerId(dataCollectionWorkerId)
                .dataCollectionType(DataCollectionType.CV)
                .build());
    setCollectionTaskIds(monitoringSourcePerpetualTask.getUuid(), perpetualTaskId, dataCollectionWorkerId);
  }

  @Override
  public void createTask(String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier,
      String monitoringSourceIdentifier) {
    hPersistence.saveIgnoringDuplicateKeys(
        Lists.newArrayList(MonitoringSourcePerpetualTask.builder()
                               .accountId(accountId)
                               .orgIdentifier(orgIdentifier)
                               .projectIdentifier(projectIdentifier)
                               .connectorIdentifier(connectorIdentifier)
                               .monitoringSourceIdentifier(monitoringSourceIdentifier)
                               .verificationType(VerificationType.LIVE_MONITORING)
                               .build(),
            MonitoringSourcePerpetualTask.builder()
                .accountId(accountId)
                .orgIdentifier(orgIdentifier)
                .projectIdentifier(projectIdentifier)
                .connectorIdentifier(connectorIdentifier)
                .monitoringSourceIdentifier(monitoringSourceIdentifier)
                .verificationType(VerificationType.DEPLOYMENT)
                .build()));
  }

  @Override
  public void deleteTask(
      String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier) {
    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTasks =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class, excludeAuthority)
            .filter(MonitoringSourcePerpetualTaskKeys.accountId, accountId)
            .filter(MonitoringSourcePerpetualTaskKeys.orgIdentifier, orgIdentifier)
            .filter(MonitoringSourcePerpetualTaskKeys.projectIdentifier, projectIdentifier)
            .filter(MonitoringSourcePerpetualTaskKeys.monitoringSourceIdentifier, monitoringSourceIdentifier)
            .asList();

    monitoringSourcePerpetualTasks.forEach(monitoringSourcePerpetualTask -> {
      if (isNotEmpty(monitoringSourcePerpetualTask.getPerpetualTaskId())) {
        deletePerpetualTasks(accountId, monitoringSourcePerpetualTask.getPerpetualTaskId());
      }
      hPersistence.delete(monitoringSourcePerpetualTask);
    });
  }

  @Override
  public List<MonitoringSourcePerpetualTask> listByConnectorIdentifier(String accountId, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierWithoutScopePrefix, Scope scope) {
    String connectorIdentifier = connectorIdentifierWithoutScopePrefix;
    if (scope == Scope.ACCOUNT || scope == Scope.ORG) {
      connectorIdentifier = scope.getYamlRepresentation() + "." + connectorIdentifierWithoutScopePrefix;
    }
    Query<MonitoringSourcePerpetualTask> query =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class, excludeAuthority)
            .filter(MonitoringSourcePerpetualTaskKeys.accountId, accountId)
            .filter(MonitoringSourcePerpetualTaskKeys.connectorIdentifier, connectorIdentifier);
    if (scope == Scope.ORG) {
      query = query.filter(MonitoringSourcePerpetualTaskKeys.orgIdentifier, orgIdentifier);
    }
    if (scope == Scope.PROJECT) {
      query = query.filter(MonitoringSourcePerpetualTaskKeys.projectIdentifier, projectIdentifier);
    }
    return query.asList();
  }

  @Override
  public void resetLiveMonitoringPerpetualTask(MonitoringSourcePerpetualTask monitoringSourcePerpetualTask) {
    if (isEmpty(monitoringSourcePerpetualTask.getPerpetualTaskId())) {
      return;
    }
    verificationManagerService.resetDataCollectionTask(monitoringSourcePerpetualTask.getAccountId(),
        monitoringSourcePerpetualTask.getOrgIdentifier(), monitoringSourcePerpetualTask.getProjectIdentifier(),
        monitoringSourcePerpetualTask.getPerpetualTaskId(),
        DataCollectionConnectorBundle.builder()
            .connectorIdentifier(monitoringSourcePerpetualTask.getConnectorIdentifier())
            .sourceIdentifier(monitoringSourcePerpetualTask.getMonitoringSourceIdentifier())
            .dataCollectionType(DataCollectionType.CV)
            .build());
  }

  @Override
  public String getLiveMonitoringWorkerId(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String monitoringSourceIdentifier) {
    return getWorkerId(accountId, orgIdentifier, projectIdentifier, connectorIdentifier, monitoringSourceIdentifier,
        VerificationType.LIVE_MONITORING);
  }

  @Override
  public String getDeploymentWorkerId(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String monitoringSourceIdentifier) {
    return getWorkerId(accountId, orgIdentifier, projectIdentifier, connectorIdentifier, monitoringSourceIdentifier,
        VerificationType.DEPLOYMENT);
  }

  private String getWorkerId(MonitoringSourcePerpetualTask monitoringSourcePerpetualTask) {
    return getWorkerId(monitoringSourcePerpetualTask.getAccountId(), monitoringSourcePerpetualTask.getOrgIdentifier(),
        monitoringSourcePerpetualTask.getProjectIdentifier(), monitoringSourcePerpetualTask.getConnectorIdentifier(),
        monitoringSourcePerpetualTask.getMonitoringSourceIdentifier(),
        monitoringSourcePerpetualTask.getVerificationType());
  }
  private String getWorkerId(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String monitoringSourceIdentifier, VerificationType verificationType) {
    StringJoiner stringJoiner = new StringJoiner(WORKER_ID_SEPARATOR)
                                    .add(accountId)
                                    .add(orgIdentifier)
                                    .add(projectIdentifier)
                                    .add(monitoringSourceIdentifier)
                                    .add(connectorIdentifier)
                                    .add(verificationType.name());
    return UUID.nameUUIDFromBytes(stringJoiner.toString().getBytes(Charsets.UTF_8)).toString();
  }
  private void setCollectionTaskIds(
      String monitoringSourceDataCollectionTaskId, String perpetualTaskId, String dataCollectionWorkerId) {
    UpdateOperations<MonitoringSourcePerpetualTask> updateOperations =
        hPersistence.createUpdateOperations(MonitoringSourcePerpetualTask.class)
            .set(MonitoringSourcePerpetualTaskKeys.perpetualTaskId, perpetualTaskId)
            .set(MonitoringSourcePerpetualTaskKeys.dataCollectionWorkerId, dataCollectionWorkerId);
    Query<MonitoringSourcePerpetualTask> query =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class, excludeAuthority)
            .filter(MonitoringSourcePerpetualTaskKeys.uuid, monitoringSourceDataCollectionTaskId);
    hPersistence.update(query, updateOperations);
  }

  @Override
  public void deleteByProjectIdentifier(
      Class<MonitoringSourcePerpetualTask> clazz, String accountId, String orgIdentifier, String projectIdentifier) {
    Query<MonitoringSourcePerpetualTask> query =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class, excludeAuthority)
            .filter(MonitoringSourcePerpetualTaskKeys.accountId, accountId);
    if (isNotEmpty(orgIdentifier)) {
      query.filter(MonitoringSourcePerpetualTaskKeys.orgIdentifier, orgIdentifier);
    }

    if (isNotEmpty(projectIdentifier)) {
      query.filter(MonitoringSourcePerpetualTaskKeys.projectIdentifier, projectIdentifier);
    }

    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTasks = query.asList();
    if (isEmpty(monitoringSourcePerpetualTasks)) {
      return;
    }

    monitoringSourcePerpetualTasks.forEach(monitoringSourcePerpetualTask
        -> deleteTask(monitoringSourcePerpetualTask.getAccountId(), monitoringSourcePerpetualTask.getOrgIdentifier(),
            monitoringSourcePerpetualTask.getProjectIdentifier(),
            monitoringSourcePerpetualTask.getMonitoringSourceIdentifier()));
  }

  @Override
  public void deleteByOrgIdentifier(
      Class<MonitoringSourcePerpetualTask> clazz, String accountId, String orgIdentifier) {
    deleteByProjectIdentifier(clazz, accountId, orgIdentifier, null);
  }

  @Override
  public void deleteByAccountIdentifier(Class<MonitoringSourcePerpetualTask> clazz, String accountId) {
    deleteByProjectIdentifier(clazz, accountId, null, null);
  }

  private void deletePerpetualTasks(String accountId, String perpetualTaskId) {
    verificationManagerService.deletePerpetualTask(accountId, perpetualTaskId);
  }
}
