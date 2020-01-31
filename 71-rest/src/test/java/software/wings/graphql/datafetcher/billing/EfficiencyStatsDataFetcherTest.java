package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLEfficiencyStatsData;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EfficiencyStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock private IdleCostTrendStatsDataFetcher idleCostTrendStatsDataFetcher;
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock QLBillingStatsHelper billingStatsHelper;
  @Mock WingsPersistence wingsPersistence;
  @InjectMocks @Inject EfficiencyStatsDataFetcher efficiencyStatsDataFetcher;

  private BigDecimal TOTAL_COST = BigDecimal.valueOf(200);
  private BigDecimal TOTAL_CPU = BigDecimal.valueOf(100);
  private BigDecimal TOTAL_MEM = BigDecimal.valueOf(100);
  private BigDecimal IDLE_COST = BigDecimal.valueOf(80);
  private BigDecimal CPU_IDLE = BigDecimal.valueOf(40);
  private BigDecimal MEM_IDLE = BigDecimal.valueOf(40);
  private BigDecimal UNALLOCATED_COST = BigDecimal.valueOf(60);
  private BigDecimal CPU_UNALLOCATED = BigDecimal.valueOf(30);
  private BigDecimal MEM_UNALLOCATED = BigDecimal.valueOf(30);
  private Instant END_TIME = Instant.ofEpochMilli(1571509800000l);
  private Instant START_TIME = Instant.ofEpochMilli(1570645800000l);

  @Before
  public void setup() {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(idleCostTrendStatsDataFetcher.getIdleCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLIdleCostData.builder()
                        .totalCost(TOTAL_COST)
                        .totalCpuCost(TOTAL_CPU)
                        .totalMemoryCost(TOTAL_MEM)
                        .idleCost(IDLE_COST)
                        .cpuIdleCost(CPU_IDLE)
                        .memoryIdleCost(MEM_IDLE)
                        .build());
    when(idleCostTrendStatsDataFetcher.getUnallocatedCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLUnallocatedCost.builder()
                        .unallocatedCost(UNALLOCATED_COST)
                        .cpuUnallocatedCost(CPU_UNALLOCATED)
                        .memoryUnallocatedCost(MEM_UNALLOCATED)
                        .build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForAllClusterData() {
    when(wingsPersistence.get(any(), any())).thenReturn(anAccount().withAccountName(ACCOUNT1_ID).build());
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    List<QLBillingDataFilter> filters = createTimeFilterList();
    filters.add(QLBillingDataFilter.builder()
                    .cluster(QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(new String[] {""}).build())
                    .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getEfficiencyScore()).isEqualTo(92);
    assertThat(data.getContext().getContextName()).isEqualTo(ACCOUNT1_ID);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(20.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(60.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(120.0);
    assertThat(data.getResourceBreakdown().get(0).getInfo().getUtilized()).isEqualTo(0.6);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForOneCluster() {
    when(billingStatsHelper.getEntityName(any(), anyString())).thenReturn(CLUSTER1_ID);
    when(idleCostTrendStatsDataFetcher.getUnallocatedCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLUnallocatedCost.builder().build());
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    List<QLBillingDataFilter> filters = createTimeFilterList();
    filters.add(
        QLBillingDataFilter.builder()
            .cluster(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {CLUSTER1_ID}).build())
            .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getEfficiencyScore()).isEqualTo(92);
    assertThat(data.getContext().getContextName()).isEqualTo(CLUSTER1_ID);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(80.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(0.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(120.0);
    assertThat(data.getResourceBreakdown().get(0).getInfo().getUtilized()).isEqualTo(0.6);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForOneNamespace() {
    when(billingStatsHelper.getEntityName(any(), anyString())).thenReturn(NAMESPACE1);
    when(idleCostTrendStatsDataFetcher.getUnallocatedCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLUnallocatedCost.builder().build());
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    List<QLBillingDataFilter> filters = createTimeFilterList();
    filters.add(
        QLBillingDataFilter.builder()
            .namespace(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {NAMESPACE1}).build())
            .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getEfficiencyScore()).isEqualTo(92);
    assertThat(data.getContext().getContextName()).isEqualTo(NAMESPACE1);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(80.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(0.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(120.0);
    assertThat(data.getResourceBreakdown().get(0).getInfo().getUtilized()).isEqualTo(0.6);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForOneWorkload() {
    when(billingStatsHelper.getEntityName(any(), anyString())).thenReturn(WORKLOAD_NAME_ACCOUNT1);
    when(idleCostTrendStatsDataFetcher.getUnallocatedCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLUnallocatedCost.builder().build());
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    List<QLBillingDataFilter> filters = createTimeFilterList();
    filters.add(QLBillingDataFilter.builder()
                    .workloadName(QLIdFilter.builder()
                                      .operator(QLIdOperator.EQUALS)
                                      .values(new String[] {WORKLOAD_NAME_ACCOUNT1})
                                      .build())
                    .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getEfficiencyScore()).isEqualTo(92);
    assertThat(data.getContext().getContextName()).isEqualTo(WORKLOAD_NAME_ACCOUNT1);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(80.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(0.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(120.0);
    assertThat(data.getResourceBreakdown().get(0).getInfo().getUtilized()).isEqualTo(0.6);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForOneApplication() {
    when(idleCostTrendStatsDataFetcher.getUnallocatedCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLUnallocatedCost.builder().build());
    when(wingsPersistence.get(any(), any())).thenReturn(anAccount().withAccountName(ACCOUNT1_ID).build());
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    List<QLBillingDataFilter> filters = createTimeFilterList();
    filters.add(QLBillingDataFilter.builder()
                    .application(QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(new String[] {""}).build())
                    .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getEfficiencyScore()).isEqualTo(92);
    assertThat(data.getContext().getContextName()).isEqualTo(ACCOUNT1_ID);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(80.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(0.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(120.0);
    assertThat(data.getResourceBreakdown().get(0).getInfo().getUtilized()).isEqualTo(0.6);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForOneService() {
    when(billingStatsHelper.getEntityName(any(), anyString())).thenReturn(SERVICE1_ID_APP1_ACCOUNT1);
    when(idleCostTrendStatsDataFetcher.getUnallocatedCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLUnallocatedCost.builder().build());
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    List<QLBillingDataFilter> filters = createTimeFilterList();
    filters.add(QLBillingDataFilter.builder()
                    .service(QLIdFilter.builder()
                                 .operator(QLIdOperator.EQUALS)
                                 .values(new String[] {SERVICE1_ID_APP1_ACCOUNT1})
                                 .build())
                    .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getEfficiencyScore()).isEqualTo(92);
    assertThat(data.getContext().getContextName()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(80.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(0.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(120.0);
    assertThat(data.getResourceBreakdown().get(0).getInfo().getUtilized()).isEqualTo(0.6);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForOneEnv() {
    when(billingStatsHelper.getEntityName(any(), anyString())).thenReturn(ENV1_ID_APP1_ACCOUNT1);
    when(idleCostTrendStatsDataFetcher.getUnallocatedCostData(anyString(), anyList(), anyList()))
        .thenReturn(QLUnallocatedCost.builder().build());
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    List<QLBillingDataFilter> filters = createTimeFilterList();
    filters.add(
        QLBillingDataFilter.builder()
            .environment(
                QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {ENV1_ID_APP1_ACCOUNT1}).build())
            .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getEfficiencyScore()).isEqualTo(92);
    assertThat(data.getContext().getContextName()).isEqualTo(ENV1_ID_APP1_ACCOUNT1);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(80.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(0.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(120.0);
    assertThat(data.getResourceBreakdown().get(0).getInfo().getUtilized()).isEqualTo(0.6);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForDBInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    assertThatThrownBy(()
                           -> efficiencyStatsDataFetcher.fetch(ACCOUNT1_ID, Collections.EMPTY_LIST,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  private List<QLBillingDataFilter> createTimeFilterList() {
    List<QLBillingDataFilter> billingDataFilterList = new ArrayList<>();
    billingDataFilterList.add(
        QLBillingDataFilter.builder()
            .startTime(QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(START_TIME.toEpochMilli()).build())
            .build());
    billingDataFilterList.add(
        QLBillingDataFilter.builder()
            .endTime(QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(END_TIME.toEpochMilli()).build())
            .build());
    return billingDataFilterList;
  }

  private List<QLCCMAggregationFunction> getAggregationList() {
    List<QLCCMAggregationFunction> aggregationFunctionList = new ArrayList<>();
    aggregationFunctionList.add(QLCCMAggregationFunction.builder()
                                    .operationType(QLCCMAggregateOperation.SUM)
                                    .columnName("billingamount")
                                    .build());
    aggregationFunctionList.add(QLCCMAggregationFunction.builder()
                                    .operationType(QLCCMAggregateOperation.SUM)
                                    .columnName("cpuidlecost")
                                    .build());
    aggregationFunctionList.add(QLCCMAggregationFunction.builder()
                                    .operationType(QLCCMAggregateOperation.SUM)
                                    .columnName("memoryidlecost")
                                    .build());
    aggregationFunctionList.add(
        QLCCMAggregationFunction.builder().operationType(QLCCMAggregateOperation.SUM).columnName("idlecost").build());
    aggregationFunctionList.add(QLCCMAggregationFunction.builder()
                                    .operationType(QLCCMAggregateOperation.SUM)
                                    .columnName("cpubillingamount")
                                    .build());
    aggregationFunctionList.add(QLCCMAggregationFunction.builder()
                                    .operationType(QLCCMAggregateOperation.SUM)
                                    .columnName("memorybillingamount")
                                    .build());
    return aggregationFunctionList;
  }
}