package io.harness.batch.processing.writer;

import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.preAggQueryKey;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.scheduledQueryKey;

import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.SettingAttribute;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Singleton
public class BillingDataPipelineWriter extends EventWriter implements ItemWriter<SettingAttribute> {
  @Autowired private BillingDataPipelineServiceImpl billingDataPipelineService;
  @Autowired private BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Autowired private JobParameters parameters;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends SettingAttribute> settingAttributes) {
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    String accountName = cloudToHarnessMappingService.getAccountNameFromId(accountId);

    List<SettingAttribute> ceConnectorsList = cloudToHarnessMappingService.getSettingAttributes(accountId,
        SettingAttribute.SettingCategory.CE_CONNECTOR.toString(), SettingValue.SettingVariableTypes.CE_AWS.toString(),
        parameters.getLong(CCMJobConstants.JOB_START_DATE), parameters.getLong(CCMJobConstants.JOB_END_DATE));

    ceConnectorsList.forEach(settingAttribute -> {
      String settingId = settingAttribute.getUuid();

      String dataSetId = billingDataPipelineService.createDataSet(accountId, accountName);
      String dataTransferJobName = null;
      HashMap<String, String> scheduledQueryJobsMap = new HashMap<>();
      try {
        dataTransferJobName =
            billingDataPipelineService.createDataTransferJob(dataSetId, settingId, accountId, accountName);
      } catch (IOException e) {
        logger.error("Error while creating GCS -> BQ Transfer Job {}", e);
      }
      try {
        scheduledQueryJobsMap = billingDataPipelineService.createScheduledQueries(dataSetId, accountId, accountName);
      } catch (IOException e) {
        logger.error("Error while creating Scheduled Queries {}", e);
      }

      BillingDataPipelineRecord dataPipelineRecord =
          BillingDataPipelineRecord.builder()
              .accountId(accountId)
              .accountName(accountName)
              .settingId(settingId)
              .dataSetId(dataSetId)
              .dataTransferJobName(dataTransferJobName)
              .fallbackTableScheduledQueryName(scheduledQueryJobsMap.get(scheduledQueryKey))
              .preAggregatedScheduledQueryName(scheduledQueryJobsMap.get(preAggQueryKey))
              .build();
      billingDataPipelineRecordDao.create(dataPipelineRecord);
    });
  }
}
