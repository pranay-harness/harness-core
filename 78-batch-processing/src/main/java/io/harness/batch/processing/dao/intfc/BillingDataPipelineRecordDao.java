package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.billing.entities.BillingDataPipelineRecord;

import java.util.List;

public interface BillingDataPipelineRecordDao {
  String create(BillingDataPipelineRecord billingDataPipelineRecord);

  BillingDataPipelineRecord getByMasterAccountId(String accountId, String awsMasterAccountId);

  BillingDataPipelineRecord getByAccountId(String accountId);

  BillingDataPipelineRecord getBySettingId(String accountId, String settingId);

  List<BillingDataPipelineRecord> listByGcpBillingAccountDataset(
      String accountId, String gcpBqProjectId, String gcpBqDatasetId);

  List<BillingDataPipelineRecord> listAllBillingDataPipelineRecords();

  BillingDataPipelineRecord upsert(BillingDataPipelineRecord billingDataPipelineRecord);
}
