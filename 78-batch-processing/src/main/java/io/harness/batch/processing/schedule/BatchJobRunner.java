package io.harness.batch.processing.schedule;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
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
  public void runJob(Job job, BatchJobType batchJobType, long duration, ChronoUnit chronoUnit)
      throws JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException,
             JobInstanceAlreadyCompleteException {
    Instant startAt = batchJobScheduledDataService.fetchLastBatchJobScheduledTime(batchJobType);
    Instant endAt = Instant.now();
    BatchJobScheduleTimeProvider batchJobScheduleTimeProvider =
        new BatchJobScheduleTimeProvider(startAt, endAt, duration, chronoUnit);
    Instant startInstant = startAt;
    while (batchJobScheduleTimeProvider.hasNext()) {
      Instant endInstant = batchJobScheduleTimeProvider.next();
      if (null != endInstant) {
        JobParameters params =
            new JobParametersBuilder()
                .addString(CCMJobConstants.JOB_ID, String.valueOf(System.currentTimeMillis()))
                .addString(CCMJobConstants.JOB_START_DATE, String.valueOf(startInstant.toEpochMilli()))
                .addString(CCMJobConstants.JOB_END_DATE, String.valueOf(endInstant.toEpochMilli()))
                .toJobParameters();
        jobLauncher.run(job, params);
        BatchJobScheduledData batchJobScheduledData = new BatchJobScheduledData(batchJobType, startInstant, endInstant);
        batchJobScheduledDataService.create(batchJobScheduledData);
        startInstant = endInstant;
      } else {
        break;
      }
    }
  }
}
