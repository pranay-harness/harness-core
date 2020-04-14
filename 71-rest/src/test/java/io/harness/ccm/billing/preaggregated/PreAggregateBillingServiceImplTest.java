package io.harness.ccm.billing.preaggregated;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;

public class PreAggregateBillingServiceImplTest extends CategoryTest {
  @Mock BigQuery bigQuery;
  @Mock TableResult tableResult;
  @Mock BigQueryService bigQueryService;
  @Mock PreAggregatedBillingDataHelper dataHelper;
  @InjectMocks PreAggregateBillingServiceImpl preAggregateBillingService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private static final String TABLE_NAME = "tableName";
  private static final String SERVICE_NAME = "service";
  private static final Double COST = 1.0;

  @Before
  public void setup() throws InterruptedException {
    when(bigQueryService.get()).thenReturn(bigQuery);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    when(dataHelper.getQuery(anyList(), anyList(), anyList(), anyList()))
        .thenReturn(PreAggregatedTableSchema.defaultTableName);
    when(dataHelper.convertToPreAggregatesTimeSeriesData(tableResult))
        .thenReturn(PreAggregateBillingTimeSeriesStatsDTO.builder().stats(null).build());
    when(dataHelper.convertToPreAggregatesEntityData(tableResult))
        .thenReturn(
            PreAggregateBillingEntityStatsDTO.builder()
                .stats(Arrays.asList(
                    PreAggregateBillingEntityDataPoint.builder().awsService(SERVICE_NAME).awsBlendedCost(COST).build()))
                .build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateBillingTimeSeriesStats() {
    PreAggregateBillingTimeSeriesStatsDTO stats = preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(
        null, null, null, Collections.emptyList(), TABLE_NAME);
    assertThat(stats.getStats()).isEqualTo(null);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateEntitySeriesStats() {
    PreAggregateBillingEntityStatsDTO stats = preAggregateBillingService.getPreAggregateBillingEntityStats(
        null, null, null, Collections.emptyList(), TABLE_NAME);
    assertThat(stats.getStats()).isNotNull();
    assertThat(stats.getStats().get(0).getAwsService()).isEqualTo(SERVICE_NAME);
    assertThat(stats.getStats().get(0).getAwsBlendedCost()).isEqualTo(COST);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateEntitySeriesStatsNegativeCase() throws InterruptedException {
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(new InterruptedException());
    PreAggregateBillingEntityStatsDTO stats = preAggregateBillingService.getPreAggregateBillingEntityStats(
        null, null, null, Collections.emptyList(), TABLE_NAME);
    assertThat(stats).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateBillingTimeSeriesStatsNegativeCase() throws InterruptedException {
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(new InterruptedException());
    PreAggregateBillingTimeSeriesStatsDTO stats = preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(
        null, null, null, Collections.emptyList(), TABLE_NAME);
    assertThat(stats).isNull();
  }
}