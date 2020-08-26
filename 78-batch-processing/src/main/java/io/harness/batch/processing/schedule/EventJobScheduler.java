package io.harness.batch.processing.schedule;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.WeeklyReportServiceImpl;
import io.harness.batch.processing.budgets.service.impl.BudgetAlertsServiceImpl;
import io.harness.batch.processing.ccm.BatchJobBucket;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.config.GcpScheduledQueryTriggerAction;
import io.harness.batch.processing.metrics.ProductMetricsService;
import io.harness.batch.processing.service.impl.BatchJobBucketLogContext;
import io.harness.batch.processing.service.impl.BatchJobTypeLogContext;
import io.harness.batch.processing.service.intfc.BillingDataPipelineHealthStatusService;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@EnableScheduling
public class EventJobScheduler {
  @Autowired private List<Job> jobs;
  @Autowired private BatchJobRunner batchJobRunner;
  @Autowired private AccountShardService accountShardService;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;
  @Autowired private WeeklyReportServiceImpl weeklyReportService;
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private BillingDataPipelineHealthStatusService billingDataPipelineHealthStatusService;
  @Autowired private GcpScheduledQueryTriggerAction gcpScheduledQueryTriggerAction;
  @Autowired private ProductMetricsService productMetricsService;
  @Autowired private BudgetAlertsServiceImpl budgetAlertsService;

  @PostConstruct
  public void orderJobs() {
    jobs.sort(Comparator.comparingInt(job -> BatchJobType.valueOf(job.getName()).getOrder()));
  }

  // this job runs every 1 hours "0 0 * ? * *". For debugging, run every minute "* * * ? * *"
  @Scheduled(cron = "0 */20 * * * ?")
  public void runCloudEfficiencyInClusterJobs() {
    runCloudEfficiencyEventJobs(BatchJobBucket.IN_CLUSTER, true);
  }

  @Scheduled(cron = "0 0 * ? * *")
  public void runCloudEfficiencyOutOfClusterJobs() {
    runCloudEfficiencyEventJobs(BatchJobBucket.OUT_OF_CLUSTER, true);
  }

  private void runCloudEfficiencyEventJobs(BatchJobBucket batchJobBucket, boolean runningMode) {
    accountShardService.getCeEnabledAccounts().forEach(account
        -> jobs.stream()
               .filter(job -> BatchJobType.fromJob(job).getBatchJobBucket() == batchJobBucket)
               .forEach(job -> runJob(account.getUuid(), job, runningMode)));
  }

  @Scheduled(cron = "0 0 */6 ? * *")
  public void scanDelayedJobs() {
    logger.info("Inside scanning delayed jobs !! ");
    Stream.of(BatchJobBucket.values()).forEach(batchJobBucket -> runCloudEfficiencyEventJobs(batchJobBucket, false));
  }

  // this job runs every 4 hours "0 0 */4 ? * *". For debugging, run every minute "* * * ? * *"
  @Scheduled(cron = "0 0 */4 ? * *")
  public void sendSegmentEvents() {
    runCloudEfficiencyEventJobs(BatchJobBucket.OTHERS, true);
  }

  @Scheduled(cron = "0 * * ? * *")
  public void runGcpScheduledQueryJobs() {
    accountShardService.getCeEnabledAccounts().forEach(
        account -> gcpScheduledQueryTriggerAction.execute(account.getUuid()));
  }

  @Scheduled(cron = "0 0 8 * * ?")
  public void runTimescalePurgeJob() {
    boolean masterPod = accountShardService.isMasterPod();
    if (masterPod) {
      try {
        k8sUtilizationGranularDataService.purgeOldKubernetesUtilData();
      } catch (Exception ex) {
        logger.error("Exception while running runTimescalePurgeJob", ex);
      }

      try {
        billingDataService.purgeOldHourlyBillingData();
      } catch (Exception ex) {
        logger.error("Exception while running purgeOldHourlyBillingData Job", ex);
      }
    }
  }

  @Scheduled(cron = "0 0 */4 ? * *")
  public void runConnectorsHealthStatusJob() {
    boolean masterPod = accountShardService.isMasterPod();
    if (masterPod) {
      try {
        billingDataPipelineHealthStatusService.processAndUpdateHealthStatus();
      } catch (Exception ex) {
        logger.error("Exception while running runConnectorsHealthStatusJob {}", ex);
      }
    }
  }

  @Scheduled(cron = "0 0 14 * * MON")
  public void runWeeklyReportJob() {
    try {
      weeklyReportService.generateAndSendWeeklyReport();
      logger.info("Weekly billing report generated and send");
    } catch (Exception ex) {
      logger.error("Exception while running weeklyReportJob", ex);
    }
  }

  @Scheduled(cron = "0 30 14 * * ?")
  public void runBudgetAlertsJob() {
    try {
      budgetAlertsService.sendBudgetAlerts();
      logger.info("Budget alerts send");
    } catch (Exception ex) {
      logger.error("Exception while running budgetAlertsJob", ex);
    }
  }

  @SuppressWarnings("squid:S1166") // not required to rethrow exceptions.
  private void runJob(String accountId, Job job, boolean runningMode) {
    try {
      BatchJobType batchJobType = BatchJobType.fromJob(job);
      if (BatchJobType.AWS_ECS_CLUSTER_DATA_SYNC == batchJobType && !accountId.equals("zEaak-FLS425IEO7OLzMUg")) {
        return;
      }
      BatchJobBucket batchJobBucket = batchJobType.getBatchJobBucket();
      try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR);
           AutoLogContext ignore1 = new BatchJobBucketLogContext(batchJobBucket.name(), OVERRIDE_ERROR);
           AutoLogContext ignore2 = new BatchJobTypeLogContext(batchJobType.name(), OVERRIDE_ERROR)) {
        batchJobRunner.runJob(accountId, job, runningMode);
      }
    } catch (Exception ex) {
      logger.error("Exception while running job {}", job);
    }
  }
}
