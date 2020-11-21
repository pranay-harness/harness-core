package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.PARENT_TEMPLATE;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.service.intfc.BillingDataPipelineHealthStatusService;
import io.harness.batch.processing.service.intfc.BillingDataPipelineService;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord.BillingDataPipelineRecordBuilder;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;

import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.ListTransferConfigsRequest;
import com.google.cloud.bigquery.datatransfer.v1.TransferConfig;
import com.google.cloud.bigquery.datatransfer.v1.TransferState;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class BillingDataPipelineHealthStatusServiceImpl implements BillingDataPipelineHealthStatusService {
  private BatchMainConfig mainConfig;
  private BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  private BillingDataPipelineService billingDataPipelineService;
  private BatchJobScheduledDataDao batchJobScheduledDataDao;

  @Autowired
  public BillingDataPipelineHealthStatusServiceImpl(BatchMainConfig mainConfig,
      BillingDataPipelineRecordDao billingDataPipelineRecordDao, BillingDataPipelineService billingDataPipelineService,
      BatchJobScheduledDataDao batchJobScheduledDataDao) {
    this.mainConfig = mainConfig;
    this.billingDataPipelineRecordDao = billingDataPipelineRecordDao;
    this.billingDataPipelineService = billingDataPipelineService;
    this.batchJobScheduledDataDao = batchJobScheduledDataDao;
  }

  @Override
  public void processAndUpdateHealthStatus() throws IOException {
    String gcpProjectId = mainConfig.getBillingDataPipelineConfig().getGcpProjectId();
    String parent = String.format(PARENT_TEMPLATE, gcpProjectId);
    DataTransferServiceClient dataTransferServiceClient = billingDataPipelineService.getDataTransferClient();
    updateBillingPipelineRecordsStatus(computeTransferConfigStatusMap(dataTransferServiceClient, parent));
  }

  Map<String, TransferState> computeTransferConfigStatusMap(
      DataTransferServiceClient dataTransferServiceClient, String parent) {
    List<TransferConfig> transferConfigList = new ArrayList<>();
    String nextToken = "";
    ListTransferConfigsRequest.Builder transferConfigsRequest =
        ListTransferConfigsRequest.newBuilder().setParent(parent);

    do {
      transferConfigsRequest.setPageToken(nextToken);
      DataTransferServiceClient.ListTransferConfigsPagedResponse transferConfigsPagedResponse =
          dataTransferServiceClient.listTransferConfigs(transferConfigsRequest.build());
      transferConfigList.addAll(Lists.newArrayList(transferConfigsPagedResponse.iterateAll()));
      nextToken = transferConfigsPagedResponse.getNextPageToken();
    } while (!nextToken.equals(""));

    logFailedTransfers(transferConfigList);

    return transferConfigList.stream().collect(ImmutableMap.toImmutableMap(
        TransferConfig::getDisplayName, TransferConfig::getState, (existing, replacement) -> existing));
  }

  private void logFailedTransfers(List<TransferConfig> transferConfigList) {
    for (TransferConfig transferConfig : transferConfigList) {
      if (transferConfig.getState().getNumber() == 5) {
        log.error("Transfer Failed: {} ", transferConfig.getDisplayName());
      }
    }
  }

  void updateBillingPipelineRecordsStatus(Map<String, TransferState> transferToStatusMap) {
    billingDataPipelineRecordDao.listAllBillingDataPipelineRecords().forEach(billingDataPipelineRecord -> {
      try {
        BillingDataPipelineRecordBuilder billingDataPipelineRecordBuilder =
            BillingDataPipelineRecord.builder()
                .accountId(billingDataPipelineRecord.getAccountId())
                .settingId(billingDataPipelineRecord.getSettingId())
                .dataTransferJobStatus(getTransferStateStringValue(
                    transferToStatusMap, billingDataPipelineRecord.getDataTransferJobName()))
                .preAggregatedScheduledQueryStatus(getTransferStateStringValue(
                    transferToStatusMap, billingDataPipelineRecord.getPreAggregatedScheduledQueryName()));
        if (billingDataPipelineRecord.getCloudProvider().equals(CloudProvider.AWS.name())) {
          billingDataPipelineRecordBuilder
              .awsFallbackTableScheduledQueryStatus(getTransferStateStringValue(
                  transferToStatusMap, billingDataPipelineRecord.getAwsFallbackTableScheduledQueryName()))
              .lastSuccessfulS3Sync(fetchLastSuccessfulS3RunInstant(billingDataPipelineRecord.getAccountId()));
        }
        billingDataPipelineRecordDao.upsert(billingDataPipelineRecordBuilder.build());
      } catch (Exception e) {
        log.error("Failed to update the health status for the account {}", billingDataPipelineRecord.getAccountId(), e);
      }
    });
  }

  private Instant fetchLastSuccessfulS3RunInstant(String accountId) {
    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(accountId, BatchJobType.SYNC_BILLING_REPORT_S3);
    if (null != batchJobScheduledData) {
      return Instant.ofEpochMilli(batchJobScheduledData.getCreatedAt());
    }
    return Instant.MIN;
  }

  protected String getTransferStateStringValue(Map<String, TransferState> transferToStatusMap, String key) {
    return transferToStatusMap.containsKey(key) ? transferToStatusMap.get(key).toString()
                                                : TransferState.UNRECOGNIZED.toString();
  }
}
