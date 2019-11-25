package software.wings.delegatetasks;

import software.wings.service.impl.analysis.DataCollectionTaskResult;

public interface DelegateCVTaskService {
  void updateCVTaskStatus(String accountId, String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult);
}
