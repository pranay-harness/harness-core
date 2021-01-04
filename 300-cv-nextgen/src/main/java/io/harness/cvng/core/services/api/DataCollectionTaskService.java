package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;

import java.util.List;
import java.util.Optional;

public interface DataCollectionTaskService {
  void save(DataCollectionTask dataCollectionTask);
  Optional<DataCollectionTask> getNextTask(String accountId, String dataCollectionWorkerId);
  Optional<DataCollectionTaskDTO> getNextTaskDTO(String accountId, String dataCollectionWorkerId);
  DataCollectionTask getDataCollectionTask(String dataCollectionTaskId);
  void updateTaskStatus(DataCollectionTaskResult dataCollectionTaskResult);
  void deletePerpetualTasks(String accountId, String perpetualTaskId);
  String enqueueFirstTask(CVConfig cvConfig);
  void resetLiveMonitoringPerpetualTask(CVConfig cvConfig);
  List<String> createSeqTasks(List<DataCollectionTask> dataCollectionTasks);
}
