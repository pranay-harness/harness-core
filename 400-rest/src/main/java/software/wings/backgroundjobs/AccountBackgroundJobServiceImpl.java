/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.backgroundjobs;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.scheduler.PersistentScheduler;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;

@OwnedBy(PL)
@Slf4j
public class AccountBackgroundJobServiceImpl implements AccountBackgroundJobService {
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler persistentScheduler;
  @Inject private AccountService accountService;
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public void manageBackgroundJobsForAccount(String accountId) {
    resumeAllPerpetualTasksForAccount(accountId);
    resumeAllQuartzJobsForAccount(accountId);
    accountService.updateBackgroundJobsDisabled(accountId, false);
  }

  private void resumeAllQuartzJobsForAccount(String accountId) {
    log.info("Resuming all Quartz jobs for account {}", accountId);
    try {
      persistentScheduler.resumeAllQuartzJobsForAccount(accountId);
    } catch (SchedulerException ex) {
      log.error("Exception occurred while resuming Quartz jobs for account {}", accountId, ex);
    }
  }

  /**
   * Resumes all the paused perpetual tasks for a given account
   * @param accountId
   */
  private void resumeAllPerpetualTasksForAccount(String accountId) {
    List<PerpetualTaskRecord> perpetualTasksList = perpetualTaskService.listAllTasksForAccount(accountId);
    log.info("Resuming all perpetual tasks for account {}", accountId);
    for (PerpetualTaskRecord perpetualTask : perpetualTasksList) {
      if (perpetualTask.getState() == PerpetualTaskState.TASK_PAUSED) {
        perpetualTaskService.resumeTask(accountId, perpetualTask.getUuid());
      }
    }
  }
}
