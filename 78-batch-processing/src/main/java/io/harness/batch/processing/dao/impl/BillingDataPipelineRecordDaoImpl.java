package io.harness.batch.processing.dao.impl;

import com.google.inject.Inject;

import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class BillingDataPipelineRecordDaoImpl implements BillingDataPipelineRecordDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public boolean create(BillingDataPipelineRecord billingDataPipelineRecord) {
    return hPersistence.save(billingDataPipelineRecord) != null;
  }

  @Override
  public BillingDataPipelineRecord getByMasterAccountId(String accountId, String awsMasterAccountId) {
    return hPersistence.createQuery(BillingDataPipelineRecord.class)
        .field(BillingDataPipelineRecordKeys.accountId)
        .equal(accountId)
        .filter(BillingDataPipelineRecordKeys.awsMasterAccountId, awsMasterAccountId)
        .get();
  }

  @Override
  public BillingDataPipelineRecord getByAccountId(String accountId) {
    return hPersistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .get();
  }

  @Override
  public List<BillingDataPipelineRecord> listByGcpBillingAccountDataset(
      String accountId, String gcpBqProjectId, String gcpBqDatasetId) {
    return hPersistence.createQuery(BillingDataPipelineRecord.class)
        .field(BillingDataPipelineRecordKeys.accountId)
        .equal(accountId)
        .field(BillingDataPipelineRecordKeys.gcpBqProjectId)
        .equal(gcpBqProjectId)
        .field(BillingDataPipelineRecordKeys.gcpBqDatasetId)
        .equal(gcpBqDatasetId)
        .asList();
  }
}
