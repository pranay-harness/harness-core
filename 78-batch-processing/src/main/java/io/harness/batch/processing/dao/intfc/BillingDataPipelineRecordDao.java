package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;

import java.util.List;

public interface BillingDataPipelineRecordDao {
  boolean create(BillingDataPipelineRecord billingDataPipelineRecord);

  BillingDataPipelineRecord getByMasterAccountId(String accountId, String awsMasterAccountId);

  BillingDataPipelineRecord getByAccountId(String accountId);

  List<BillingDataPipelineRecord> listByGcpBillingAccountDataset(
      String accountId, String gcpBqProjectId, String gcpBqDatasetId);
}
