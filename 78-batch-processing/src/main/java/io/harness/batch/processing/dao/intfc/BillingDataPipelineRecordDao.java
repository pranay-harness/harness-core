package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;

public interface BillingDataPipelineRecordDao {
  boolean create(BillingDataPipelineRecord billingDataPipelineRecord);

  BillingDataPipelineRecord getByMasterAccountId(String masterAccountId);
}
