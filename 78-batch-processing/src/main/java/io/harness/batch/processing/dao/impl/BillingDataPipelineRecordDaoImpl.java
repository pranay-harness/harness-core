package io.harness.batch.processing.dao.impl;

import com.google.inject.Inject;

import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class BillingDataPipelineRecordDaoImpl implements BillingDataPipelineRecordDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public boolean create(BillingDataPipelineRecord billingDataPipelineRecord) {
    return hPersistence.save(billingDataPipelineRecord) != null;
  }

  @Override
  public BillingDataPipelineRecord getByMasterAccountId(String masterAccountId) {
    return hPersistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.masterAccountId, masterAccountId)
        .get();
  }
}
