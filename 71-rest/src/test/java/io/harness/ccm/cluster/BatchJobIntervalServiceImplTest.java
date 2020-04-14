package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.BatchJobIntervalDao;
import io.harness.ccm.cluster.entities.BatchJobInterval;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.time.temporal.ChronoUnit;

public class BatchJobIntervalServiceImplTest extends WingsBaseTest {
  @Mock BatchJobIntervalDao batchJobIntervalDao;
  @Inject @InjectMocks BatchJobIntervalServiceImpl batchJobIntervalService;
  private static final String ACCOUNT_ID = "accountId";
  private static final String BATCH_JOB_TYPE = "INSTANCE_BILLING";

  @Before
  public void setUp() {}

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testIsIntervalUnitHours() {
    when(batchJobIntervalDao.fetchBatchJobInterval(ACCOUNT_ID, BATCH_JOB_TYPE))
        .thenReturn(getTestBatchJobInterval(ChronoUnit.HOURS));
    boolean isIntervalUnitHours = batchJobIntervalService.isIntervalUnitHours(ACCOUNT_ID, BATCH_JOB_TYPE);
    assertThat(isIntervalUnitHours).isEqualTo(true);
    when(batchJobIntervalDao.fetchBatchJobInterval(ACCOUNT_ID, BATCH_JOB_TYPE))
        .thenReturn(getTestBatchJobInterval(ChronoUnit.DAYS));
    isIntervalUnitHours = batchJobIntervalService.isIntervalUnitHours(ACCOUNT_ID, BATCH_JOB_TYPE);
    assertThat(isIntervalUnitHours).isEqualTo(false);
  }

  private BatchJobInterval getTestBatchJobInterval(ChronoUnit unit) {
    return new BatchJobInterval(ACCOUNT_ID, BATCH_JOB_TYPE, unit, 0);
  }
}
