package io.harness.batch.processing.svcmetrics;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.metrics.service.api.MetricService;

import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BatchJobExecutionListener implements JobExecutionListener {
  private final MetricService metricService;

  @Autowired
  public BatchJobExecutionListener(MetricService metricService) {
    this.metricService = metricService;
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {}

  @Override
  public void afterJob(JobExecution jobExecution) {
    JobParameters params = jobExecution.getJobParameters();
    String accountId = params.getString(CCMJobConstants.ACCOUNT_ID);
    String jobType = jobExecution.getJobInstance().getJobName();

    Date startTime = jobExecution.getStartTime();
    Date endTime = jobExecution.getEndTime();
    long durationInMillis = endTime.getTime() - startTime.getTime();
    long durationInSeconds = durationInMillis / 1000;

    log.info("Job execution completed in {} sec: accountId={} jobType={}", durationInSeconds, accountId, jobType);
    try (JobMetricContext _ = new JobMetricContext(accountId, jobType)) {
      metricService.recordMetric(BatchProcessingMetricName.JOB_EXECUTION_TIME_IN_SEC, durationInSeconds);
    }
  }
}
