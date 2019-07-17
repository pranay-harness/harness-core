package software.wings.service.intfc.verification;

import software.wings.verification.CVTask;

import java.util.Optional;

public interface CVTaskService {
  Optional<CVTask> getLastCVTask(String accountId, String cvConfigId);

  CVTask enqueueTask(String accountId, String cvConfigId, long startMilliSec, long endMilliSec);
}
