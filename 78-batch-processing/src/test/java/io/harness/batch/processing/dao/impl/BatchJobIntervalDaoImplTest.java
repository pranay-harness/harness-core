package io.harness.batch.processing.dao.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.entities.BatchJobInterval;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;

import java.time.temporal.ChronoUnit;

@RunWith(MockitoJUnitRunner.class)
public class BatchJobIntervalDaoImplTest extends WingsBaseTest {
  @Inject private BatchJobIntervalDaoImpl batchJobIntervalDao;

  private final String ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final ChronoUnit INTERVAL_UNIT = ChronoUnit.HOURS;
  private final long INTERVAL = 1;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchBatchJobInterval() {
    batchJobIntervalDao.create(batchJobInterval(INTERVAL_UNIT, INTERVAL));
    BatchJobInterval batchJobInterval = batchJobIntervalDao.fetchBatchJobInterval(ACCOUNT_ID, BatchJobType.K8S_EVENT);
    assertThat(batchJobInterval.getInterval()).isEqualTo(INTERVAL);
    assertThat(batchJobInterval.getIntervalUnit()).isEqualTo(INTERVAL_UNIT);
  }

  private BatchJobInterval batchJobInterval(ChronoUnit intervalUnit, long interval) {
    return new BatchJobInterval(ACCOUNT_ID, BatchJobType.K8S_EVENT, intervalUnit, interval);
  }
}
