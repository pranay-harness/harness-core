package io.harness.batch.processing.schedule;

import static io.harness.batch.processing.ccm.BatchJobType.BILLING_DATA_PIPELINE;
import static io.harness.batch.processing.ccm.BatchJobType.DEPLOYMENT_EVENT;
import static io.harness.batch.processing.ccm.BatchJobType.K8S_WATCH_EVENT;
import static io.harness.batch.processing.ccm.BatchJobType.SYNC_BILLING_REPORT_S3;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.collect.ImmutableSet;

import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
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
  }

  private void runJob(String accountId, Job job) {
    try {
      if (ImmutableSet.of(BILLING_DATA_PIPELINE, DEPLOYMENT_EVENT, K8S_WATCH_EVENT, SYNC_BILLING_REPORT_S3)
              .contains(BatchJobType.fromJob(job))) {
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
