package io.harness.batch.processing.billing.writer;

import static java.lang.String.format;

import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;

import com.google.inject.Singleton;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class InstanceBillingAggregationDataTasklet implements Tasklet {
  @Autowired private BillingDataServiceImpl billingDataService;

  private JobParameters parameters;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant startTime = getFieldValueFromJobParams(CCMJobConstants.JOB_START_DATE);
    Instant endTime = getFieldValueFromJobParams(CCMJobConstants.JOB_END_DATE);
    BatchJobType batchJobType =
        CCMJobConstants.getBatchJobTypeFromJobParams(parameters, CCMJobConstants.BATCH_JOB_TYPE);

    // since we have not created unique index, we have to delete existing data to handle batch job re-run for a
    // particular (starttime, endtime).
    if (!billingDataService.cleanPreAggBillingData(accountId, startTime, endTime)
        || !billingDataService.generatePreAggBillingData(accountId, startTime, endTime)) {
      log.error("BatchJobType:{} execution was un-successful for accountId:{} startTime:{} endTime:{}",
          batchJobType.name(), accountId, startTime, endTime);
      throw new Exception(format("BatchJobType:%s failed for accountId:%s startTime:%s endTime:%s", batchJobType.name(),
          accountId, startTime, endTime));
    }
    return null;
  }

  private Instant getFieldValueFromJobParams(String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(Objects.requireNonNull(parameters.getString(fieldName))));
  }
}
