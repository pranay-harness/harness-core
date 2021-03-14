package io.harness.ccm.cluster.dao;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.ccm.cluster.entities.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.mongodb.morphia.query.Sort;

@Singleton
@TargetModule(Module._375_CE_GRAPHQL)
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
