package io.harness.batch.processing.schedule;

import static io.harness.batch.processing.ccm.BatchJobType.DEPLOYMENT_EVENT;
import static io.harness.batch.processing.ccm.BatchJobType.K8S_WATCH_EVENT;

import com.google.common.collect.ImmutableSet;

import io.harness.batch.processing.ccm.BatchJobType;
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

  @PostConstruct
  public void orderJobs() {
    jobs.sort(Comparator.comparingInt(job -> BatchJobType.valueOf(job.getName()).getOrder()));
  }

  @Scheduled(cron = "0 */20 * * * ?")
  public void runCloudEfficiencyEventJobs() {
    List<Account> ccmEnabledAccounts = cloudToHarnessMappingService.getCCMEnabledAccounts();
    ccmEnabledAccounts.forEach(account -> jobs.forEach(job -> runJob(account.getUuid(), job)));
  }

  private void runJob(String accountId, Job job) {
    try {
      if (ImmutableSet.of(DEPLOYMENT_EVENT, K8S_WATCH_EVENT).contains(BatchJobType.fromJob(job))) {
        return;
      }
      batchJobRunner.runJob(accountId, job);
    } catch (Exception ex) {
      logger.error("Exception while running job {}", job);
    }
  }
}
