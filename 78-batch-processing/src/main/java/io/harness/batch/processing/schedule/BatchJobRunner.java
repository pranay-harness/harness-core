package io.harness.batch.processing.schedule;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class BatchJobRunner {
  @Autowired private JobLauncher jobLauncher;

  @Autowired private BatchJobScheduledDataService batchJobScheduledDataService;

  /**
   * Runs the batch job from previous end time and save the job logs
   * @param job - Job
   * @param batchJobType - type of batch job
   * @param duration - event duration for job (endTime - startTime)
   * @param chronoUnit - duration unit
   * @throws Exception
   */
  public void runJob(Job job, BatchJobType batchJobType, long duration, ChronoUnit chronoUnit) throws Exception {
    Instant startAt = batchJobScheduledDataService.fetchLastBatchJobScheduledTime(batchJobType);
    Instant endAt = Instant.now().minus(10, ChronoUnit.MINUTES);
    BatchJobScheduleTimeProvider batchJobScheduleTimeProvider =
        new BatchJobScheduleTimeProvider(startAt, endAt, duration, chronoUnit);
    Instant startInstant = startAt;
    while (batchJobScheduleTimeProvider.hasNext()) {
      Instant endInstant = batchJobScheduleTimeProvider.next();
      JobParameters params = new JobParametersBuilder()
                                 .addString("JobID", String.valueOf(System.currentTimeMillis()))
                                 .addString("startDate", String.valueOf(startInstant.toEpochMilli()))
                                 .addString("endDate", String.valueOf(endInstant.toEpochMilli()))
                                 .toJobParameters();
      logger.info("Job params {} ", params.toString());
      jobLauncher.run(job, params);
      BatchJobScheduledData batchJobScheduledData = new BatchJobScheduledData(batchJobType, startInstant, endInstant);
      batchJobScheduledDataService.create(batchJobScheduledData);
      startInstant = endInstant;
    }
  }
}
