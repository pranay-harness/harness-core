package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.DataCollectionTask;

public interface DataCollectionTaskManagementService<T> {
  void handleCreateNextTask(T entity);
  void createNextTask(DataCollectionTask dataCollectionTask);
}
