package io.harness.batch.processing.schedule;

import io.harness.batch.processing.ccm.BatchJobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@EnableScheduling
public class EventJobScheduler {
  @Autowired @Qualifier("ecsJob") private Job ecsJob;
  @Autowired @Qualifier("k8sJob") private Job k8sJob;
  @Autowired @Qualifier("ecsUtilizationJob") private Job ecsUtilizationJob;
  @Autowired @Qualifier("instanceBillingJob") private Job instanceBillingJob;
  @Autowired @Qualifier("k8sUtilizationJob") private Job k8sUtilizationJob;

  @Autowired private BatchJobRunner batchJobRunner;

  @Scheduled(cron = "0 */1 * * * ?")
  public void runCloudEfficiencyEventJobs() {
    try {
      batchJobRunner.runJob(ecsJob, BatchJobType.ECS_EVENT, 1, ChronoUnit.HOURS);
    } catch (Exception ex) {
      logger.error("Exception while running runEcsEventJob job ", ex);
    }

    try {
      batchJobRunner.runJob(k8sJob, BatchJobType.K8S_EVENT, 1, ChronoUnit.HOURS);
    } catch (Exception ex) {
      logger.error("Exception while running runK8sEventJob job ", ex);
    }

    try {
      batchJobRunner.runJob(ecsUtilizationJob, BatchJobType.ECS_UTILIZATION, 1, ChronoUnit.HOURS);
    } catch (Exception ex) {
      logger.error("Exception while running runEcsUtilizationJob job ", ex);
    }

    try {
      batchJobRunner.runJob(k8sUtilizationJob, BatchJobType.K8S_UTILIZATION, 1, ChronoUnit.HOURS);
    } catch (Exception ex) {
      logger.error("Exception while running runK8sUtilizationJob job ", ex);
    }

    try {
      batchJobRunner.runJob(instanceBillingJob, BatchJobType.INSTANCE_BILLING, 1, ChronoUnit.HOURS);
    } catch (Exception ex) {
      logger.error("Exception while running runBillingBatchJob job ", ex);
    }
  }
}
