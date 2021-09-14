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
import io.harness.ccm.commons.entities.batch.BatchJobScheduledData;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BatchJobScheduledDataDaoImplTest extends BatchProcessingTestBase {
  @Inject private BatchJobScheduledDataDaoImpl batchJobScheduledDataDao;

  private final String ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final Instant NOW = Instant.now();
  private final Instant START_INSTANT = NOW.truncatedTo(ChronoUnit.DAYS);
  private final Instant END_INSTANT = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
  private final Instant PREV_START_INSTANT = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchLastBatchJobScheduledData() {
    boolean createFirstEntry =
        batchJobScheduledDataDao.create(batchJobScheduledData(PREV_START_INSTANT, START_INSTANT));
    boolean createSecondEntry = batchJobScheduledDataDao.create(batchJobScheduledData(START_INSTANT, END_INSTANT));
    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(ACCOUNT_ID, BatchJobType.K8S_EVENT);
    assertThat(createFirstEntry).isTrue();
    assertThat(createSecondEntry).isTrue();
    assertThat(batchJobScheduledData.getStartAt()).isEqualTo(START_INSTANT);
    assertThat(batchJobScheduledData.getEndAt()).isEqualTo(END_INSTANT);
  }

  private BatchJobScheduledData batchJobScheduledData(Instant startInstant, Instant endInstant) {
    return new BatchJobScheduledData(ACCOUNT_ID, BatchJobType.K8S_EVENT.name(), 1, startInstant, endInstant);
  }
}
