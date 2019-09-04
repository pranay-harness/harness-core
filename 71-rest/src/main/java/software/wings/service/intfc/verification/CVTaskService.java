package software.wings.service.intfc.verification;

import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.verification.CVTask;

import java.util.List;
import java.util.Optional;

public interface CVTaskService {
  CVTask enqueueTask(String accountId, String cvConfigId, long startMilliSec, long endMilliSec);

  void saveCVTask(CVTask cvTask);

  void enqueueSequentialTasks(List<CVTask> cvTasks);

  Optional<CVTask> getNextTask(String accountId);

  CVTask getCVTask(String cvTaskId);

  void updateTaskStatus(String cvTaskId, DataCollectionTaskResult result);

  void expireLongRunningTasks(String accountId);

  void retryTasks(String accountId);
}
