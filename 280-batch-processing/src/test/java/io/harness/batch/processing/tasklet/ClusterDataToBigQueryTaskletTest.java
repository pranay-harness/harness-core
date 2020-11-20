package io.harness.batch.processing.tasklet;

import static io.harness.batch.processing.ccm.CCMJobConstants.ACCOUNT_ID;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.impl.GoogleCloudStorageServiceImpl;
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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import software.wings.security.authentication.BatchQueryConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDataToBigQueryTaskletTest extends CategoryTest {
  public static final String BILLING_DATA = "billing_data";
  public static final int BATCH_SIZE = 500;
  @Mock GoogleCloudStorageServiceImpl googleCloudStorageService;
  @Mock BillingDataServiceImpl billingDataService;
  @Mock private BatchMainConfig config;
  @InjectMocks ClusterDataToBigQueryTasklet clusterDataToBigQueryTasklet;

  private static final Instant instant = Instant.now();
  private static final long startTime = instant.toEpochMilli();
  private static final long endTime = instant.plus(1, ChronoUnit.DAYS).toEpochMilli();
  private ChunkContext chunkContext;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Map<String, JobParameter> parameters = new HashMap<>();
    parameters.put(CCMJobConstants.JOB_START_DATE, new JobParameter(String.valueOf(startTime), true));
    parameters.put(CCMJobConstants.JOB_END_DATE, new JobParameter(String.valueOf(endTime), true));
    parameters.put(ACCOUNT_ID, new JobParameter(ACCOUNT_ID, true));
    JobParameters jobParameters = new JobParameters(parameters);
    StepExecution stepExecution = new StepExecution("clusterDataToBigQueryStep", new JobExecution(0L, jobParameters));
    chunkContext = new ChunkContext(new StepContext(stepExecution));

    InstanceBillingData instanceBillingData = InstanceBillingData.builder()
                                                  .startTimestamp(startTime)
                                                  .endTimestamp(endTime)
                                                  .accountId(ACCOUNT_ID)
                                                  .instanceId("instanceId")
                                                  .instanceType("instanceType")
                                                  .billingAmount(BigDecimal.ZERO)
                                                  .cpuBillingAmount(BigDecimal.ZERO)
                                                  .memoryBillingAmount(BigDecimal.ZERO)
                                                  .idleCost(BigDecimal.ZERO)
                                                  .cpuIdleCost(BigDecimal.ZERO)
                                                  .memoryIdleCost(BigDecimal.ZERO)
                                                  .systemCost(BigDecimal.ZERO)
                                                  .cpuSystemCost(BigDecimal.ZERO)
                                                  .memorySystemCost(BigDecimal.ZERO)
                                                  .actualIdleCost(BigDecimal.ZERO)
                                                  .cpuActualIdleCost(BigDecimal.ZERO)
                                                  .memoryActualIdleCost(BigDecimal.ZERO)
                                                  .unallocatedCost(BigDecimal.ZERO)
                                                  .cpuUnallocatedCost(BigDecimal.ZERO)
                                                  .memoryUnallocatedCost(BigDecimal.ZERO)
                                                  .build();

    when(config.getBatchQueryConfig()).thenReturn(BatchQueryConfig.builder().queryBatchSize(BATCH_SIZE).build());
    when(billingDataService.read(
             ACCOUNT_ID, Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime), BATCH_SIZE, 0))
        .thenReturn(Collections.singletonList(instanceBillingData));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldExecute() throws Exception {
    RepeatStatus execute = clusterDataToBigQueryTasklet.execute(null, chunkContext);
    assertThat(execute).isNull();
  }
}
