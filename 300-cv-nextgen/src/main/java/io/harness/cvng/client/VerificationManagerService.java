package io.harness.cvng.client;

import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.DataCollectionType;

import java.util.List;
import java.util.Map;

public interface VerificationManagerService {
  String createDataCollectionTask(String accountId, String orgIdentifier, String projectIdentifier,
      DataCollectionType dataCollectionType, Map<String, String> params);
  void deletePerpetualTask(String accountId, String perpetualTaskId);
  void deletePerpetualTasks(String accountId, List<String> perpetualTaskIds);
  String getDataCollectionResponse(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionRequest request);
  List<String> getKubernetesNamespaces(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String filter);
  List<String> getKubernetesWorkloads(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String namespace, String filter);
}
