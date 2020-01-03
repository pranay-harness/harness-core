package io.harness.batch.processing.schedule;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RunWith(MockitoJUnitRunner.class)
public class BatchJobRunnerTest extends CategoryTest {
  @InjectMocks private BatchJobRunner batchJobRunner;
  @Mock private BatchJobScheduledDataService batchJobScheduledDataService;

  private static final Instant NOW = Instant.now();

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfAllDependentJobFinished() {
    when(batchJobScheduledDataService.fetchLastBatchJobScheduledTime(BatchJobType.ECS_UTILIZATION))
        .thenReturn(NOW.minus(2, ChronoUnit.DAYS));
    when(batchJobScheduledDataService.fetchLastBatchJobScheduledTime(BatchJobType.K8S_UTILIZATION))
        .thenReturn(NOW.minus(2, ChronoUnit.DAYS));
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished =
        batchJobRunner.checkDependentJobFinished(NOW.minus(3, ChronoUnit.DAYS), batchJobType.getDependentBatchJobs());
    assertThat(jobFinished).isTrue();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfAllDependentJobNotFinished() {
    when(batchJobScheduledDataService.fetchLastBatchJobScheduledTime(BatchJobType.ECS_UTILIZATION))
        .thenReturn(NOW.minus(2, ChronoUnit.DAYS));
    when(batchJobScheduledDataService.fetchLastBatchJobScheduledTime(BatchJobType.K8S_UTILIZATION))
        .thenReturn(NOW.minus(4, ChronoUnit.DAYS));
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished =
        batchJobRunner.checkDependentJobFinished(NOW.minus(3, ChronoUnit.DAYS), batchJobType.getDependentBatchJobs());
    assertThat(jobFinished).isFalse();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfDependentJobFinished() {
    when(batchJobScheduledDataService.fetchLastBatchJobScheduledTime(any(BatchJobType.class))).thenReturn(NOW);
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished = batchJobRunner.checkDependentJobFinished(NOW, batchJobType.getDependentBatchJobs());
    assertThat(jobFinished).isFalse();
  }
}
