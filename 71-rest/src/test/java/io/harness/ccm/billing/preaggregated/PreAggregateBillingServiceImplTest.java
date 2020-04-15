package io.harness.ccm.billing.preaggregated;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.graphql.BillingIdFilter;
import io.harness.ccm.billing.graphql.BillingTimeFilter;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

public class PreAggregateBillingServiceImplTest extends CategoryTest {
  @Mock BigQuery bigQuery;
  @Mock TableResult tableResult;
  @Mock BigQueryService bigQueryService;
  @Mock PreAggregatedBillingDataHelper dataHelper;
  @InjectMocks PreAggregateBillingServiceImpl preAggregateBillingService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private static final long currentMillis = Instant.now().toEpochMilli();
  private static final String TABLE_NAME = "tableName";
  private static final String SERVICE_NAME = "service";
  private static final String CLOUD_PROVIDER = "AWS";
  private static final String BLENDED_COST_LABEL = "Blended Cost";
  private static final String STATS_LABEL = "statsLabel";
  private static final String STATS_VALUE = "statsValue";
  private static final String STATS_DESCRIPTION = "statsDescription";
  private static final long MIN_START_TIME = 0L;
  private static final long MAX_START_TIME = currentMillis;
  private static final Double COST = 1.0;
  private static Calendar calendar1;
  private static Calendar calendar2;
  private List<Condition> conditions = new ArrayList<>();
  private List<Object> groupByObjects = new ArrayList<>();
  private List<CloudBillingFilter> filters = new ArrayList<>();

  @Before
  public void setup() throws InterruptedException {
    calendar1 = new GregorianCalendar(2020, Calendar.JANUARY, 1);
    calendar2 = new GregorianCalendar(2020, Calendar.JANUARY, 31);
    Condition startTimeCondition =
        BinaryCondition.greaterThanOrEq(PreAggregatedTableSchema.startTime, Timestamp.of(calendar1.getTime()));
    Condition endTimeCondition =
        BinaryCondition.lessThanOrEq(PreAggregatedTableSchema.startTime, Timestamp.of(calendar2.getTime()));
    conditions.addAll(Arrays.asList(startTimeCondition, endTimeCondition));

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

    // Setup for Trend Stats Test
    PreAggregatedCostData blendedCostData =
        PreAggregatedCostData.builder().cost(COST).maxStartTime(MIN_START_TIME).minStartTime(MAX_START_TIME).build();
    filters.addAll(Arrays.asList(getCloudProviderFilter(new String[] {CLOUD_PROVIDER}), getPreAggStartTimeFilter(0L),
        getPreAggEndTimeFilter(currentMillis)));

    when(dataHelper.convertToAggregatedCostData(tableResult))
        .thenReturn(PreAggregatedCostDataStats.builder().blendedCost(blendedCostData).build());

    when(dataHelper.getCostBillingStats(blendedCostData, filters, BLENDED_COST_LABEL))
        .thenReturn(QLBillingStatsInfo.builder()
                        .statsValue(STATS_VALUE)
                        .statsDescription(STATS_DESCRIPTION)
                        .statsLabel(STATS_LABEL)
                        .build());
    groupByObjects.add(PreAggregatedTableSchema.usageAccountId);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateBillingTimeSeriesStats() {
    PreAggregateBillingTimeSeriesStatsDTO stats = preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(
        null, groupByObjects, null, Collections.emptyList(), TABLE_NAME);
    assertThat(stats.getStats()).isEqualTo(null);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateEntitySeriesStats() {
    PreAggregateBillingEntityStatsDTO stats = preAggregateBillingService.getPreAggregateBillingEntityStats(
        null, groupByObjects, null, Collections.emptyList(), TABLE_NAME);
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
        null, groupByObjects, null, Collections.emptyList(), TABLE_NAME);
    assertThat(stats).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateBillingTimeSeriesStatsNegativeCase() throws InterruptedException {
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(new InterruptedException());
    PreAggregateBillingTimeSeriesStatsDTO stats = preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(
        null, groupByObjects, null, Collections.emptyList(), TABLE_NAME);
    assertThat(stats).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateBillingTrendStats() {
    PreAggregateBillingTrendStatsDTO stats =
        preAggregateBillingService.getPreAggregateBillingTrendStats(null, conditions, TABLE_NAME, filters);
    assertThat(stats.getBlendedCost()).isNotNull();
    assertThat(stats.getBlendedCost().getStatsValue()).isEqualTo(STATS_VALUE);
    assertThat(stats.getBlendedCost().getStatsLabel()).isEqualTo(STATS_LABEL);
    assertThat(stats.getBlendedCost().getStatsDescription()).isEqualTo(STATS_DESCRIPTION);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getPreAggregateBillingTrendStatsNegativeCase() throws InterruptedException {
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenThrow(new InterruptedException());
    PreAggregateBillingTrendStatsDTO stats =
        preAggregateBillingService.getPreAggregateBillingTrendStats(null, conditions, TABLE_NAME, filters);
    assertThat(stats).isEqualTo(PreAggregateBillingTrendStatsDTO.builder().build());
  }

  private CloudBillingFilter getCloudProviderFilter(String[] cloudProvider) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setCloudProvider(
        BillingIdFilter.builder().operator(QLIdOperator.IN).values(cloudProvider).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getPreAggStartTimeFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableStartTime(
        BillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getPreAggEndTimeFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableEndTime(
        BillingTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(filterTime).build());
    return cloudBillingFilter;
  }
}