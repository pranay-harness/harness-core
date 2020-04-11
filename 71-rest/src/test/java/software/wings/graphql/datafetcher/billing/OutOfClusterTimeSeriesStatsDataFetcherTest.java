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
import io.harness.ccm.billing.graphql.BillingTimeFilter;
import io.harness.ccm.billing.graphql.OutOfClusterBillingFilter;
import io.harness.ccm.billing.graphql.OutOfClusterEntityGroupBy;
import io.harness.ccm.billing.graphql.OutOfClusterGroupBy;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingServiceImpl;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingTimeSeriesStatsDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OutOfClusterTimeSeriesStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock PreAggregateBillingServiceImpl preAggregateBillingService;
  @InjectMocks @Inject OutOfClusterTimeSeriesStatsDataFetcher outOfClusterTimeSeriesStatsDataFetcher;

  private static final String COST = "unblendedCost";
  private static final String DISCOUNT = "blendedCost";
  private static final String SERVICE = "service";
  private static final String LINKED_ACCOUNT = "linkedAccount";
  private static final String USAGE_TYPE = "usageType";
  private static final String INSTANCE_TYPE = "instanceType";
  private static final String CLOUD_PROVIDER = "AWS";

  private List<BillingAggregate> billingAggregates = new ArrayList<>();
  private List<OutOfClusterBillingFilter> filters = new ArrayList<>();
  private List<OutOfClusterGroupBy> groupBy = new ArrayList<>();

  @Before
  public void setup() {
    billingAggregates.add(getBillingAggregate(COST));
    billingAggregates.add(getBillingAggregate(DISCOUNT));
    filters.addAll(Arrays.asList(getStartTimeAwsBillingFilter(0L), getServiceAwsFilter(new String[] {SERVICE}),
        getLinkedAccountsAwsFilter(new String[] {LINKED_ACCOUNT}), getUsageTypeAwsFilter(new String[] {USAGE_TYPE}),
        getAwsRegionFilter(new String[] {INSTANCE_TYPE}), getCloudProviderFilter(new String[] {CLOUD_PROVIDER}),
        getInstanceTypeAwsFilter(new String[] {INSTANCE_TYPE})));

    groupBy.addAll(Arrays.asList(getServiceGroupBy(), getLinkedAccountsGroupBy(), getInstanceTypeGroupBy(),
        getUsageTypeGroupBy(), getAwsRegionGroupBy()));

    when(preAggregateBillingService.getPreAggregateBillingTimeSeriesStats(anyList(), anyList(), anyList(), any()))
        .thenReturn(PreAggregateBillingTimeSeriesStatsDTO.builder().build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testTimeSeriesDataFetcher() {
    QLData data =
        outOfClusterTimeSeriesStatsDataFetcher.fetch(ACCOUNT1_ID, billingAggregates, filters, groupBy, null, 5, 0);
    assertThat(data).isEqualTo(PreAggregateBillingTimeSeriesStatsDTO.builder().build());
  }

  private BillingAggregate getBillingAggregate(String columnName) {
    return BillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName(columnName).build();
  }

  private OutOfClusterBillingFilter getStartTimeAwsBillingFilter(Long filterTime) {
    OutOfClusterBillingFilter outOfClusterBillingFilter = new OutOfClusterBillingFilter();
    outOfClusterBillingFilter.setAwsStartTime(
        BillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build());
    return outOfClusterBillingFilter;
  }

  private OutOfClusterBillingFilter getServiceAwsFilter(String[] service) {
    OutOfClusterBillingFilter outOfClusterBillingFilter = new OutOfClusterBillingFilter();
    outOfClusterBillingFilter.setService(
        BillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(service).build());
    return outOfClusterBillingFilter;
  }

  private OutOfClusterBillingFilter getLinkedAccountsAwsFilter(String[] linkedAccounts) {
    OutOfClusterBillingFilter outOfClusterBillingFilter = new OutOfClusterBillingFilter();
    outOfClusterBillingFilter.setLinkedAccount(
        BillingIdFilter.builder().operator(QLIdOperator.IN).values(linkedAccounts).build());
    return outOfClusterBillingFilter;
  }

  private OutOfClusterBillingFilter getUsageTypeAwsFilter(String[] usageType) {
    OutOfClusterBillingFilter outOfClusterBillingFilter = new OutOfClusterBillingFilter();
    outOfClusterBillingFilter.setUsageType(
        BillingIdFilter.builder().operator(QLIdOperator.NOT_IN).values(usageType).build());
    return outOfClusterBillingFilter;
  }

  private OutOfClusterBillingFilter getInstanceTypeAwsFilter(String[] instanceType) {
    OutOfClusterBillingFilter outOfClusterBillingFilter = new OutOfClusterBillingFilter();
    outOfClusterBillingFilter.setInstanceType(
        BillingIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(instanceType).build());
    return outOfClusterBillingFilter;
  }

  private OutOfClusterBillingFilter getAwsRegionFilter(String[] region) {
    OutOfClusterBillingFilter outOfClusterBillingFilter = new OutOfClusterBillingFilter();
    outOfClusterBillingFilter.setAwsRegion(
        BillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(region).build());
    return outOfClusterBillingFilter;
  }

  private OutOfClusterBillingFilter getCloudProviderFilter(String[] cloudProvider) {
    OutOfClusterBillingFilter outOfClusterBillingFilter = new OutOfClusterBillingFilter();
    outOfClusterBillingFilter.setCloudProvider(
        BillingIdFilter.builder().operator(QLIdOperator.IN).values(cloudProvider).build());
    return outOfClusterBillingFilter;
  }

  private OutOfClusterGroupBy getServiceGroupBy() {
    OutOfClusterGroupBy outOfClusterGroupBy = new OutOfClusterGroupBy();
    outOfClusterGroupBy.setEntityGroupBy(OutOfClusterEntityGroupBy.service);
    return outOfClusterGroupBy;
  }

  private OutOfClusterGroupBy getLinkedAccountsGroupBy() {
    OutOfClusterGroupBy outOfClusterGroupBy = new OutOfClusterGroupBy();
    outOfClusterGroupBy.setEntityGroupBy(OutOfClusterEntityGroupBy.likedAccount);
    return outOfClusterGroupBy;
  }

  private OutOfClusterGroupBy getUsageTypeGroupBy() {
    OutOfClusterGroupBy outOfClusterGroupBy = new OutOfClusterGroupBy();
    outOfClusterGroupBy.setEntityGroupBy(OutOfClusterEntityGroupBy.usageType);
    return outOfClusterGroupBy;
  }

  private OutOfClusterGroupBy getInstanceTypeGroupBy() {
    OutOfClusterGroupBy outOfClusterGroupBy = new OutOfClusterGroupBy();
    outOfClusterGroupBy.setEntityGroupBy(OutOfClusterEntityGroupBy.instanceType);
    return outOfClusterGroupBy;
  }

  private OutOfClusterGroupBy getAwsRegionGroupBy() {
    OutOfClusterGroupBy outOfClusterGroupBy = new OutOfClusterGroupBy();
    outOfClusterGroupBy.setEntityGroupBy(OutOfClusterEntityGroupBy.awsRegion);
    return outOfClusterGroupBy;
  }
}