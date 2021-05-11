package io.harness.cvng.state;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.state.CVNGVerificationTask.CVNGVerificationTaskKeys;
import io.harness.cvng.state.CVNGVerificationTask.Status;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(CV)
public class CVNGVerificationTaskServiceImpl implements CVNGVerificationTaskService {
  @Inject private HPersistence hPersistence;
  @Override
  public String create(CVNGVerificationTask cvngVerificationTask) {
    hPersistence.save(cvngVerificationTask);
    return cvngVerificationTask.getUuid();
  }

  @Override
  public void markDone(String cvngVerificationTaskId) {
    updateStatus(cvngVerificationTaskId, Status.DONE);
  }

  @Override
  public void markTimedOut(String cvngVerificationTaskId) {
    updateStatus(cvngVerificationTaskId, Status.TIMED_OUT);
  }

  private void updateStatus(String cvngVerificationTaskId, Status status) {
    UpdateOperations<CVNGVerificationTask> updateOperations =
        hPersistence.createUpdateOperations(CVNGVerificationTask.class).set(CVNGVerificationTaskKeys.status, status);
    Query<CVNGVerificationTask> query = hPersistence.createQuery(CVNGVerificationTask.class)
                                            .filter(CVNGVerificationTaskKeys.uuid, cvngVerificationTaskId);
    hPersistence.update(query, updateOperations);
  }
  @Override
  public CVNGVerificationTask get(String cvngVerificationTaskId) {
    return hPersistence.get(CVNGVerificationTask.class, cvngVerificationTaskId);
  }

  @Override
  public CVNGVerificationTask getByActivityId(String activityId) {
    return hPersistence.createQuery(CVNGVerificationTask.class)
        .filter(CVNGVerificationTaskKeys.activityId, activityId)
        .get();
  }
}
