package io.harness.ccm.cluster.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.ccm.cluster.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.mongodb.morphia.query.Sort;

@Singleton
@OwnedBy(CE)
public class BatchJobScheduledDataDao {
  private final HPersistence hPersistence;

  @Inject
  public BatchJobScheduledDataDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public BatchJobScheduledData fetchLastBatchJobScheduledData(String accountId, String batchJobType) {
    return hPersistence.createQuery(BatchJobScheduledData.class)
        .filter(BatchJobScheduledDataKeys.accountId, accountId)
        .filter(BatchJobScheduledDataKeys.batchJobType, batchJobType)
        .order(Sort.descending(BatchJobScheduledDataKeys.endAt))
        .get();
  }
}
