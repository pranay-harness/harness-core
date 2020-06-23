package io.harness.app.cvng.api;

import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.DataCollectionTask;
import io.harness.cvng.core.services.entities.DataCollectionTask.DataCollectionTaskResult;

import java.util.Optional;

public interface DataCollectionTaskService {
  void save(DataCollectionTask dataCollectionTask);
  Optional<DataCollectionTask> getNextTask(String accountId, String cvConfigId);
  DataCollectionTask getDataCollectionTask(String dataCollectionTaskId);
  void updateTaskStatus(DataCollectionTaskResult dataCollectionTaskResult);
  String enqueueFirstTask(CVConfig cvConfig);
}
