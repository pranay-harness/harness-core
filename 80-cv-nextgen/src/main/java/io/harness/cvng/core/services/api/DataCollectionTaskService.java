package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;

import java.util.Optional;

public interface DataCollectionTaskService {
  void save(DataCollectionTask dataCollectionTask);
  Optional<DataCollectionTask> getNextTask(String accountId, String cvConfigId);
  Optional<DataCollectionTaskDTO> getNextTaskDTO(String accountId, String cvConfigId);
  DataCollectionTask getDataCollectionTask(String dataCollectionTaskId);
  void updateTaskStatus(DataCollectionTaskResult dataCollectionTaskResult);
  String enqueueFirstTask(CVConfig cvConfig);
}
