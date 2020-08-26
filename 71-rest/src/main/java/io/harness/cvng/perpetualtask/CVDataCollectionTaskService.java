package io.harness.cvng.perpetualtask;

import io.harness.cvng.beans.DataCollectionConnectorBundle;

public interface CVDataCollectionTaskService {
  String create(String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle);
  void delete(String accountId, String taskId);
}
