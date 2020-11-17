package io.harness.cvng.client;

import io.harness.cvng.beans.DataCollectionType;

import java.util.List;
import java.util.Map;

public interface VerificationManagerService {
  String createDataCollectionTask(String accountId, String orgIdentifier, String projectIdentifier,
      DataCollectionType dataCollectionType, Map<String, String> params);
  void deletePerpetualTask(String accountId, String perpetualTaskId);
  void deletePerpetualTasks(String accountId, List<String> perpetualTaskIds);
  List<String> getKubernetesNamespaces(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
  List<String> getKubernetesWorkloads(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String namespace);
}
