package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.graphql.BillingIdFilter;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudEntityGroupBy;
import io.harness.ccm.billing.graphql.CloudGroupBy;
import io.harness.ccm.billing.graphql.CloudSortCriteria;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingServiceImpl;
import io.harness.ccm.billing.preaggregated.PreAggregateConstants;
import io.harness.ccm.billing.preaggregated.PreAggregateFilterValuesDTO;
import io.harness.ccm.billing.preaggregated.PreAggregatedFilterValuesDataPoint;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CloudFilterValuesDataFetcherTest extends AbstractDataFetcherTest {
  @Mock PreAggregateBillingServiceImpl preAggregateBillingService;
  @InjectMocks @Inject CloudFilterValuesDataFetcher cloudFilterValuesDataFetcher;

  private List<CloudBillingFilter> filters = new ArrayList<>();
  private List<CloudGroupBy> groupBy = new ArrayList<>();
  private List<CloudSortCriteria> sort = new ArrayList<>();
  private static QLEntityData entityData;
  private static final String INSTANCE_TYPE = "instanceType";
  private static final String CLOUD_PROVIDER = "AWS";
  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String TYPE = "type";

  @Before
  public void setup() {
    filters.addAll(Arrays.asList(getCloudProviderFilter(new String[] {CLOUD_PROVIDER}),
        getRegionAwsFilter(new String[] {PreAggregateConstants.nullStringValueConstant}),
        getInstanceTypeAwsFilter(new String[] {INSTANCE_TYPE, PreAggregateConstants.nullStringValueConstant})));

    groupBy.addAll(Arrays.asList(getServiceGroupBy()));

    Set<QLEntityData> awsRegionSet = new HashSet<>();
    entityData = QLEntityData.builder().id(ID).name(NAME).type(TYPE).build();
    awsRegionSet.add(entityData);

    when(preAggregateBillingService.getPreAggregateFilterValueStats(anyList(), anyList(), anyString()))
        .thenReturn(PreAggregateFilterValuesDTO.builder()
                        .data(Arrays.asList(PreAggregatedFilterValuesDataPoint.builder()
                                                .awsRegion(awsRegionSet)
                                                .awsInstanceType(null)
                                                .awsService(null)
                                                .awsUsageType(null)
                                                .awsLinkedAccount(null)
                                                .build()))
                        .build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetch() {
    PreAggregateFilterValuesDTO data =
        (PreAggregateFilterValuesDTO) cloudFilterValuesDataFetcher.fetch(ACCOUNT1_ID, null, filters, groupBy, sort);
    assertThat(data.getData()).isNotNull();
    assertThat(data.getData().get(0).getAwsRegion()).contains(entityData);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void postFetch() {
    QLData postFetchData = cloudFilterValuesDataFetcher.postFetch(ACCOUNT1_ID, groupBy, null);
    assertThat(postFetchData).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getEntityType() {
    String entityType = cloudFilterValuesDataFetcher.getEntityType();
    assertThat(entityType).isNull();
  }

  private CloudGroupBy getServiceGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.awsService);
    return cloudGroupBy;
  }

  private CloudBillingFilter getRegionAwsFilter(String[] region) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setAwsRegion(BillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(region).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getInstanceTypeAwsFilter(String[] instanceType) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setAwsInstanceType(
        BillingIdFilter.builder().operator(QLIdOperator.IN).values(instanceType).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getCloudProviderFilter(String[] cloudProvider) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setCloudProvider(
        BillingIdFilter.builder().operator(QLIdOperator.IN).values(cloudProvider).build());
    return cloudBillingFilter;
  }
}