package io.harness.ccm.billing.dao;

import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BillingDataPipelineRecordDao {
  @Inject private HPersistence persistence;

  public boolean create(BillingDataPipelineRecord billingDataPipelineRecord) {
    return persistence.save(billingDataPipelineRecord) != null;
  }

  public BillingDataPipelineRecord get(String uuid) {
    return persistence.get(BillingDataPipelineRecord.class, uuid);
  }

  public BillingDataPipelineRecord fetchBillingPipelineMetaDataFromAccountId(String accountId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .get();
  }

  public List<BillingDataPipelineRecord> fetchBillingPipelineRecords(String accountId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .asList();
  }

  public BillingDataPipelineRecord fetchBillingPipelineRecord(String accountId, String settingId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .filter(BillingDataPipelineRecordKeys.settingId, settingId)
        .get();
  }
}
