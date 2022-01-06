/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.scheduler;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.beans.trigger.TriggerConditionType.SCHEDULED;
import static software.wings.common.Constants.ACCOUNT_ID_KEY;
import static software.wings.common.Constants.APP_ID_KEY;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.scheduler.PersistentScheduler;

import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.TriggerService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerBuilder;

/**
 * Created by sgurubelli on 10/26/17.
 */
@OwnedBy(CDC)
@Slf4j
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class ScheduledTriggerJob implements Job {
  public static final String GROUP = "SCHEDULED_TRIGGER_CRON_GROUP";

  // 'Second' unit prefix to convert unix to quartz cron expression
  public static final String PREFIX = "0 ";
  public static final String TRIGGER_ID_KEY = "triggerId";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private TriggerService triggerService;
  @Inject private AppService appService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public static org.quartz.Trigger getQuartzTrigger(Trigger trigger) {
    return TriggerBuilder.newTrigger()
        .withIdentity(trigger.getUuid(), ScheduledTriggerJob.GROUP)
        .withSchedule(CronScheduleBuilder.cronSchedule(
            PREFIX + ((ScheduledTriggerCondition) trigger.getCondition()).getCronExpression()))
        .build();
  }

  public static void add(
      PersistentScheduler jobScheduler, String accountId, String appId, String triggerId, Trigger trigger) {
    add(jobScheduler, accountId, appId, triggerId, getQuartzTrigger(trigger));
  }

  public static void add(
      PersistentScheduler jobScheduler, String accountId, String appId, String triggerId, org.quartz.Trigger trigger) {
    JobDetail job = JobBuilder.newJob(ScheduledTriggerJob.class)
                        .withIdentity(triggerId, ScheduledTriggerJob.GROUP)
                        .usingJobData(TRIGGER_ID_KEY, triggerId)
                        .usingJobData(APP_ID_KEY, appId)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .build();
    jobScheduler.scheduleJob(job, trigger);
  }

  public static void delete(PersistentScheduler jobScheduler, String triggerId) {
    jobScheduler.deleteJob(triggerId, GROUP);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String triggerId = jobExecutionContext.getMergedJobDataMap().getString(TRIGGER_ID_KEY);
    String appId = jobExecutionContext.getMergedJobDataMap().getString(APP_ID_KEY);
    String accountId = jobExecutionContext.getMergedJobDataMap().getString(ACCOUNT_ID_KEY);
    Trigger trigger = wingsPersistence.getWithAppId(Trigger.class, appId, triggerId);
    if (trigger == null || trigger.getCondition().getConditionType() != SCHEDULED) {
      log.info("Trigger not found or wrong type. Deleting job associated to it");
      jobScheduler.deleteJob(triggerId, GROUP);
      return;
    }
    log.warn("Trigger running with quartz scheduler and not iterator framework with appId - {} and triggerId - {}",
        appId, triggerId);
    log.info("Triggering scheduled job for appId {} and triggerId {} with the scheduled fire time {}", appId, triggerId,
        jobExecutionContext.getNextFireTime());
    triggerService.triggerScheduledExecutionAsync(trigger, jobExecutionContext.getNextFireTime());

    // Old cron jobs doesn't have accountId. Will need to recreate with accountId as part of the job details
    if (isEmpty(accountId)) {
      log.info(
          "Quartz job '{}' in group {} doesn't have accountId in job details. Will recreate with accountId included.",
          triggerId, GROUP);
      jobScheduler.deleteJob(triggerId, GROUP);
      accountId = appService.getAccountIdByAppId(appId);
      add(jobScheduler, accountId, appId, triggerId, getQuartzTrigger(trigger));
    }
  }
}
