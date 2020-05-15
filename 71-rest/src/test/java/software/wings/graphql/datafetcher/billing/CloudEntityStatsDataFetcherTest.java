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
import io.harness.ccm.billing.graphql.CloudBillingIdFilter;
import io.harness.ccm.billing.graphql.CloudBillingSortCriteria;
import io.harness.ccm.billing.graphql.CloudEntityGroupBy;
import io.harness.ccm.billing.graphql.CloudSortType;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingEntityDataPoint;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingEntityStatsDTO;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingServiceImpl;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudEntityStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock CloudBillingHelper cloudBillingHelper;
  @Mock PreAggregateBillingServiceImpl preAggregateBillingService;
  @InjectMocks CloudEntityStatsDataFetcher cloudEntityStatsDataFetcher;

  private static final String UN_BLENDED_COST = "unblendedCost";
  private static final String BLENDED_COST = "blendedCost";
  private static final String SERVICE_NAME = "service";
  private static final String CLOUD_PROVIDER = "AWS";
  private static final Double UN_BLENDED_COST_VALUE = 2.0;
  private static final Double BLENDED_COST_VALUE = 1.0;

  private List<CloudBillingAggregate> cloudBillingAggregates = new ArrayList<>();
  private List<CloudBillingFilter> filters = new ArrayList<>();
  private List<CloudBillingGroupBy> groupBy = new ArrayList<>();
  private List<CloudBillingSortCriteria> sort = new ArrayList<>();

  @Before
  public void setup() {
    cloudBillingAggregates.add(getBillingAggregate(BLENDED_COST));
    cloudBillingAggregates.add(getBillingAggregate(UN_BLENDED_COST));
    filters.addAll(Arrays.asList(getCloudProviderFilter(new String[] {CLOUD_PROVIDER})));

    groupBy.addAll(Arrays.asList(getServiceGroupBy()));
    sort.addAll(Arrays.asList(getAscBlended(), getAscTime(), getDescUnBlended()));

    when(preAggregateBillingService.getPreAggregateBillingEntityStats(
             anyString(), anyList(), anyList(), anyList(), anyList(), any(), anyList()))
        .thenReturn(PreAggregateBillingEntityStatsDTO.builder()
                        .stats(Arrays.asList(PreAggregateBillingEntityDataPoint.builder()
                                                 .awsService(SERVICE_NAME)
                                                 .awsBlendedCost(BLENDED_COST_VALUE)
                                                 .awsUnblendedCost(UN_BLENDED_COST_VALUE)
                                                 .build()))
                        .build());
    when(cloudBillingHelper.getCloudProviderTableName(anyString())).thenReturn("CLOUD_PROVIDER_TABLE_NAME");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shoudFetch() {
    PreAggregateBillingEntityStatsDTO data = (PreAggregateBillingEntityStatsDTO) cloudEntityStatsDataFetcher.fetch(
        ACCOUNT1_ID, cloudBillingAggregates, filters, groupBy, sort, 5, 0);
    assertThat(data.getStats()).isNotNull();
    assertThat(data.getStats().get(0).getAwsService()).isEqualTo(SERVICE_NAME);
    assertThat(data.getStats().get(0).getAwsBlendedCost()).isEqualTo(BLENDED_COST_VALUE);
    assertThat(data.getStats().get(0).getAwsUnblendedCost()).isEqualTo(UN_BLENDED_COST_VALUE);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void postFetch() {
    QLData postFetchData =
        cloudEntityStatsDataFetcher.postFetch(ACCOUNT1_ID, groupBy, cloudBillingAggregates, sort, null, 5, true);
    assertThat(postFetchData).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getEntityType() {
    String entityType = cloudEntityStatsDataFetcher.getEntityType();
    assertThat(entityType).isNull();
  }

  private CloudBillingAggregate getBillingAggregate(String columnName) {
    return CloudBillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName(columnName).build();
  }

  private CloudBillingSortCriteria getAscBlended() {
    return CloudBillingSortCriteria.builder()
        .sortOrder(QLSortOrder.ASCENDING)
        .sortType(CloudSortType.awsBlendedCost)
        .build();
  }

  private CloudBillingSortCriteria getDescUnBlended() {
    return CloudBillingSortCriteria.builder()
        .sortOrder(QLSortOrder.DESCENDING)
        .sortType(CloudSortType.awsUnblendedCost)
        .build();
  }

  private CloudBillingSortCriteria getAscTime() {
    return CloudBillingSortCriteria.builder().sortOrder(QLSortOrder.ASCENDING).sortType(CloudSortType.Time).build();
  }

  private CloudBillingGroupBy getServiceGroupBy() {
    CloudBillingGroupBy cloudBillingGroupBy = new CloudBillingGroupBy();
    cloudBillingGroupBy.setEntityGroupBy(CloudEntityGroupBy.awsService);
    return cloudBillingGroupBy;
  }

  private CloudBillingFilter getCloudProviderFilter(String[] cloudProvider) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setCloudProvider(
        CloudBillingIdFilter.builder().operator(QLIdOperator.IN).values(cloudProvider).build());
    return cloudBillingFilter;
  }
}