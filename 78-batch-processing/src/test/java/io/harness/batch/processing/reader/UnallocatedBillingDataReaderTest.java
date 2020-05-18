package io.harness.batch.processing.reader;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.batch.processing.billing.timeseries.service.impl.UnallocatedBillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.UnallocatedCostData;
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
import software.wings.WingsBaseTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(MockitoJUnitRunner.class)
public class UnallocatedBillingDataReaderTest extends WingsBaseTest {
  @Inject @InjectMocks private UnallocatedBillingDataReader unallocatedBillingDataReader;
  @Mock private UnallocatedBillingDataServiceImpl unallocatedBillingDataService;
  @Mock private JobParameters parameters;
  @Mock private AtomicBoolean runOnlyOnce;

  private final Instant NOW = Instant.now();
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();
  private static final String ACCOUNT_ID = "accountId";
  private final String CLUSTER_ID = "clusterId";
  private final String INSTANCE_TYPE = "instanceType";
  private final double COST = 2.0;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(unallocatedBillingDataService.getUnallocatedCostData(
             ACCOUNT_ID, START_TIME_MILLIS, END_TIME_MILLIS, BatchJobType.UNALLOCATED_BILLING_HOURLY))
        .thenReturn(Collections.singletonList(UnallocatedCostData.builder()
                                                  .clusterId(CLUSTER_ID)
                                                  .instanceType(INSTANCE_TYPE)
                                                  .cost(COST)
                                                  .cpuCost(COST / 2)
                                                  .memoryCost(COST / 2)
                                                  .startTime(START_TIME_MILLIS)
                                                  .endTime(END_TIME_MILLIS)
                                                  .build()));
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.BATCH_JOB_TYPE))
        .thenReturn(BatchJobType.UNALLOCATED_BILLING_HOURLY.name());
    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));
    runOnlyOnce = new AtomicBoolean(false);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testUnallocatedBillingDataReader() {
    List<UnallocatedCostData> list = unallocatedBillingDataReader.read();
    assertThat(list.get(0).getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(list.get(0).getInstanceType()).isEqualTo(INSTANCE_TYPE);
    assertThat(list.get(0).getCost()).isEqualTo(COST);
    assertThat(list.get(0).getCpuCost()).isEqualTo(COST / 2);
    assertThat(list.get(0).getMemoryCost()).isEqualTo(COST / 2);
    assertThat(list.get(0).getStartTime()).isEqualTo(START_TIME_MILLIS);
    assertThat(list.get(0).getEndTime()).isEqualTo(END_TIME_MILLIS);
    assertThat(unallocatedBillingDataReader.read()).isNull();
  }
}
