package io.harness.ccm.cluster.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BillingDataPipelineRecordDao {
  @Inject private HPersistence persistence;

  public boolean create(BillingDataPipelineRecord billingDataPipelineRecord) {
    return persistence.save(billingDataPipelineRecord) != null;
  }

  public BillingDataPipelineRecord fetchBillingPipelineMetaDataFromAccountId(String accountId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .get();
  }

  public BillingDataPipelineRecord fetchBillingPipelineRecord(String accountId, String settingId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .filter(BillingDataPipelineRecordKeys.settingId, settingId)
        .get();
  }
}
