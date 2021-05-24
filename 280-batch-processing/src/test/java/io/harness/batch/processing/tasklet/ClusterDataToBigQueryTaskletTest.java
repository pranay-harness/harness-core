package io.harness.batch.processing.tasklet;

import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.impl.GoogleCloudStorageServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testsupport.BaseTaskletTest;

import software.wings.security.authentication.BatchQueryConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.repeat.RepeatStatus;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDataToBigQueryTaskletTest extends BaseTaskletTest {
  public static final String BILLING_DATA = "billing_data";
  public static final int BATCH_SIZE = 500;
  @Mock BillingDataServiceImpl billingDataService;
  @Mock private BatchMainConfig config;
  @Mock GoogleCloudStorageServiceImpl googleCloudStorageService;
  @InjectMocks ClusterDataToBigQueryTasklet clusterDataToBigQueryTasklet;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mockChunkContext();

    InstanceBillingData instanceBillingData = InstanceBillingData.builder()
                                                  .startTimestamp(START_TIME_MILLIS)
                                                  .endTimestamp(END_TIME_MILLIS)
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
                                                  .storageBillingAmount(BigDecimal.ZERO)
                                                  .storageActualIdleCost(BigDecimal.ZERO)
                                                  .storageUnallocatedCost(BigDecimal.ZERO)
                                                  .storageUtilizationValue(0D)
                                                  .storageRequest(0D)
                                                  .build();

    when(config.getBatchQueryConfig()).thenReturn(BatchQueryConfig.builder().queryBatchSize(BATCH_SIZE).build());
    when(billingDataService.read(
             ACCOUNT_ID, Instant.ofEpochMilli(START_TIME_MILLIS), Instant.ofEpochMilli(END_TIME_MILLIS), BATCH_SIZE, 0))
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
