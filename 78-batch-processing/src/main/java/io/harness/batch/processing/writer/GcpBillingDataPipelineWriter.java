package io.harness.batch.processing.writer;

import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.preAggQueryKey;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.service.intfc.BillingDataPipelineService;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountDao;
import io.harness.ccm.config.GcpOrganization;
import io.harness.ccm.config.GcpOrganizationDao;
import io.harness.ccm.config.GcpOrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Singleton
public class GcpBillingDataPipelineWriter implements ItemWriter<SettingAttribute> {
  @Autowired private BatchMainConfig mainConfig;
  @Autowired protected CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private GcpBillingAccountDao gcpBillingAccountDao;
  @Autowired private GcpOrganizationDao gcpOrganizationDao;
  @Autowired private GcpOrganizationService gcpOrganizationService;
  @Autowired private BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Autowired private BillingDataPipelineService billingDataPipelineService;
  private JobParameters parameters;
  private static final String COPY_TRANSFER_JOB_NAME_TEMPLATE = "BigQueryCopyTransferJob_%s_%s";
  private static final String GCP_PRE_AGG_QUERY_TEMPLATE = "gcpPreAggQuery_%s_%s";

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends SettingAttribute> gcpBillingAccountList) throws Exception {
    String gcpProjectId = mainConfig.getBillingDataPipelineConfig().getGcpProjectId();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Account account = cloudToHarnessMappingService.getAccountInfoFromId(accountId);
    String accountName = account.getAccountName();

    List<GcpBillingAccount> gcpBillingAccounts =
        cloudToHarnessMappingService.listGcpBillingAccountUpdatedInDuration(accountId,
            CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE).toEpochMilli(),
            CCMJobConstants.getFieldValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE).toEpochMilli());

    gcpBillingAccounts.stream()
        .filter(gcpBillingAccount -> {
          List<BillingDataPipelineRecord> billingDataPipelineRecords =
              billingDataPipelineRecordDao.listByGcpBillingAccountDataset(gcpBillingAccount.getAccountId(),
                  gcpBillingAccount.getBqProjectId(), gcpBillingAccount.getBqDatasetId());
          return isEmpty(billingDataPipelineRecords);
        })
        .forEach(gcpBillingAccount -> {
          String dstDataSetId = billingDataPipelineService.createDataSet(account);
          String gcpBqProjectId = gcpBillingAccount.getBqProjectId();
          String gcpBqDatasetId = gcpBillingAccount.getBqDatasetId();

          String transferJobName = String.format(COPY_TRANSFER_JOB_NAME_TEMPLATE, gcpBqProjectId, gcpBqDatasetId);
          GcpOrganization gcpOrganization = gcpOrganizationDao.get(gcpBillingAccount.getOrganizationSettingId());
          String dstProjectId = gcpProjectId;

          try {
            billingDataPipelineService.createDataTransferJobFromBQ(transferJobName, gcpBqProjectId, gcpBqDatasetId,
                dstProjectId, dstDataSetId, gcpOrganization.getServiceAccountEmail());
          } catch (IOException e) {
            logger.error("Error while creating BQ -> BQ Transfer Job {}", e);
          }
          HashMap<String, String> scheduledQueriesMap = new HashMap<>();
          try {
            String scheduledQueryJobName = String.format(GCP_PRE_AGG_QUERY_TEMPLATE, gcpBqProjectId, gcpBqDatasetId);
            billingDataPipelineService.createScheduledQueriesForGCP(scheduledQueryJobName, dstDataSetId);
            scheduledQueriesMap.put(preAggQueryKey, scheduledQueryJobName);
          } catch (IOException e) {
            logger.error("Error while creating Scheduled Queries {}", e);
          }
          billingDataPipelineRecordDao.create(
              BillingDataPipelineRecord.builder()
                  .accountId(accountId)
                  .accountName(accountName)
                  .settingId(gcpBillingAccount.getOrganizationSettingId())
                  .cloudProvider(CloudProvider.GCP.name())
                  .dataSetId(dstDataSetId)
                  .dataTransferJobName(transferJobName)
                  .gcpBqProjectId(gcpBqProjectId)
                  .gcpBqDatasetId(gcpBqDatasetId)
                  .preAggregatedScheduledQueryName(scheduledQueriesMap.get(preAggQueryKey))
                  .build());
        });
  }
}
