package software.wings.scheduler;

import com.google.inject.Inject;

import io.harness.event.model.EventConstants;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.event.usagemetrics.UsageMetricsHelper;
import io.harness.lock.AcquiredLock;
import io.harness.scheduler.BackgroundExecutorService;
import io.harness.scheduler.BackgroundSchedulerLocker;
import io.harness.scheduler.PersistentScheduler;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.instance.dashboard.InstanceStatsUtil;
import software.wings.service.intfc.instance.stats.InstanceStatService;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@DisallowConcurrentExecution
public class InstanceStatsMetricsJob implements Job {
  private static final String CRON_NAME = "INSTANCE_STATS_METRICS_CRON_NAME";
  private static final String CRON_GROUP = "INSTANCE_STATS_METRICS_CRON_GROUP";
  private static final String LOCK = "INSTANCE_STATS_METRICS";
  private static final Logger logger = LoggerFactory.getLogger(InstanceStatsMetricsJob.class);

  @Inject private BackgroundExecutorService executorService;
  @Inject private BackgroundSchedulerLocker persistentLocker;
  @Inject private InstanceStatService instanceStatService;
  @Inject private UsageMetricsHelper usageMetricsHelper;
  @Inject private UsageMetricsEventPublisher eventPublisher;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running instance stats metrics job asynchronously");
    executorService.submit(this ::executeInternal);
  }

  private void executeInternal() {
    try (AcquiredLock lock = persistentLocker.getLocker().tryToAcquireLock(LOCK, Duration.ofMinutes(5))) {
      if (lock == null) {
        logger.info("Lock not acquired");
        return;
      }
      try {
        List<Account> accountList = usageMetricsHelper.listAllAccountsWithDefaults();
        Map<String, Integer> instanceCountMap = usageMetricsHelper.getAllValidInstanceCounts();
        logger.info("Instance metrics job start");
        final int[] globalCount = new int[] {0};
        accountList.forEach(account -> {
          try {
            logger.info("Publishing metric for accountId:[{}]", account.getUuid());
            Integer instanceCount =
                instanceCountMap.get(account.getUuid()) == null ? 0 : instanceCountMap.get(account.getUuid());
            eventPublisher.publishInstanceMetric(
                account.getUuid(), account.getAccountName(), instanceCount, EventConstants.INSTANCE_COUNT_TOTAL);
            eventPublisher.publishInstanceMetric(account.getUuid(), account.getAccountName(),
                InstanceStatsUtil.actualUsage(account.getUuid(), instanceStatService),
                EventConstants.INSTANCE_COUNT_NINETY_FIVE_PERCENTILE);
            globalCount[0] = +instanceCount;

          } catch (Exception e) {
            logger.warn("Failed to retrieve metric for accountId: [{}], accountName: [{}]", account.getUuid(),
                account.getAccountName(), e);
          }
        });
        try {
          eventPublisher.publishInstanceMetric(Account.GLOBAL_ACCOUNT_ID, Account.GLOBAL_ACCOUNT_ID, globalCount[0],
              EventConstants.INSTANCE_COUNT_GLOBAL_TOTAL);
        } catch (Exception e) {
          logger.warn("Failed to publish global account metrics", e);
        }
        logger.info("Instance metrics job end");
      } catch (Exception e) {
        logger.warn("Instance metrics job failed to execute", e);
      }
    }
  }

  public static void addJob(PersistentScheduler jobScheduler) {
    JobDetail job = JobBuilder.newJob(InstanceStatsMetricsJob.class)
                        .withIdentity(CRON_NAME, CRON_GROUP)
                        .withDescription("Instance Stats Metric Job")
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(CRON_NAME, CRON_GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(10).repeatForever())
            .build();

    jobScheduler.ensureJob__UnderConstruction(job, trigger);
  }
}
