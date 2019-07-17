package software.wings.service.impl.verification;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import org.mongodb.morphia.query.Sort;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.verification.CVTaskService;
import software.wings.verification.CVTask;
import software.wings.verification.CVTask.CVTaskKeys;

import java.util.Optional;

public class CVTaskServiceImpl implements CVTaskService {
  @Inject protected WingsPersistence wingsPersistence;

  @Override
  public Optional<CVTask> getLastCVTask(String accountId, String cvConfigId) {
    CVTask cvTask = wingsPersistence.createQuery(CVTask.class)
                        .filter(CVTaskKeys.accountId, accountId)
                        .filter(CVTaskKeys.cvConfigId, cvConfigId)
                        .order(Sort.descending(CVTaskKeys.endMilliSec))
                        .get();
    return Optional.ofNullable(cvTask);
  }

  @Override
  public CVTask enqueueTask(String accountId, String cvConfigId, long startMilliSec, long endMilliSec) {
    CVTask cvTask = CVTask.builder()
                        .accountId(accountId)
                        .cvConfigId(cvConfigId)
                        .startMilliSec(startMilliSec)
                        .endMilliSec(endMilliSec)
                        .status(ExecutionStatus.SUCCESS) // TODO: Change this to queued. marking this passed for the
                                                         // first release. This is a migration strategy to fill cvTasks.
                        .build();
    wingsPersistence.save(cvTask);
    return cvTask;
  }
}
