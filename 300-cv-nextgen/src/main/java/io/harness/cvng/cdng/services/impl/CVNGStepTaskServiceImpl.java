/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
    if (entity.isSkip()) {
      waitNotifyEngine.doneWith(entity.getCallbackId(), CVNGResponseData.builder().skip(true).build());
      markDone(entity.getUuid());
    } else {
      ActivityStatusDTO activityStatusDTO =
          activityService.getActivityStatus(entity.getAccountId(), entity.getActivityId());
      // send final progress even if the status is a final status.
      waitNotifyEngine.progressOn(entity.getCallbackId(),
          CVNGResponseData.builder().activityId(entity.getActivityId()).activityStatusDTO(activityStatusDTO).build());
      if (ActivityVerificationStatus.getFinalStates().contains(activityStatusDTO.getStatus())) {
        waitNotifyEngine.doneWith(entity.getCallbackId(),
            CVNGResponseData.builder().activityId(entity.getActivityId()).activityStatusDTO(activityStatusDTO).build());
        markDone(entity.getUuid());
      }
    }
  }

  @Override
  public CVNGStepTask getByCallBackId(String callBackId) {
    return hPersistence.createQuery(CVNGStepTask.class).filter(CVNGStepTaskKeys.callbackId, callBackId).get();
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
