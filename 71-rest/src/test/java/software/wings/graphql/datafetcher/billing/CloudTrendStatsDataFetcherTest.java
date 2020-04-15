package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.BillingAggregate;
import io.harness.ccm.billing.graphql.BillingIdFilter;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudGroupBy;
import io.harness.ccm.billing.graphql.CloudSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingServiceImpl;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingTrendStatsDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudTrendStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock PreAggregateBillingServiceImpl preAggregateBillingService;
  @InjectMocks @Inject CloudTrendStatsDataFetcher cloudTrendStatsDataFetcher;

  private static final String UN_BLENDED_COST = "unblendedCost";
  private static final String BLENDED_COST = "blendedCost";
  private static final String START_TIME = "startTime";
  private static final String CLOUD_PROVIDER = "AWS";
  private static final String STATS_LABEL = "statsLabel";
  private static final String STATS_VALUE = "statsValue";
  private static final String STATS_DESCRIPTION = "statsDescription";

  private List<BillingAggregate> billingAggregates = new ArrayList<>();
  private List<CloudBillingFilter> filters = new ArrayList<>();
  private List<CloudGroupBy> groupBy = new ArrayList<>();
  private List<CloudSortCriteria> sort = new ArrayList<>();

  @Before
  public void setup() {
    billingAggregates.add(getBillingAggregate(QLCCMAggregateOperation.SUM, BLENDED_COST));
    billingAggregates.add(getBillingAggregate(QLCCMAggregateOperation.SUM, UN_BLENDED_COST));
    billingAggregates.add(getBillingAggregate(QLCCMAggregateOperation.MIN, START_TIME));
    billingAggregates.add(getBillingAggregate(QLCCMAggregateOperation.MAX, START_TIME));
    filters.addAll(Arrays.asList(getCloudProviderFilter(new String[] {CLOUD_PROVIDER})));

    when(preAggregateBillingService.getPreAggregateBillingTrendStats(anyList(), anyList(), any(), anyList()))
        .thenReturn(PreAggregateBillingTrendStatsDTO.builder()
                        .blendedCost(QLBillingStatsInfo.builder()
                                         .statsValue(STATS_VALUE)
                                         .statsLabel(STATS_LABEL)
                                         .statsDescription(STATS_DESCRIPTION)
                                         .build())
                        .build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetch() {
    PreAggregateBillingTrendStatsDTO stats = (PreAggregateBillingTrendStatsDTO) cloudTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, billingAggregates, filters, groupBy, sort);
    assertThat(stats).isNotNull();
    assertThat(stats.getBlendedCost().getStatsValue()).isEqualTo(STATS_VALUE);
    assertThat(stats.getBlendedCost().getStatsLabel()).isEqualTo(STATS_LABEL);
    assertThat(stats.getBlendedCost().getStatsDescription()).isEqualTo(STATS_DESCRIPTION);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testPostFetch() {
    QLData postFetchData = cloudTrendStatsDataFetcher.postFetch(ACCOUNT1_ID, groupBy, billingAggregates, sort, null);
    assertThat(postFetchData).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetEntityType() {
    String entityType = cloudTrendStatsDataFetcher.getEntityType();
    assertThat(entityType).isNull();
  }

  private CloudBillingFilter getCloudProviderFilter(String[] cloudProvider) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setCloudProvider(
        BillingIdFilter.builder().operator(QLIdOperator.IN).values(cloudProvider).build());
    return cloudBillingFilter;
  }

  private BillingAggregate getBillingAggregate(QLCCMAggregateOperation operation, String columnName) {
    return BillingAggregate.builder().operationType(operation).columnName(columnName).build();
  }
}