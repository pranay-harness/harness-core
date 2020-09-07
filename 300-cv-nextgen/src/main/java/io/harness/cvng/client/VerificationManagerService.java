package io.harness.cvng.client;

import java.util.List;

public interface VerificationManagerService {
  String createServiceGuardPerpetualTask(String accountId, String cvConfigId, String connectorIdentifier,
      String orgIdentifier, String projectIdentifier, String dataCollectionWorkerId);
  String createDeploymentVerificationPerpetualTask(String accountId, String connectorIdentifier, String orgIdentifier,
      String projectIdentifier, String dataCollectionWorkerId);
  void deletePerpetualTask(String accountId, String perpetualTaskId);

  void deletePerpetualTasks(String accountId, List<String> perpetualTaskIds);
}
