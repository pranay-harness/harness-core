package io.harness.batch.processing.billing.writer;

import static io.harness.rule.OwnerRule.UTSAV;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.BatchProcessingBaseTest;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RunWith(MockitoJUnitRunner.class)
public class InstanceBillingAggregationDataTaskletTest extends BatchProcessingBaseTest {
  @InjectMocks private InstanceBillingAggregationDataTasklet instanceBillingAggregationDataTasklet;
  @Mock private BillingDataServiceImpl billingDataService;

  @Mock private ChunkContext chunkContext;
  @Mock private StepContext stepContext;
  @Mock private StepExecution stepExecution;
  @Mock private JobParameters parameters;

  private static final String ACCOUNT_ID = "account_id";

  private final Instant END_INSTANT = Instant.now();
  private final Instant START_INSTANT = END_INSTANT.minus(1, ChronoUnit.HOURS);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);

    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_INSTANT.toEpochMilli()));
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_INSTANT.toEpochMilli()));
    when(parameters.getString(CCMJobConstants.BATCH_JOB_TYPE))
        .thenReturn(BatchJobType.INSTANCE_BILLING_AGGREGATION.name());
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testExecute() throws Exception {
    when(billingDataService.cleanPreAggBillingData(any(), any(), any())).thenReturn(true);
    when(billingDataService.generatePreAggBillingData(any(), any(), any())).thenReturn(true);

    RepeatStatus repeatStatus = instanceBillingAggregationDataTasklet.execute(null, chunkContext);

    verify(billingDataService, times(1)).cleanPreAggBillingData(eq(ACCOUNT_ID), eq(START_INSTANT), eq(END_INSTANT));
    verify(billingDataService, times(1)).generatePreAggBillingData(eq(ACCOUNT_ID), eq(START_INSTANT), eq(END_INSTANT));

    assertThat(repeatStatus).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testFailedExecute() throws Exception {
    when(billingDataService.cleanPreAggBillingData(any(), any(), any())).thenReturn(true);
    when(billingDataService.generatePreAggBillingData(any(), any(), any())).thenReturn(false);

    assertThatThrownBy(() -> instanceBillingAggregationDataTasklet.execute(null, chunkContext))
        .hasMessageContaining(format("BatchJobType:%s failed", BatchJobType.INSTANCE_BILLING_AGGREGATION.name()));
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testFailedExecuteCleanData() throws Exception {
    when(billingDataService.cleanPreAggBillingData(any(), any(), any())).thenReturn(false);
    when(billingDataService.generatePreAggBillingData(any(), any(), any())).thenReturn(true);

    assertThatThrownBy(() -> instanceBillingAggregationDataTasklet.execute(null, chunkContext))
        .hasMessageContaining(format("BatchJobType:%s failed", BatchJobType.INSTANCE_BILLING_AGGREGATION.name()));
  }
}