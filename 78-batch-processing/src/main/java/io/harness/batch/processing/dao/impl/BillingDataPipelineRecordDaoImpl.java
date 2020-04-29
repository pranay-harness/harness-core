package io.harness.batch.processing.dao.impl;

import com.google.inject.Inject;

import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.persistence.HPersistence;
import org.springframework.beans.factory.annotation.Autowired;

public class BillingDataPipelineRecordDaoImpl implements BillingDataPipelineRecordDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public boolean create(BillingDataPipelineRecord billingDataPipelineRecord) {
    return hPersistence.save(billingDataPipelineRecord) != null;
  }
}
