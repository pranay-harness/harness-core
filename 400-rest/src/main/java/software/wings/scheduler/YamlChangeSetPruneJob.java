/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.scheduler;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import static software.wings.yaml.gitSync.YamlChangeSet.Status.COMPLETED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.FAILED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.SKIPPED;

import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.scheduler.PersistentScheduler;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

@Slf4j
public class YamlChangeSetPruneJob implements Job {
  public static final String GROUP = "YAML_CHANGE_SET_PRUNE_GROUP";
  public static final String NAME = "MAINTENANCE";
  public static final String ACCOUNT_ID = "accountId";
  // Run every 6 hours, 4 times a day.
  private static final int POLL_INTERVAL = 6;
  // Delete max 20k records per job run
  private static final int MAX_DELETE_PER_JOB_RUN = 20000;
  // Delete any YamlChangeSets older than 30 days
  private static final int RETENTION_PERIOD_IN_DAYS = 30;
  // Delete 2k record in a batch
  private static final String BATCH_SIZE = "500";

  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private AccountService accountService;
  @Inject private PersistentLocker persistentLocker;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private ExecutorService executorService;

  public static void add(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(YamlChangeSetPruneJob.class).withIdentity(NAME, GROUP).build();

    OffsetDateTime startTime = OffsetDateTime.now().plusMinutes(10);

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(NAME, GROUP)
            .startAt(Date.from(startTime.toInstant()))
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    List<Account> accounts = accountService.list(aPageRequest().addFieldsIncluded("_id").build());
    for (Account account : accounts) {
      executorService.submit(() -> executeInternal(account.getUuid()));
    }
  }

  private void executeInternal(String accountId) {
    log.info("Running YamlChangeSetPruneJob");
    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(YamlChangeSet.class, accountId, Duration.ofSeconds(120))) {
      try {
        yamlChangeSetService.deleteChangeSets(accountId, new Status[] {FAILED, SKIPPED, COMPLETED},
            MAX_DELETE_PER_JOB_RUN, BATCH_SIZE, RETENTION_PERIOD_IN_DAYS);

      } catch (WingsException e) {
        log.error("YamlChangeSet deletion cron job failed with error: " + e.getParams().get("message"));
      }
    } catch (Exception e) {
      log.warn("Failed to acquire lock for account [{}]", accountId);
    }
  }
}
