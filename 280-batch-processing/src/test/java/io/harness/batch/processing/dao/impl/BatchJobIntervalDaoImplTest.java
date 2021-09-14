/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.impl;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.batch.processing.BatchProcessingTestBase;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.batch.BatchJobInterval;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BatchJobIntervalDaoImplTest extends BatchProcessingTestBase {
  @Inject private BatchJobIntervalDaoImpl batchJobIntervalDao;

  private final String ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final ChronoUnit INTERVAL_UNIT = ChronoUnit.HOURS;
  private final long INTERVAL = 1;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchBatchJobInterval() {
    batchJobIntervalDao.create(batchJobInterval(INTERVAL_UNIT, INTERVAL));
    BatchJobInterval batchJobInterval =
        batchJobIntervalDao.fetchBatchJobInterval(ACCOUNT_ID, BatchJobType.K8S_EVENT.name());
    assertThat(batchJobInterval.getInterval()).isEqualTo(INTERVAL);
    assertThat(batchJobInterval.getIntervalUnit()).isEqualTo(INTERVAL_UNIT);
  }

  private BatchJobInterval batchJobInterval(ChronoUnit intervalUnit, long interval) {
    return new BatchJobInterval(ACCOUNT_ID, BatchJobType.K8S_EVENT.name(), intervalUnit, interval);
  }
}
