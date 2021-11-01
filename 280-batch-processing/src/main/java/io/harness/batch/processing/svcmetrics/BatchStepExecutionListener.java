package io.harness.batch.processing.svcmetrics;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.metrics.AutoMetricContext;
import io.harness.metrics.service.api.MetricService;

import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BatchStepExecutionListener implements StepExecutionListener {
  private final MetricService metricService;

  @Autowired
  public BatchStepExecutionListener(MetricService metricService) {
    this.metricService = metricService;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {}

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    String accountId = stepExecution.getJobExecution().getJobParameters().getString(CCMJobConstants.ACCOUNT_ID);

    log.info("XXX Step execution completed: accountId={} stepName={} startTime={} endTime={} status={}", accountId,
        stepExecution.getStepName(), stepExecution.getStartTime(), stepExecution.getEndTime(),
        stepExecution.getStatus()); // TODO REMOVE

    Date startTime = stepExecution.getStartTime();
    Date endTime = stepExecution.getEndTime();
    long durationInMillis = endTime.getTime() - startTime.getTime();
    long durationInSeconds = durationInMillis / 1000;

    log.info("Step execution completed in {} sec: accountId={} stepName={}", durationInSeconds, accountId,
        stepExecution.getStepName());
    try (StepMetricContext _ = new StepMetricContext(accountId, stepExecution.getStepName())) {
      metricService.recordMetric(BatchProcessingMetricName.STEP_EXECUTION_TIME_IN_SEC, durationInSeconds);
    }
    return null;
  }
}

@Data
@Builder
@NoArgsConstructor
class StepMetricContext extends AutoMetricContext {
  StepMetricContext(String accountId, String stepName) {
    put("accountId", accountId);
    put("stepName", stepName);
  }
}