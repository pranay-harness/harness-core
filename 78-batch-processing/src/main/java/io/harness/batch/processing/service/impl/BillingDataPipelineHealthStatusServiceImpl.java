package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.PARENT_TEMPLATE;

import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;
import com.google.cloud.bigquery.datatransfer.v1.ListTransferConfigsRequest;
import com.google.cloud.bigquery.datatransfer.v1.TransferConfig;
import com.google.cloud.bigquery.datatransfer.v1.TransferState;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.service.intfc.BillingDataPipelineHealthStatusService;
import io.harness.batch.processing.service.intfc.BillingDataPipelineService;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord.BillingDataPipelineRecordBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Singleton
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

    return transferConfigList.stream().collect(ImmutableMap.toImmutableMap(
        TransferConfig::getDisplayName, TransferConfig::getState, (existing, replacement) -> existing));
  }

  void updateBillingPipelineRecordsStatus(Map<String, TransferState> transferToStatusMap) {
    List<BillingDataPipelineRecord> billingDataPipelineRecords =
        billingDataPipelineRecordDao.getAllBillingDataPipelineRecords();
    for (BillingDataPipelineRecord record : billingDataPipelineRecords) {
      BillingDataPipelineRecordBuilder billingDataPipelineRecordBuilder = BillingDataPipelineRecord.builder();
      billingDataPipelineRecordBuilder.accountId(record.getAccountId());
      billingDataPipelineRecordBuilder.settingId(record.getSettingId());
      billingDataPipelineRecordBuilder.dataTransferJobStatus(
          getTransferStateStringValue(transferToStatusMap, record.getDataTransferJobName()));
      billingDataPipelineRecordBuilder.preAggregatedScheduledQueryStatus(
          getTransferStateStringValue(transferToStatusMap, record.getPreAggregatedScheduledQueryName()));
      billingDataPipelineRecordBuilder.awsFallbackTableScheduledQueryStatus(
          getTransferStateStringValue(transferToStatusMap, record.getAwsFallbackTableScheduledQueryName()));
      billingDataPipelineRecordBuilder.lastSuccessfulS3Sync(fetchLastSuccessfulS3RunInstant(record.getAccountId()));
      billingDataPipelineRecordDao.upsert(billingDataPipelineRecordBuilder.build());
    }
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
