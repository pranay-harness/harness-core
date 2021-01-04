package io.harness.cvng.perpetualtask;

import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionRequest;

import io.kubernetes.client.openapi.ApiException;
import java.util.List;

public interface CVDataCollectionTaskService {
  void resetTask(String accountId, String orgIdentifier, String projectIdentifier, String taskId,
      DataCollectionConnectorBundle bundle);

  String create(String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle);
  void delete(String accountId, String taskId);

  String getDataCollectionResult(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionRequest dataCollectionRequest);

  List<String> getNamespaces(String accountId, String orgIdentifier, String projectIdentifier, String filter,
      DataCollectionConnectorBundle bundle) throws ApiException;
  List<String> getWorkloads(String accountId, String orgIdentifier, String projectIdentifier, String namespace,
      String filter, DataCollectionConnectorBundle bundle) throws ApiException;
}
