package io.harness.batch.processing.anomalydetection;

import io.harness.batch.processing.anomalydetection.alerts.service.itfc.AnomalyAlertsService;
import io.harness.batch.processing.ccm.CCMJobConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class SlackNotificationsTasklet implements Tasklet {
  private JobParameters parameters;

  @Autowired @Inject private AnomalyAlertsService alertsService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant endTime = getFieldValueFromJobParams(CCMJobConstants.JOB_END_DATE);
    alertsService.sendAnomalyDailyReport(accountId, endTime);
    return null;
  }
  private Instant getFieldValueFromJobParams(String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(parameters.getString(fieldName)));
  }
}