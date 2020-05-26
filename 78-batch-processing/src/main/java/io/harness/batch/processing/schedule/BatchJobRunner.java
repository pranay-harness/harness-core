package io.harness.batch.processing.schedule;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.service.intfc.BatchJobIntervalService;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.ccm.cluster.entities.BatchJobInterval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
public class BatchJobRunner {
  @Autowired private JobLauncher jobLauncher;
  @Autowired private BatchJobIntervalService batchJobIntervalService;
  @Autowired private BatchJobScheduledDataService batchJobScheduledDataService;

  /**
   * Runs the batch job from previous end time and save the job logs
   * @param job - Job
   * @throws Exception
   */
  public void runJob(String accountId, Job job) throws JobParametersInvalidException,
                                                       JobExecutionAlreadyRunningException, JobRestartException,
                                                       JobInstanceAlreadyCompleteException {
    BatchJobType batchJobType = BatchJobType.fromJob(job);
    long duration = batchJobType.getInterval();
    ChronoUnit chronoUnit = batchJobType.getIntervalUnit();
    BatchJobInterval batchJobInterval = batchJobIntervalService.fetchBatchJobInterval(accountId, batchJobType);
    if (null != batchJobInterval) {
      chronoUnit = batchJobInterval.getIntervalUnit();
      duration = batchJobInterval.getInterval();
    }
    List<BatchJobType> dependentBatchJobs = batchJobType.getDependentBatchJobs();
    Instant startAt = batchJobScheduledDataService.fetchLastBatchJobScheduledTime(accountId, batchJobType);
    if (null == startAt) {
      logger.warn("Event not received for account {} ", accountId);
      return;
    }
    Instant endAt = Instant.now().minus(1, ChronoUnit.HOURS);
    BatchJobScheduleTimeProvider batchJobScheduleTimeProvider =
        new BatchJobScheduleTimeProvider(startAt, endAt, duration, chronoUnit);
    Instant startInstant = startAt;
    while (batchJobScheduleTimeProvider.hasNext()) {
      Instant endInstant = batchJobScheduleTimeProvider.next();
      if (null != endInstant && checkDependentJobFinished(accountId, startInstant, dependentBatchJobs)) {
        JobParameters params =
            new JobParametersBuilder()
                .addString(CCMJobConstants.JOB_ID, String.valueOf(System.currentTimeMillis()))
                .addString(CCMJobConstants.ACCOUNT_ID, accountId)
                .addString(CCMJobConstants.JOB_START_DATE, String.valueOf(startInstant.toEpochMilli()))
                .addString(CCMJobConstants.JOB_END_DATE, String.valueOf(endInstant.toEpochMilli()))
                .addString(CCMJobConstants.BATCH_JOB_TYPE, batchJobType.name())
                .toJobParameters();
        Instant jobStartTime = Instant.now();
        BatchStatus status = jobLauncher.run(job, params).getStatus();
        logger.info("Job status {}", status);
        Instant jobStopTime = Instant.now();

        if (status == BatchStatus.COMPLETED) {
          BatchJobScheduledData batchJobScheduledData = new BatchJobScheduledData(accountId, batchJobType,
              Duration.between(jobStartTime, jobStopTime).toMillis(), startInstant, endInstant);
          batchJobScheduledDataService.create(batchJobScheduledData);
          startInstant = endInstant;
        } else {
          logger.error("Error while running batch job for account {} type {} status {} time range {} - {}", accountId,
              batchJobType, status, startInstant, endInstant);
          break;
        }
      } else {
        break;
      }
    }
  }

  boolean checkDependentJobFinished(String accountId, Instant startAt, List<BatchJobType> dependentBatchJobs) {
    for (BatchJobType dependentBatchJob : dependentBatchJobs) {
      Instant instant =
          batchJobScheduledDataService.fetchLastDependentBatchJobScheduledTime(accountId, dependentBatchJob);
      if (null == instant || !instant.isAfter(startAt)) {
        return false;
      }
    }
    return true;
  }
}
