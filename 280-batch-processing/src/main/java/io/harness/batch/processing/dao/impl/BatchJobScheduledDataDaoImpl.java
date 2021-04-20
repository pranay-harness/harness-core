package io.harness.batch.processing.dao.impl;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.ccm.cluster.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.ccm.commons.entities.CEDataCleanupRequest;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
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
    Query<BatchJobScheduledData> query = hPersistence.createQuery(BatchJobScheduledData.class)
                                             .filter(BatchJobScheduledDataKeys.accountId, accountId)
                                             .filter(BatchJobScheduledDataKeys.batchJobType, batchJobType.name());
    query.or(query.criteria(BatchJobScheduledDataKeys.validRun).doesNotExist(),
        query.criteria(BatchJobScheduledDataKeys.validRun).equal(true));
    query.order(Sort.descending(BatchJobScheduledDataKeys.endAt));
    return query.get();
  }

  public void invalidateJobs(CEDataCleanupRequest ceDataCleanupRequest) {
    Query<BatchJobScheduledData> query =
        hPersistence.createQuery(BatchJobScheduledData.class)
            .filter(BatchJobScheduledDataKeys.batchJobType, ceDataCleanupRequest.getBatchJobType());

    if (ceDataCleanupRequest.getAccountId() != null) {
      query.criteria(BatchJobScheduledDataKeys.accountId).equal(ceDataCleanupRequest.getAccountId());
    }

    if (ceDataCleanupRequest.getStartAt() != null) {
      query.criteria(BatchJobScheduledDataKeys.startAt).greaterThanOrEq(ceDataCleanupRequest.getStartAt());
    }

    UpdateOperations<BatchJobScheduledData> updateOperations =
        hPersistence.createUpdateOperations(BatchJobScheduledData.class);
    updateOperations.set(BatchJobScheduledDataKeys.validRun, false);
    updateOperations.set(BatchJobScheduledDataKeys.comments, ceDataCleanupRequest.getUuid());
    log.info("Query to invalidate jobs {}", query);
    hPersistence.update(query, updateOperations);
  }

  @Override
  public void invalidateJobs(String accountId, List<String> batchJobTypes, Instant instant) {
    Query<BatchJobScheduledData> query = hPersistence.createQuery(BatchJobScheduledData.class)
                                             .filter(BatchJobScheduledDataKeys.accountId, accountId)
                                             .field(BatchJobScheduledDataKeys.batchJobType)
                                             .in(batchJobTypes)
                                             .field(BatchJobScheduledDataKeys.startAt)
                                             .greaterThan(instant);

    UpdateOperations<BatchJobScheduledData> updateOperations =
        hPersistence.createUpdateOperations(BatchJobScheduledData.class);
    updateOperations.set(BatchJobScheduledDataKeys.validRun, false);
    hPersistence.update(query, updateOperations);
  }
}
