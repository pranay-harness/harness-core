package software.wings.backgroundjobs;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.SchedulerException;
import software.wings.beans.AccountStatus;
import software.wings.processingcontrollers.DelegateProcessingController;
import software.wings.scheduler.DelegateDeletionJob;
import software.wings.service.intfc.AccountService;

import java.util.List;

@Slf4j
public class AccountBackgroundJobServiceImpl implements AccountBackgroundJobService {
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler persistentScheduler;
  @Inject private AccountService accountService;
  @Inject private DelegateProcessingController delegateProcessingController;
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public void manageBackgroundJobsForAccount(String accountId) {
    String accountStatus = accountService.getAccountStatus(accountId);
    if (!AccountStatus.DELETED.equals(accountStatus)) {
      managePerpetualTasks(accountId);
      manageQuartzJobs(accountId);
      manageDelegates(accountId);
    }
  }

  private void managePerpetualTasks(String accountId) {
    if (isAccountDisabled(accountId)) {
      pauseAllPerpetualTasksForAccount(accountId);
    } else {
      resumeAllPerpetualTasksForAccount(accountId);
    }
  }

  private void manageQuartzJobs(String accountId) {
    try {
      if (isAccountDisabled(accountId)) {
        logger.info("Pausing all Quartz jobs for account {}", accountId);
        persistentScheduler.pauseAllQuartzJobsForAccount(accountId);
      } else {
        logger.info("Resuming all Quartz jobs for account {}", accountId);
        persistentScheduler.resumeAllQuartzJobsForAccount(accountId);
      }
    } catch (SchedulerException ex) {
      logger.error("Exception occurred at manageQuartzJobs() for account {}", accountId, ex);
    }
  }

  private void manageDelegates(String accountId) {
    if (!delegateProcessingController.canProcessAccount(accountId)) {
      // Trigger Delegate Deletion Job
      DelegateDeletionJob.addWithDelay(persistentScheduler, accountId, StringUtils.EMPTY, 30);
    }
  }

  /**
   * Pauses all perpetual tasks for a given account
   * @param accountId
   */
  private void pauseAllPerpetualTasksForAccount(String accountId) {
    List<PerpetualTaskRecord> perpetualTasksList = perpetualTaskService.listAllTasksForAccount(accountId);
    logger.info("Pausing all perpetual tasks for account {}", accountId);
    for (PerpetualTaskRecord perpetualTask : perpetualTasksList) {
      if (PerpetualTaskState.TASK_PAUSED != perpetualTask.getState()) {
        perpetualTaskService.pauseTask(accountId, perpetualTask.getUuid());
      }
    }
  }

  /**
   * Resumes all the paused perpetual tasks for a given account
   * @param accountId
   */
  private void resumeAllPerpetualTasksForAccount(String accountId) {
    List<PerpetualTaskRecord> perpetualTasksList = perpetualTaskService.listAllTasksForAccount(accountId);
    logger.info("Resuming all perpetual tasks for account {}", accountId);
    for (PerpetualTaskRecord perpetualTask : perpetualTasksList) {
      if (perpetualTask.getState() == PerpetualTaskState.TASK_PAUSED) {
        perpetualTaskService.resumeTask(accountId, perpetualTask.getUuid());
      }
    }
  }

  private boolean isAccountDisabled(String accountId) {
    String accountStatus = accountService.getAccountStatus(accountId);
    return !AccountStatus.ACTIVE.equals(accountStatus);
  }
}
