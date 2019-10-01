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
public class EcsEventJobScheduler {
  @Autowired @Qualifier("ecsJob") private Job ecsJob;
  @Autowired @Qualifier("k8sJob") private Job k8sJob;

  @Autowired private BatchJobRunner batchJobRunner;

  @Scheduled(cron = "0 */1 * * * ?")
  public void runEcsEventJob() {
    try {
      batchJobRunner.runJob(ecsJob, BatchJobType.ECS_EVENT, 1, ChronoUnit.DAYS);
    } catch (Exception ex) {
      logger.error("Exception while running job ", ex);
    }
  }

  @Scheduled(cron = "0 */1 * * * ?")
  public void runK8sEventJob() {
    try {
      batchJobRunner.runJob(k8sJob, BatchJobType.K8S_EVENT, 1, ChronoUnit.DAYS); // TODO: change to DAYS
    } catch (Exception ex) {
      logger.error("Exception while running job ", ex);
    }
  }
}
