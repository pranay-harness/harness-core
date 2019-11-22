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

  @Autowired private BatchJobRunner batchJobRunner;

  @Scheduled(cron = "0 */1 * * * ?")
  public void runEcsEventJob() {
    try {
      batchJobRunner.runJob(ecsJob, BatchJobType.ECS_EVENT, 1, ChronoUnit.DAYS);
    } catch (Exception ex) {
      logger.error("Exception while running runEcsEventJob job ", ex);
    }
  }

  @Scheduled(cron = "0 */1 * * * ?")
  public void runBillingBatchJob() {
    try {
      batchJobRunner.runJob(instanceBillingJob, BatchJobType.INSTANCE_BILLING, 1, ChronoUnit.DAYS);
    } catch (Exception ex) {
      logger.error("Exception while running runBillingBatchJob job ", ex);
    }
  }

  @Scheduled(cron = "0 */1 * * * ?")
  public void runK8sEventJob() {
    try {
      batchJobRunner.runJob(k8sJob, BatchJobType.K8S_EVENT, 1, ChronoUnit.DAYS); // TODO: change to DAYS
    } catch (Exception ex) {
      logger.error("Exception while running runK8sEventJob job ", ex);
    }
  }

  @Scheduled(cron = "0 */1 * * * ?")
  public void runEcsUtilizationJob() {
    try {
      batchJobRunner.runJob(ecsUtilizationJob, BatchJobType.ECS_UTILIZATION, 1, ChronoUnit.DAYS);
    } catch (Exception ex) {
      logger.error("Exception while running runEcsUtilizationJob job ", ex);
    }
  }
}
