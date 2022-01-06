/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.scheduler;

import io.harness.scheduler.BackgroundExecutorService;
import io.harness.scheduler.BackgroundSchedulerLocker;
import io.harness.scheduler.PersistentScheduler;

import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.beans.instance.dashboard.InstanceStatsUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.instance.licensing.InstanceUsageLimitExcessHandler;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

@Slf4j
@DisallowConcurrentExecution
public class ServiceInstanceUsageCheckerJob implements Job {
  private static final SecureRandom random = new SecureRandom();
  public static final String GROUP = "SERVICE_INSTANCE_USAGE_CHECKER_CRON_GROUP";

  private static final int SYNC_INTERVAL_IN_HOURS = 24;

  @Inject private BackgroundExecutorService executorService;
  @Inject private BackgroundSchedulerLocker persistentLocker;
  @Inject private InstanceUsageLimitExcessHandler instanceUsageLimitExcessHandler;
  @Inject private InstanceStatService instanceStatService;
  @Inject private AccountService accountService;

  public static void addWithDelay(PersistentScheduler jobScheduler, String accountId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + random.nextInt((int) TimeUnit.MINUTES.toMillis(30));
    addInternal(jobScheduler, accountId, new Date(startTime));
  }

  public static void add(PersistentScheduler jobScheduler, String accountId) {
    addInternal(jobScheduler, accountId, null);
  }

  public static void delete(PersistentScheduler jobScheduler, String accountId) {
    jobScheduler.deleteJob(accountId, GROUP);
  }

  private static void addInternal(PersistentScheduler jobScheduler, String accountId, Date triggerStartTime) {
    JobDetail job =
        JobBuilder.newJob(ServiceInstanceUsageCheckerJob.class)
            .withIdentity(accountId, GROUP)
            .withDescription(
                "Checks for Service Instance usage of an account periodically and updates the violation (that is if usage > allowed usage) in database")
            .usingJobData(InstanceKeys.accountId, accountId)
            .build();

    TriggerBuilder triggerBuilder =
        TriggerBuilder.newTrigger()
            .withIdentity(accountId, GROUP)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(SYNC_INTERVAL_IN_HOURS).repeatForever());

    if (triggerStartTime != null) {
      triggerBuilder.startAt(triggerStartTime);
    }

    jobScheduler.ensureJob__UnderConstruction(job, triggerBuilder.build());
    log.info("Scheduled SI usage check job. accountId={}", accountId);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    executorService.submit(() -> {
      String accountId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(InstanceKeys.accountId);
      Objects.requireNonNull(accountId, "[ServiceInstanceUsageCheckerJob] accountId must be passed in job context");

      // Skip for non-CE accounts
      if (!accountService.isCommunityAccount(accountId)) {
        return;
      }

      log.info("Triggered: {} accountId: {}", ServiceInstanceUsageCheckerJob.class.getSimpleName(), accountId);
      double usage = InstanceStatsUtils.actualUsage(accountId, instanceStatService);

      instanceUsageLimitExcessHandler.updateViolationCount(accountId, usage);
    });
  }
}
