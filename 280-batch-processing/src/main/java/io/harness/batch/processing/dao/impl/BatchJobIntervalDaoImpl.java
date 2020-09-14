package io.harness.batch.processing.dao.impl;

import com.google.inject.Inject;

import io.harness.batch.processing.dao.intfc.BatchJobIntervalDao;
import io.harness.ccm.cluster.entities.BatchJobInterval;
import io.harness.ccm.cluster.entities.BatchJobInterval.BatchJobIntervalKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class BatchJobIntervalDaoImpl implements BatchJobIntervalDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public boolean create(BatchJobInterval batchJobInterval) {
    return hPersistence.save(batchJobInterval) != null;
  }

  @Override
  public BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType) {
    return hPersistence.createQuery(BatchJobInterval.class)
        .filter(BatchJobIntervalKeys.accountId, accountId)
        .filter(BatchJobIntervalKeys.batchJobType, batchJobType)
        .get();
  }
}
