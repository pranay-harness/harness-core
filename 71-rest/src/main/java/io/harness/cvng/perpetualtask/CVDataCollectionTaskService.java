package io.harness.cvng.perpetualtask;

import io.harness.cvng.beans.DataCollectionConnectorBundle;

public interface CVDataCollectionTaskService {
  String create(String accountId, DataCollectionConnectorBundle bundle);
  void delete(String accountId, String taskId);
}
