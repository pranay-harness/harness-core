package io.harness.batch.processing.tasklet;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.cloud.bigquery.datatransfer.v1.TransferRun;
import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.service.intfc.BillingDataPipelineService;
import io.harness.ccm.billing.dao.CloudBillingTransferRunDao;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.ccm.billing.entities.CloudBillingTransferRun;
import io.harness.ccm.billing.entities.TransferJobRunState;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpOrganization;
import io.harness.ccm.config.GcpOrganizationDao;
import io.harness.ccm.config.GcpOrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.io.IOException;
import java.util.List;

@Slf4j
@Singleton
public class GcpBillingDataPipelineTasklet implements Tasklet {
  @Autowired private BatchMainConfig mainConfig;
  @Autowired protected CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private GcpOrganizationDao gcpOrganizationDao;
  @Autowired private GcpOrganizationService gcpOrganizationService;
  @Autowired private BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Autowired private CloudBillingTransferRunDao cloudBillingTransferRunDao;
  @Autowired private BillingDataPipelineService billingDataPipelineService;
  private JobParameters parameters;
  private static final String COPY_TRANSFER_JOB_NAME_TEMPLATE = "BigQueryCopyTransferJob_%s_%s";
  private static final String GCP_PRE_AGG_QUERY_TEMPLATE = "gcpPreAggQuery_%s_%s";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Account account = cloudToHarnessMappingService.getAccountInfoFromId(accountId);
    String accountName = account.getAccountName();
    String gcpProjectId = mainConfig.getBillingDataPipelineConfig().getGcpProjectId();

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

          String transferJobDisplayName =
              String.format(COPY_TRANSFER_JOB_NAME_TEMPLATE, gcpBqProjectId, gcpBqDatasetId);
          GcpOrganization gcpOrganization = gcpOrganizationDao.get(gcpBillingAccount.getOrganizationSettingId());
          String dstProjectId = gcpProjectId;

          String transferJobResourceName = null;
          try {
            transferJobResourceName = billingDataPipelineService.createDataTransferJobFromBQ(transferJobDisplayName,
                gcpBqProjectId, gcpBqDatasetId, dstProjectId, dstDataSetId, gcpOrganization.getServiceAccountEmail());
          } catch (IOException e) {
            logger.error("Error while creating BQ -> BQ Transfer Job {}", transferJobDisplayName, e);
          }
          try {
            billingDataPipelineService.triggerTransferJobRun(
                transferJobResourceName, gcpOrganization.getServiceAccountEmail());
          } catch (IOException e) {
            logger.error("Error while starting manual run for BQ -> BQ Transfer Job {}", transferJobDisplayName, e);
          }

          String scheduledQueryResourceName = null;
          String scheduledQueryDisplayName = String.format(GCP_PRE_AGG_QUERY_TEMPLATE, gcpBqProjectId, gcpBqDatasetId);
          try {
            scheduledQueryResourceName =
                billingDataPipelineService.createScheduledQueriesForGCP(scheduledQueryDisplayName, dstDataSetId);
          } catch (IOException e) {
            logger.error("Error while creating Scheduled Queries {}", scheduledQueryDisplayName, e);
          }

          String billingDataPipelineRecordId = billingDataPipelineRecordDao.create(
              BillingDataPipelineRecord.builder()
                  .accountId(accountId)
                  .accountName(accountName)
                  .settingId(gcpBillingAccount.getOrganizationSettingId())
                  .cloudProvider(CloudProvider.GCP.name())
                  .dataSetId(dstDataSetId)
                  .dataTransferJobName(transferJobDisplayName)
                  .transferJobResourceName(transferJobResourceName)
                  .gcpBqProjectId(gcpBqProjectId)
                  .gcpBqDatasetId(gcpBqDatasetId)
                  .preAggregatedScheduledQueryName(scheduledQueryDisplayName)
                  .preAggregatedScheduleQueryResourceName(scheduledQueryResourceName)
                  .build());
          try {
            List<TransferRun> transferRunList = billingDataPipelineService.listTransferRuns(
                transferJobResourceName, gcpOrganization.getServiceAccountEmail());
            transferRunList.forEach(transferRun -> {
              cloudBillingTransferRunDao.upsert(CloudBillingTransferRun.builder()
                                                    .accountId(accountId)
                                                    .organizationUuid(gcpBillingAccount.getOrganizationSettingId())
                                                    .billingDataPipelineRecordId(billingDataPipelineRecordId)
                                                    .transferRunResourceName(transferRun.getName())
                                                    .state(TransferJobRunState.PENDING)
                                                    .build());
            });
          } catch (IOException e) {
            logger.error("Error while getting manual runs for BQ -> BQ Transfer Job {}", transferJobDisplayName, e);
          }
        });
    return null;
  }
}
