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
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudEntityGroupBy;
import io.harness.ccm.billing.graphql.CloudGroupBy;
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
  private List<CloudBillingFilter> filters = new ArrayList<>();
  private List<CloudGroupBy> groupBy = new ArrayList<>();

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

  private CloudBillingFilter getStartTimeGcpBillingFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setStartTime(
        BillingTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getEndTimeGcpBillingFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setEndTime(
        BillingTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(filterTime).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getProductGcpBillingFilter(String[] product) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setProduct(BillingIdFilter.builder().operator(QLIdOperator.EQUALS).values(product).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getProjectGcpBillingFilter(String[] project) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setProject(BillingIdFilter.builder().operator(QLIdOperator.IN).values(project).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getSkuGcpBillingFilter(String[] sku) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setSku(BillingIdFilter.builder().operator(QLIdOperator.NOT_IN).values(sku).build());
    return cloudBillingFilter;
  }

  private CloudBillingFilter getBillingAccountIdGcpBillingFilter(String[] billingAccountId) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setBillingAccountId(
        BillingIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(billingAccountId).build());
    return cloudBillingFilter;
  }

  private CloudGroupBy getProductGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.product);
    return cloudGroupBy;
  }

  private CloudGroupBy getProjectGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.project);
    return cloudGroupBy;
  }

  private CloudGroupBy getProjectIdGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.projectId);
    return cloudGroupBy;
  }

  private CloudGroupBy getProjectNumberGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.projectNumber);
    return cloudGroupBy;
  }

  private CloudGroupBy getSkuGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.sku);
    return cloudGroupBy;
  }

  private CloudGroupBy getSkuIdGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.skuId);
    return cloudGroupBy;
  }

  private CloudGroupBy getUsageAmountGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.usageAmount);
    return cloudGroupBy;
  }

  private CloudGroupBy getUsageUnitGroupBy() {
    CloudGroupBy cloudGroupBy = new CloudGroupBy();
    cloudGroupBy.setEntityGroupBy(CloudEntityGroupBy.usageUnit);
    return cloudGroupBy;
  }
}