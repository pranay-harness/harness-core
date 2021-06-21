package io.harness.ccm.cluster.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.batch.BatchJobInterval;
import io.harness.ccm.commons.entities.batch.BatchJobInterval.BatchJobIntervalKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public class BatchJobIntervalDao {
  private final HPersistence hPersistence;

  @Inject
  public BatchJobIntervalDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public BatchJobInterval fetchBatchJobInterval(String accountId, String batchJobType) {
    return hPersistence.createQuery(BatchJobInterval.class)
        .filter(BatchJobIntervalKeys.accountId, accountId)
        .filter(BatchJobIntervalKeys.batchJobType, batchJobType)
        .get();
  }
}
