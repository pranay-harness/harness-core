package io.harness.cvng.cdng.services.impl;

import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.entities.CVNGStepTask.CVNGStepTaskKeys;
import io.harness.cvng.cdng.entities.CVNGStepTask.Status;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.cdng.services.impl.CVNGStep.CVNGResponseData;
import io.harness.persistence.HPersistence;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class CVNGStepTaskServiceImpl implements CVNGStepTaskService {
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Override
  public void create(CVNGStepTask cvngStepTask) {
    cvngStepTask.validate();
    hPersistence.save(cvngStepTask);
  }

  @Override
  public void notifyCVNGStep(CVNGStepTask entity) {
    ActivityStatusDTO activityStatusDTO =
        activityService.getActivityStatus(entity.getAccountId(), entity.getActivityId());
    waitNotifyEngine.doneWith(entity.getActivityId(),
        CVNGResponseData.builder().activityId(entity.getActivityId()).activityStatusDTO(activityStatusDTO).build());
    if (ActivityVerificationStatus.getFinalStates().contains(activityStatusDTO.getStatus())) {
      markDone(entity.getUuid());
    }
  }

  private void markDone(String uuid) {
    updateStatus(uuid, Status.DONE);
  }

  private void updateStatus(String cvngStepTaskId, Status status) {
    UpdateOperations<CVNGStepTask> updateOperations =
        hPersistence.createUpdateOperations(CVNGStepTask.class).set(CVNGStepTaskKeys.status, status);
    Query<CVNGStepTask> query =
        hPersistence.createQuery(CVNGStepTask.class).filter(CVNGStepTaskKeys.uuid, cvngStepTaskId);
    hPersistence.update(query, updateOperations);
  }
}
