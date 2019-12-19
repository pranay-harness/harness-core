package io.harness.batch.processing.schedule;

import io.harness.batch.processing.ccm.BatchJobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@EnableScheduling
public class EventJobScheduler {
  @Autowired private List<Job> jobs;
  @Autowired private BatchJobRunner batchJobRunner;

  @PostConstruct
  public void orderJobs() {
    jobs.sort(Comparator.comparingInt(job -> BatchJobType.valueOf(job.getName()).getOrder()));
  }

  @Scheduled(cron = "0 */1 * * * ?")
  public void runCloudEfficiencyEventJobs() {
    jobs.forEach(this ::runJob);
  }

  private void runJob(Job job) {
    try {
      batchJobRunner.runJob(job, 1, ChronoUnit.HOURS);
    } catch (Exception ex) {
      logger.error("Exception while running job {}", job);
    }
  }
}
