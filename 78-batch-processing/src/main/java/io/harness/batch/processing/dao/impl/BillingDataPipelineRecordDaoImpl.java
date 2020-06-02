package io.harness.batch.processing.dao.impl;

import static java.util.Objects.isNull;

import com.google.inject.Inject;

import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
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
  public List<BillingDataPipelineRecord> getAllBillingDataPipelineRecords() {
    return hPersistence.createQuery(BillingDataPipelineRecord.class).asList();
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

  @Override
  public BillingDataPipelineRecord upsert(BillingDataPipelineRecord billingDataPipelineRecord) {
    Query<BillingDataPipelineRecord> query =
        hPersistence.createQuery(BillingDataPipelineRecord.class)
            .filter(BillingDataPipelineRecordKeys.accountId, billingDataPipelineRecord.getAccountId())
            .filter(BillingDataPipelineRecordKeys.settingId, billingDataPipelineRecord.getSettingId());

    UpdateOperations<BillingDataPipelineRecord> updateOperations =
        hPersistence.createUpdateOperations(BillingDataPipelineRecord.class);

    if (!isNull(billingDataPipelineRecord.getDataTransferJobStatus())) {
      updateOperations.set(
          BillingDataPipelineRecordKeys.dataTransferJobStatus, billingDataPipelineRecord.getDataTransferJobStatus());
    }

    if (!isNull(billingDataPipelineRecord.getPreAggregatedScheduledQueryStatus())) {
      updateOperations.set(BillingDataPipelineRecordKeys.preAggregatedScheduledQueryStatus,
          billingDataPipelineRecord.getPreAggregatedScheduledQueryStatus());
    }

    if (!isNull(billingDataPipelineRecord.getAwsFallbackTableScheduledQueryStatus())) {
      updateOperations.set(BillingDataPipelineRecordKeys.awsFallbackTableScheduledQueryStatus,
          billingDataPipelineRecord.getAwsFallbackTableScheduledQueryStatus());
    }

    if (!isNull(billingDataPipelineRecord.getLastSuccessfulS3Sync())) {
      updateOperations.set(
          BillingDataPipelineRecordKeys.lastSuccessfulS3Sync, billingDataPipelineRecord.getLastSuccessfulS3Sync());
    }

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().returnNew(true);
    return hPersistence.upsert(query, updateOperations, findAndModifyOptions);
  }
}
