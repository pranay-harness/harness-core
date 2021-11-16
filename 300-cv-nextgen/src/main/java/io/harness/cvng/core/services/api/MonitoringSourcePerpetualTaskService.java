package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.encryption.Scope;

import java.util.List;

public interface MonitoringSourcePerpetualTaskService extends DeleteEntityByHandler<MonitoringSourcePerpetualTask> {
  void createTask(String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier,
      String monitoringSourceIdentifier, boolean isDemo);
  void deleteTask(String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier);
  List<MonitoringSourcePerpetualTask> listByConnectorIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, Scope scope);
  void createPerpetualTask(MonitoringSourcePerpetualTask monitoringSourcePerpetualTask);
  void resetLiveMonitoringPerpetualTask(MonitoringSourcePerpetualTask monitoringSourcePerpetualTask);
  String getLiveMonitoringWorkerId(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String monitoringSourceIdentifier);

  String getDeploymentWorkerId(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String monitoringSourceIdentifier);
}
