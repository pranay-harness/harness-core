package software.wings.delegatetasks;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.DataCollectionTaskResult;

import java.util.concurrent.TimeoutException;
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface DelegateCVTaskService {
  void updateCVTaskStatus(String accountId, String cvTaskId, DataCollectionTaskResult dataCollectionTaskResult)
      throws TimeoutException;
}
