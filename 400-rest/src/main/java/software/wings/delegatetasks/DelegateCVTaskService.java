package software.wings.delegatetasks;

import software.wings.service.impl.analysis.DataCollectionTaskResult;

import java.util.concurrent.TimeoutException;

public interface DelegateCVTaskService {
  void updateCVTaskStatus(String accountId, String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult)
      throws TimeoutException;
}
