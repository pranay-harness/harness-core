package io.harness.cvng.perpetualtask;

import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.kubernetes.client.openapi.ApiException;

import java.util.List;

public interface CVDataCollectionTaskService {
  String create(String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle);
  void delete(String accountId, String taskId);
  List<String> getNamespaces(String accountId, String orgIdentifier, String projectIdentifier,
      DataCollectionConnectorBundle bundle) throws ApiException;
  List<String> getWorkloads(String accountId, String orgIdentifier, String projectIdentifier, String namespace,
      DataCollectionConnectorBundle bundle) throws ApiException;
}
