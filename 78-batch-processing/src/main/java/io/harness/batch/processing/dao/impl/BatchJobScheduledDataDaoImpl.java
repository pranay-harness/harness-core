package io.harness.batch.processing.dao.impl;

import com.google.inject.Inject;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.batch.processing.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class BatchJobScheduledDataDaoImpl implements BatchJobScheduledDataDao {
  @Autowired @Inject private HPersistence hPersistence;

  @Override
  public boolean create(BatchJobScheduledData batchJobScheduledData) {
    return hPersistence.save(batchJobScheduledData) != null;
  }

  @Override
  public BatchJobScheduledData fetchLastBatchJobScheduledData(String accountId, BatchJobType batchJobType) {
    return hPersistence.createQuery(BatchJobScheduledData.class)
        .filter(BatchJobScheduledDataKeys.accountId, accountId)
        .filter(BatchJobScheduledDataKeys.batchJobType, batchJobType)
        .order(Sort.descending(BatchJobScheduledDataKeys.endAt))
        .get();
  }
}
