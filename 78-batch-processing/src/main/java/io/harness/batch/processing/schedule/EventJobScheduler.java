package io.harness.batch.processing.schedule;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.WeeklyReportServiceImpl;
import io.harness.batch.processing.ccm.BatchJobBucket;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.config.GcpScheduledQueryTriggerAction;
import io.harness.batch.processing.service.impl.BatchJobBucketLogContext;
import io.harness.batch.processing.service.intfc.BillingDataPipelineHealthStatusService;
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
import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@EnableScheduling
public class EventJobScheduler {
  @Autowired private List<Job> jobs;
  @Autowired private BatchJobRunner batchJobRunner;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;
  @Autowired private WeeklyReportServiceImpl weeklyReportService;
  @Autowired private BillingDataServiceImpl billingDataService;
  @Autowired private BillingDataPipelineHealthStatusService billingDataPipelineHealthStatusService;
  @Autowired private GcpScheduledQueryTriggerAction gcpScheduledQueryTriggerAction;

  @PostConstruct
  public void orderJobs() {
    jobs.sort(Comparator.comparingInt(job -> BatchJobType.valueOf(job.getName()).getOrder()));
  }

  @Scheduled(cron = "0 0 * ? * *")
  public void runCloudEfficiencyB1Jobs() {
    runCloudEfficiencyEventJobs(BatchJobBucket.OUT_OF_CLUSTER);
  }

  @Scheduled(cron = "0 */20 * * * ?")
  public void runCloudEfficiencyB2Jobs() {
    runCloudEfficiencyEventJobs(BatchJobBucket.IN_CLUSTER);
  }

  public void runCloudEfficiencyEventJobs(BatchJobBucket batchJobBucket) {
    cloudToHarnessMappingService.getCCMEnabledAccounts().forEach(account
        -> jobs.stream()
               .filter(job -> BatchJobType.fromJob(job).getBatchJobBucket() == batchJobBucket)
               .forEach(job -> runJob(account.getUuid(), job)));
  }

  @Scheduled(cron = "0 * * ? * *")
  public void runGcpScheduledQueryJobs() {
    cloudToHarnessMappingService.getCCMEnabledAccounts().forEach(
        account -> gcpScheduledQueryTriggerAction.execute(account.getUuid()));
  }

  @Scheduled(cron = "0 0 8 * * ?")
  public void runTimescalePurgeJob() {
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

  @Scheduled(cron = "0 0 8 * * ?")
  public void runConnectorsHealthStatusJob() {
    try {
      billingDataPipelineHealthStatusService.processAndUpdateHealthStatus();
    } catch (Exception ex) {
      logger.error("Exception while running runConnectorsHealthStatusJob {}", ex);
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

  @SuppressWarnings("squid:S1166") // not required to rethrow exceptions.
  private void runJob(String accountId, Job job) {
    try {
      BatchJobType batchJobType = BatchJobType.fromJob(job);
      BatchJobBucket batchJobBucket = batchJobType.getBatchJobBucket();
      try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR);
           AutoLogContext ignore1 = new BatchJobBucketLogContext(batchJobBucket.name(), OVERRIDE_ERROR)) {
        batchJobRunner.runJob(accountId, job);
      }
    } catch (Exception ex) {
      logger.error("Exception while running job {}", job);
    }
  }
}
