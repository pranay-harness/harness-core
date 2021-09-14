/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData.BatchJobScheduledDataKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.mongodb.morphia.query.Sort;

@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
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
