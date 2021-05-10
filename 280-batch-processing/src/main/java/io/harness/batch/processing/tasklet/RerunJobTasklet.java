package io.harness.batch.processing.tasklet;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.batch.processing.service.intfc.InstanceDataBulkWriteService;
import io.harness.ccm.commons.dao.CEMetadataRecordDao;
import io.harness.ccm.commons.entities.CEMetadataRecord;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@OwnedBy(HarnessTeam.CE)
@Slf4j
public class RerunJobTasklet implements Tasklet {
  private JobParameters parameters;
  @Autowired private InstanceDataBulkWriteService instanceDataBulkWriteService;
  @Autowired private CEMetadataRecordDao ceMetadataRecordDao;
  @Autowired private BatchJobScheduledDataService batchJobScheduledDataService;
  @Autowired private BillingDataServiceImpl billingDataService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    Long endTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

    Instant startInstant = Instant.ofEpochMilli(startTime).minus(3, ChronoUnit.DAYS);
    CEMetadataRecord ceMetadataRecord = ceMetadataRecordDao.getByAccountId(accountId);
    if (null != ceMetadataRecord && isCloudDataPresent(ceMetadataRecord)) {
      log.info("invalidate jobs for {}", accountId);
      ImmutableList<String> batchJobs = ImmutableList.of(BatchJobType.ANOMALY_DETECTION_CLOUD.toString());
      batchJobScheduledDataService.invalidateJobs(accountId, batchJobs, startInstant);
    }

    if (null != ceMetadataRecord
        && ((null != ceMetadataRecord.getAwsDataPresent() && ceMetadataRecord.getAwsDataPresent())
            || (null != ceMetadataRecord.getAzureDataPresent() && ceMetadataRecord.getAzureDataPresent()))) {
      log.info("invalidate cluster jobs for {}", accountId);
      ImmutableList<String> batchJobs =
          ImmutableList.of(BatchJobType.INSTANCE_BILLING.toString(), BatchJobType.ACTUAL_IDLE_COST_BILLING.toString(),
              BatchJobType.INSTANCE_BILLING_AGGREGATION.toString(), BatchJobType.CLUSTER_DATA_TO_BIG_QUERY.toString());
      batchJobScheduledDataService.invalidateJobs(accountId, batchJobs, startInstant);
      boolean cleanBillingData = billingDataService.cleanBillingData(
          accountId, startInstant, Instant.ofEpochMilli(endTime), BatchJobType.INSTANCE_BILLING);
      if (!cleanBillingData) {
        throw new Exception("Error Cleaning billing data");
      }
    }
    return null;
  }

  private boolean isCloudDataPresent(CEMetadataRecord ceMetadataRecord) {
    if ((null != ceMetadataRecord.getAwsDataPresent() && ceMetadataRecord.getAwsDataPresent())
        || (null != ceMetadataRecord.getGcpDataPresent() && ceMetadataRecord.getGcpDataPresent())
        || (null != ceMetadataRecord.getAzureDataPresent() && ceMetadataRecord.getAzureDataPresent())) {
      return true;
    }
    return false;
  }
}
