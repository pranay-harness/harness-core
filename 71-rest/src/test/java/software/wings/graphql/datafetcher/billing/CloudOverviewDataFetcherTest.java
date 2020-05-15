package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.CloudBillingAggregate;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingTimeFilter;
import io.harness.ccm.billing.graphql.CloudEntityGroupBy;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingServiceImpl;
import io.harness.ccm.billing.preaggregated.PreAggregateCloudOverviewDataDTO;
import io.harness.ccm.billing.preaggregated.PreAggregateCloudOverviewDataPoint;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CloudOverviewDataFetcherTest extends AbstractDataFetcherTest {
  @Mock CloudBillingHelper cloudBillingHelper;
  @Mock PreAggregateBillingServiceImpl preAggregateBillingService;
  @InjectMocks CloudOverviewDataFetcher cloudOverviewDataFetcher;

  private static final String COST = "cost";
  private static final String NAME = "name";
  private static final Double TREND = 2.44;
  private static final Double TOTAL_COST = 100.0;

  private List<CloudBillingAggregate> cloudBillingAggregates = new ArrayList<>();
  private List<CloudBillingFilter> filters = new ArrayList<>();
  private List<CloudBillingGroupBy> groupBy = new ArrayList<>();
  private PreAggregateCloudOverviewDataDTO QLData;

  @Before
  public void setup() {
    cloudBillingAggregates.add(getBillingAggregate(COST));
    filters.addAll(Arrays.asList(getStartTimeBillingFilter(0L), getEndTimeBillingFilter(Instant.now().toEpochMilli())));
    groupBy.addAll(Arrays.asList(getCloudProviderGroupBy()));
    PreAggregateCloudOverviewDataPoint preAggregateCloudOverviewDataPoint =
        PreAggregateCloudOverviewDataPoint.builder().name(NAME).cost(TOTAL_COST).trend(TREND).build();

    QLData = PreAggregateCloudOverviewDataDTO.builder()
                 .totalCost(TOTAL_COST)
                 .data(Collections.singletonList(preAggregateCloudOverviewDataPoint))
                 .build();

    when(preAggregateBillingService.getPreAggregateBillingOverview(
             anyList(), anyList(), anyList(), anyList(), any(), anyList()))
        .thenReturn(QLData);
    when(cloudBillingHelper.getCloudProviderTableName(anyString())).thenReturn("CLOUD_PROVIDER_TABLE_NAME");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetch() {
    QLData data = cloudOverviewDataFetcher.fetch(ACCOUNT1_ID, cloudBillingAggregates, filters, groupBy, null);
    PreAggregateCloudOverviewDataDTO overviewDataDTO = (PreAggregateCloudOverviewDataDTO) data;
    assertThat(overviewDataDTO.getTotalCost()).isEqualTo(TOTAL_COST);
    assertThat(overviewDataDTO.getData().get(0).getCost()).isEqualTo(TOTAL_COST);
    assertThat(overviewDataDTO.getData().get(0).getName()).isEqualTo(NAME);
    assertThat(overviewDataDTO.getData().get(0).getTrend()).isEqualTo(TREND);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void postFetch() {
    QLData data = cloudOverviewDataFetcher.postFetch(ACCOUNT1_ID, groupBy, cloudBillingAggregates, null, QLData);
    assertThat(data).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getEntityType() {
    String entityType = cloudOverviewDataFetcher.getEntityType();
    assertThat(entityType).isNull();
  }

  private CloudBillingAggregate getBillingAggregate(String columnName) {
    return CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName(columnName).build();
  }

  private CloudBillingFilter getStartTimeBillingFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableStartTime(
        CloudBillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getEndTimeBillingFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableStartTime(
        CloudBillingTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(filterTime).build());
    return cloudBillingFilter;
  }

  private CloudBillingGroupBy getCloudProviderGroupBy() {
    CloudBillingGroupBy cloudBillingGroupBy = new CloudBillingGroupBy();
    cloudBillingGroupBy.setEntityGroupBy(CloudEntityGroupBy.cloudProvider);
    return cloudBillingGroupBy;
  }
}