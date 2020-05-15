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
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Singleton
public class BillingDataPipelineWriter extends EventWriter implements ItemWriter<SettingAttribute> {
  @Autowired private BillingDataPipelineServiceImpl billingDataPipelineService;
  @Autowired private BillingDataPipelineRecordDao billingDataPipelineRecordDao;

  private JobParameters parameters;
  private static final String PAID_ACCOUNT_TYPE = "PAID";

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends SettingAttribute> settingAttributes) {
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Account accountInfo = cloudToHarnessMappingService.getAccountInfoFromId(accountId);
    String accountName = accountInfo.getAccountName();
    String accountType = getAccountType(accountInfo);

    List<SettingAttribute> ceConnectorsList = cloudToHarnessMappingService.getSettingAttributes(accountId,
        SettingCategory.CE_CONNECTOR, SettingVariableTypes.CE_AWS,
        CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE).toEpochMilli(),
        CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE).toEpochMilli());

    ceConnectorsList.forEach(settingAttribute -> {
      String settingId = settingAttribute.getUuid();
      CEAwsConfig awsConfig = (CEAwsConfig) settingAttribute.getValue();
      String masterAccountId = awsConfig.getAwsMasterAccountId();
      BillingDataPipelineRecord billingDataPipelineRecord =
          billingDataPipelineRecordDao.getByMasterAccountId(masterAccountId);
      if (null == billingDataPipelineRecord) {
        String dataSetId =
            billingDataPipelineService.createDataSet(accountId, accountName, masterAccountId, accountType);
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
                .masterAccountId(masterAccountId)
                .settingId(settingId)
                .dataSetId(dataSetId)
                .dataTransferJobName(dataTransferJobName)
                .fallbackTableScheduledQueryName(scheduledQueryJobsMap.get(scheduledQueryKey))
                .preAggregatedScheduledQueryName(scheduledQueryJobsMap.get(preAggQueryKey))
                .build();
        billingDataPipelineRecordDao.create(dataPipelineRecord);
      }
    });
  }

  private String getAccountType(Account accountInfo) {
    if (accountInfo.getLicenseInfo() != null) {
      return accountInfo.getLicenseInfo().getAccountType();
    }
    return PAID_ACCOUNT_TYPE;
  }
}
