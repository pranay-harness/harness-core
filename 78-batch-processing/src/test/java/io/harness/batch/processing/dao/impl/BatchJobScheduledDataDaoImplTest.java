package io.harness.batch.processing.dao.impl;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobScheduledData;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RunWith(MockitoJUnitRunner.class)
public class BatchJobScheduledDataDaoImplTest extends WingsBaseTest {
  @Inject private BatchJobScheduledDataDaoImpl batchJobScheduledDataDao;

  private final Instant NOW = Instant.now();
  private final Instant START_INSTANT = NOW.truncatedTo(ChronoUnit.DAYS);
  private final Instant END_INSTANT = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
  private final Instant PREV_START_INSTANT = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testFetchLastBatchJobScheduledData() {
    boolean createFirstEntry =
        batchJobScheduledDataDao.create(batchJobScheduledData(PREV_START_INSTANT, START_INSTANT));
    boolean createSecondEntry = batchJobScheduledDataDao.create(batchJobScheduledData(START_INSTANT, END_INSTANT));
    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(BatchJobType.ECS_EVENT);
    assertThat(createFirstEntry).isTrue();
    assertThat(createSecondEntry).isTrue();
    assertThat(batchJobScheduledData.getStartAt()).isEqualTo(START_INSTANT);
    assertThat(batchJobScheduledData.getEndAt()).isEqualTo(END_INSTANT);
  }

  private BatchJobScheduledData batchJobScheduledData(Instant startInstant, Instant endInstant) {
    return new BatchJobScheduledData(BatchJobType.ECS_EVENT, startInstant, endInstant);
  }
}
