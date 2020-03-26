package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.GcpBillingEntityStatsDTO;
import io.harness.ccm.billing.GcpBillingServiceImpl;
import io.harness.ccm.billing.graphql.BillingAggregate;
import io.harness.ccm.billing.graphql.BillingIdFilter;
import io.harness.ccm.billing.graphql.BillingTimeFilter;
import io.harness.ccm.billing.graphql.GcpBillingEntityGroupby;
import io.harness.ccm.billing.graphql.GcpBillingFilter;
import io.harness.ccm.billing.graphql.GcpBillingGroupby;
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
import java.util.Collections;
import java.util.List;

public class GcpBillingEntityStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock GcpBillingServiceImpl gcpBillingServiceImpl;
  @InjectMocks @Inject GcpBillingEntityStatsDataFetcher entityStatsDataFetcher;

  private static final String COST = "cost";
  private static final String DISCOUNT = "discount";
  private static final String PRODUCT = "product";
  private static final String PROJECT = "project";
  private static final String SKU = "sku";
  private static final String BILLING_ACCOUNT_ID = "billingAccountId";

  private List<BillingAggregate> billingAggregates = new ArrayList<>();
  private List<GcpBillingFilter> filters = new ArrayList<>();
  private List<GcpBillingGroupby> groupBy = new ArrayList<>();

  @Before
  public void setup() {
    billingAggregates.add(getBillingAggregate(COST));
    billingAggregates.add(getBillingAggregate(DISCOUNT));
    filters.addAll(Arrays.asList(getStartTimeGcpBillingFilter(0L), getEndTimeGcpBillingFilter(0L),
        getProductGcpBillingFilter(new String[] {PRODUCT}), getProjectGcpBillingFilter(new String[] {PROJECT}),
        getSkuGcpBillingFilter(new String[] {SKU}),
        getBillingAccountIdGcpBillingFilter(new String[] {BILLING_ACCOUNT_ID})));
    groupBy.addAll(Arrays.asList(getProductGroupBy(), getProjectGroupBy(), getProjectIdGroupBy(),
        getProjectNumberGroupBy(), getSkuGroupBy(), getSkuIdGroupBy(), getUsageAmountGroupBy(), getUsageUnitGroupBy()));

    when(gcpBillingServiceImpl.getGcpBillingEntityStats(anyList(), anyList(), anyList()))
        .thenReturn(GcpBillingEntityStatsDTO.builder().build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetchTest() {
    QLData qlData =
        entityStatsDataFetcher.fetch(ACCOUNT1_ID, billingAggregates, filters, groupBy, Collections.emptyList());
    assertThat(qlData instanceof GcpBillingEntityStatsDTO).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getEntityTypeTest() {
    String entityType = entityStatsDataFetcher.getEntityType();
    assertThat(entityType).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void postFetchTest() {
    QLData postFetchData = entityStatsDataFetcher.postFetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
    assertThat(postFetchData).isNull();
  }

  private BillingAggregate getBillingAggregate(String columnName) {
    return BillingAggregate.builder().operationType(QLCCMAggregateOperation.SUM).columnName(columnName).build();
  }

  private GcpBillingFilter getStartTimeGcpBillingFilter(Long filterTime) {
    GcpBillingFilter gcpBillingFilter = new GcpBillingFilter();
    gcpBillingFilter.setStartTime(BillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build());
    return gcpBillingFilter;
  }

  private GcpBillingFilter getEndTimeGcpBillingFilter(Long filterTime) {
    GcpBillingFilter gcpBillingFilter = new GcpBillingFilter();
    gcpBillingFilter.setEndTime(BillingTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(filterTime).build());
    return gcpBillingFilter;
  }

  private GcpBillingFilter getProductGcpBillingFilter(String[] product) {
    GcpBillingFilter gcpBillingFilter = new GcpBillingFilter();
    gcpBillingFilter.setProduct(BillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(product).build());
    return gcpBillingFilter;
  }

  private GcpBillingFilter getProjectGcpBillingFilter(String[] project) {
    GcpBillingFilter gcpBillingFilter = new GcpBillingFilter();
    gcpBillingFilter.setProject(BillingIdFilter.builder().operator(QLIdOperator.IN).values(project).build());
    return gcpBillingFilter;
  }

  private GcpBillingFilter getSkuGcpBillingFilter(String[] sku) {
    GcpBillingFilter gcpBillingFilter = new GcpBillingFilter();
    gcpBillingFilter.setSku(BillingIdFilter.builder().operator(QLIdOperator.NOT_IN).values(sku).build());
    return gcpBillingFilter;
  }

  private GcpBillingFilter getBillingAccountIdGcpBillingFilter(String[] billingAccountId) {
    GcpBillingFilter gcpBillingFilter = new GcpBillingFilter();
    gcpBillingFilter.setBillingAccountId(
        BillingIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(billingAccountId).build());
    return gcpBillingFilter;
  }

  private GcpBillingGroupby getProductGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.product);
    return gcpBillingGroupby;
  }

  private GcpBillingGroupby getProjectGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.project);
    return gcpBillingGroupby;
  }

  private GcpBillingGroupby getProjectIdGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.projectId);
    return gcpBillingGroupby;
  }

  private GcpBillingGroupby getProjectNumberGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.projectNumber);
    return gcpBillingGroupby;
  }

  private GcpBillingGroupby getSkuGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.sku);
    return gcpBillingGroupby;
  }

  private GcpBillingGroupby getSkuIdGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.skuId);
    return gcpBillingGroupby;
  }

  private GcpBillingGroupby getUsageAmountGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.usageAmount);
    return gcpBillingGroupby;
  }

  private GcpBillingGroupby getUsageUnitGroupBy() {
    GcpBillingGroupby gcpBillingGroupby = new GcpBillingGroupby();
    gcpBillingGroupby.setEntityGroupBy(GcpBillingEntityGroupby.usageUnit);
    return gcpBillingGroupby;
  }
}