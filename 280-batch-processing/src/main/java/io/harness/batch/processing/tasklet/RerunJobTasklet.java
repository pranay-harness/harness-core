package io.harness.batch.processing.tasklet;

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

@Slf4j
public class RerunJobTasklet implements Tasklet {
  private JobParameters parameters;
  @Autowired private InstanceDataBulkWriteService instanceDataBulkWriteService;
  @Autowired private CEMetadataRecordDao ceMetadataRecordDao;
  @Autowired private BatchJobScheduledDataService batchJobScheduledDataService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    Long endTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

    Instant startInstant = Instant.ofEpochMilli(startTime).minus(4, ChronoUnit.DAYS);
    CEMetadataRecord ceMetadataRecord = ceMetadataRecordDao.getByAccountId(accountId);
    if (null != ceMetadataRecord && isCloudDataPresent(ceMetadataRecord)) {
      log.info("invalidate jobs for {}", accountId);
      ImmutableList<String> batchJobs = ImmutableList.of(BatchJobType.ANOMALY_DETECTION_CLOUD.toString());
      batchJobScheduledDataService.invalidateJobs(accountId, batchJobs, startInstant);
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
