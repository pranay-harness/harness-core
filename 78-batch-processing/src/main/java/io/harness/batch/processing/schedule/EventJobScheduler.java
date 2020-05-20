package io.harness.batch.processing.schedule;

import static io.harness.batch.processing.ccm.BatchJobType.DEPLOYMENT_EVENT;
import static io.harness.batch.processing.ccm.BatchJobType.K8S_WATCH_EVENT;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.collect.ImmutableSet;

import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.WeeklyReportServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import software.wings.beans.Account;
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

  @PostConstruct
  public void orderJobs() {
    jobs.sort(Comparator.comparingInt(job -> BatchJobType.valueOf(job.getName()).getOrder()));
  }

  @Scheduled(cron = "0 */20 * * * ?")
  public void runCloudEfficiencyEventJobs() {
    List<Account> ccmEnabledAccounts = cloudToHarnessMappingService.getCCMEnabledAccounts();
    ccmEnabledAccounts.forEach(account -> jobs.forEach(job -> runJob(account.getUuid(), job)));
  }

  @Scheduled(cron = "0 0 8 * * ?")
  public void runTimescalePurgeJob() {
    try {
      k8sUtilizationGranularDataService.purgeOldKubernetesUtilData();
    } catch (Exception ex) {
      logger.error("Exception while running runTimescalePurgeJob {}", ex);
    }

    try {
      billingDataService.purgeOldHourlyBillingData();
    } catch (Exception ex) {
      logger.error("Exception while running purgeOldHourlyBillingData Job {}", ex);
    }
  }

  @Scheduled(cron = "0 0 1 * * ?")
  public void runWeeklyReportJob() {
    try {
      weeklyReportService.generateAndSendWeeklyReport();
      logger.info("Weekly billing report generated and send");
    } catch (Exception ex) {
      logger.error("Exception while running weeklyReportJob {}", ex);
    }
  }

  private void runJob(String accountId, Job job) {
    try {
      if (ImmutableSet.of(DEPLOYMENT_EVENT, K8S_WATCH_EVENT).contains(BatchJobType.fromJob(job))) {
        return;
      }
      try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
        batchJobRunner.runJob(accountId, job);
      }
    } catch (Exception ex) {
      logger.error("Exception while running job {}", job);
    }
  }
}
