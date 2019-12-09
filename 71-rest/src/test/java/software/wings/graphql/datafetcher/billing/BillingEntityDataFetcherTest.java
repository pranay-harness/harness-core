package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.OwnerRule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableListData;
import software.wings.security.UserThreadLocal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BillingEntityDataFetcherTest extends AbstractDataFetcherTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock private DataFetcherUtils utils;
  @Inject @InjectMocks BillingStatsEntityDataFetcher billingStatsEntityDataFetcher;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  final int[] count = {0};
  final double[] doubleVal = {0};
  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);

    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenReturn(resultSet);
    resetValues();
    mockResultSet();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingTrendWhenDbIsInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    assertThatThrownBy(()
                           -> billingStatsEntityDataFetcher.fetch(ACCOUNT1_ID, aggregationFunction,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingEntitySeriesDataFetcher() {
    Long filterTime = 0L;

    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLBillingDataFilter> filters = Arrays.asList(makeTimeFilter(filterTime));
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeStartTimeEntityGroupBy(), makeApplicationEntityGroupBy(), makeClusterTypeEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLEntityTableListData data = (QLEntityTableListData) billingStatsEntityDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(filters.get(0).getStartTime().getOperator()).isEqualTo(QLTimeOperator.AFTER);
    assertThat(filters.get(0).getStartTime().getValue()).isEqualTo(filterTime);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getId()).isEqualTo("APP1_ID_ACCOUNT1");
    assertThat(data.getData().get(0).getName()).isEqualTo("APP1_ID_ACCOUNT1");
    assertThat(data.getData().get(0).getType()).isEqualTo("APPID");
    assertThat(data.getData().get(0).getCostTrend()).isEqualTo(BillingStatsDefaultKeys.COSTTREND);
    assertThat(data.getData().get(0).getIdleCost()).isEqualTo(BillingStatsDefaultKeys.IDLECOST);
    assertThat(data.getData().get(0).getTrendType()).isEqualTo(BillingStatsDefaultKeys.TRENDTYPE);
    assertThat(data.getData().get(0).getRegion()).isEqualTo(BillingStatsDefaultKeys.REGION);
    assertThat(data.getData().get(0).getClusterType()).isEqualTo(CLUSTER_TYPE1);
    assertThat(data.getData().get(0).getTotalCost()).isEqualTo(10.0);
    assertThat(data.getData().get(1).getTotalCost()).isEqualTo(11.0);
    assertThat(data.getData().get(2).getTotalCost()).isEqualTo(12.0);
    assertThat(data.getData().get(3).getTotalCost()).isEqualTo(13.0);
    assertThat(data.getData().get(4).getTotalCost()).isEqualTo(14.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingEntitySeriesDataFetcherForClusterInsight() {
    Long filterTime = 0L;

    List<QLCCMAggregationFunction> aggregationFunction =
        Arrays.asList(makeBillingAmtAggregation(), makeIdleCostAggregation());
    List<QLBillingDataFilter> filters = Arrays.asList(makeTimeFilter(filterTime));
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeStartTimeEntityGroupBy(), makeWorkloadTypeEntityGroupBy(), makeWorkloadNameEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLEntityTableListData data = (QLEntityTableListData) billingStatsEntityDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(filters.get(0).getStartTime().getOperator()).isEqualTo(QLTimeOperator.AFTER);
    assertThat(filters.get(0).getStartTime().getValue()).isEqualTo(filterTime);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getWorkloadName()).isEqualTo("WORKLOAD_NAME_ACCOUNT1");
    assertThat(data.getData().get(0).getWorkloadType()).isEqualTo("WORKLOAD_TYPE_ACCOUNT1");
    assertThat(data.getData().get(0).getId()).isEqualTo(BillingStatsDefaultKeys.ENTITYID);
    assertThat(data.getData().get(0).getName()).isEqualTo(BillingStatsDefaultKeys.NAME);
    assertThat(data.getData().get(0).getType()).isEqualTo(BillingStatsDefaultKeys.TYPE);
    assertThat(data.getData().get(0).getCostTrend()).isEqualTo(BillingStatsDefaultKeys.COSTTREND);
    assertThat(data.getData().get(0).getIdleCost()).isEqualTo(BillingStatsDefaultKeys.IDLECOST);
    assertThat(data.getData().get(0).getTrendType()).isEqualTo(BillingStatsDefaultKeys.TRENDTYPE);
    assertThat(data.getData().get(0).getRegion()).isEqualTo(BillingStatsDefaultKeys.REGION);
    assertThat(data.getData().get(0).getClusterType()).isEqualTo(BillingStatsDefaultKeys.CLUSTERTYPE);
    assertThat(data.getData().get(0).getTotalCost()).isEqualTo(10.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingEntitySeriesDataFetcherForIdleCost() {
    Long filterTime = 0L;

    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation(),
        makeIdleCostAggregation(), makeCpuIdleCostAggregation(), makeMemoryIdleCostAggregation());
    String[] clusterValues = new String[] {CLUSTER1_ID};
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeClusterFilter(clusterValues));
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeRegionEntityGroupBy(), makeClusterEntityGroupBy(), makeClusterNameEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLEntityTableListData data = (QLEntityTableListData) billingStatsEntityDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, null);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(aggregationFunction.get(1).getColumnName()).isEqualTo("idlecost");
    assertThat(aggregationFunction.get(1).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(aggregationFunction.get(2).getColumnName()).isEqualTo("cpuidlecost");
    assertThat(aggregationFunction.get(2).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(aggregationFunction.get(3).getColumnName()).isEqualTo("memoryidlecost");
    assertThat(aggregationFunction.get(3).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(filters.get(0).getCluster().getOperator()).isEqualTo(QLIdOperator.EQUALS);
    assertThat(filters.get(0).getCluster().getValues()).isEqualTo(clusterValues);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Time);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.DESCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getRegion()).isEqualTo(REGION1);
    assertThat(data.getData().get(0).getName()).isEqualTo(CLUSTER1_NAME);
    assertThat(data.getData().get(0).getId()).isEqualTo(CLUSTER1_NAME);
    assertThat(data.getData().get(0).getWorkloadName()).isEqualTo(BillingStatsDefaultKeys.WORKLOADNAME);
    assertThat(data.getData().get(0).getWorkloadType()).isEqualTo(BillingStatsDefaultKeys.WORKLOADTYPE);
    assertThat(data.getData().get(0).getIdleCost()).isEqualTo(BillingStatsDefaultKeys.IDLECOST);
    assertThat(data.getData().get(0).getTrendType()).isEqualTo(BillingStatsDefaultKeys.TRENDTYPE);
    assertThat(data.getData().get(0).getClusterType()).isEqualTo(BillingStatsDefaultKeys.CLUSTERTYPE);
    assertThat(data.getData().get(0).getClusterId()).isEqualTo(CLUSTER1_ID);
    assertThat(data.getData().get(0).getTotalCost()).isEqualTo(10.0);
    assertThat(data.getData().get(0).getIdleCost()).isEqualTo(0.0);
    assertThat(data.getData().get(0).getCpuIdleCost()).isEqualTo(0);
    assertThat(data.getData().get(0).getMemoryIdleCost()).isEqualTo(0);
  }

  public QLBillingSortCriteria makeDescByTimeSortingCriteria() {
    return QLBillingSortCriteria.builder().sortOrder(QLSortOrder.DESCENDING).sortType(QLBillingSortType.Time).build();
  }

  public QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  public QLCCMAggregationFunction makeIdleCostAggregation() {
    return QLCCMAggregationFunction.builder().operationType(QLCCMAggregateOperation.SUM).columnName("idlecost").build();
  }

  public QLCCMAggregationFunction makeCpuIdleCostAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("cpuidlecost")
        .build();
  }

  public QLCCMAggregationFunction makeMemoryIdleCostAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("memoryidlecost")
        .build();
  }

  public QLCCMGroupBy makeStartTimeEntityGroupBy() {
    QLCCMEntityGroupBy startTimeGroupBy = QLCCMEntityGroupBy.StartTime;
    return QLCCMGroupBy.builder().entityGroupBy(startTimeGroupBy).build();
  }

  public QLCCMGroupBy makeApplicationEntityGroupBy() {
    QLCCMEntityGroupBy applicationGroupBy = QLCCMEntityGroupBy.Application;
    return QLCCMGroupBy.builder().entityGroupBy(applicationGroupBy).build();
  }

  public QLCCMGroupBy makeWorkloadNameEntityGroupBy() {
    QLCCMEntityGroupBy workloadNameGroupBy = QLCCMEntityGroupBy.WorkloadName;
    return QLCCMGroupBy.builder().entityGroupBy(workloadNameGroupBy).build();
  }

  public QLCCMGroupBy makeWorkloadTypeEntityGroupBy() {
    QLCCMEntityGroupBy workloadTypeGroupBy = QLCCMEntityGroupBy.WorkloadType;
    return QLCCMGroupBy.builder().entityGroupBy(workloadTypeGroupBy).build();
  }

  public QLCCMGroupBy makeClusterTypeEntityGroupBy() {
    QLCCMEntityGroupBy clusterTypeGroupBy = QLCCMEntityGroupBy.ClusterType;
    return QLCCMGroupBy.builder().entityGroupBy(clusterTypeGroupBy).build();
  }

  public QLCCMGroupBy makeClusterEntityGroupBy() {
    QLCCMEntityGroupBy clusterGroupBy = QLCCMEntityGroupBy.Cluster;
    return QLCCMGroupBy.builder().entityGroupBy(clusterGroupBy).build();
  }

  public QLCCMGroupBy makeClusterNameEntityGroupBy() {
    QLCCMEntityGroupBy clusterNameGroupBy = QLCCMEntityGroupBy.ClusterName;
    return QLCCMGroupBy.builder().entityGroupBy(clusterNameGroupBy).build();
  }

  public QLCCMGroupBy makeRegionEntityGroupBy() {
    QLCCMEntityGroupBy regionGroupBy = QLCCMEntityGroupBy.Region;
    return QLCCMGroupBy.builder().entityGroupBy(regionGroupBy).build();
  }

  public QLBillingDataFilter makeTimeFilter(Long filterTime) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  public QLBillingDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getDouble("COST")).thenAnswer((Answer<Double>) invocation -> 10.0 + doubleVal[0]++);
    when(resultSet.getDouble("IDLECOST")).thenAnswer((Answer<Double>) invocation -> 0.0);
    when(resultSet.getDouble("CPUIDLECOST")).thenAnswer((Answer<Double>) invocation -> 0.0);
    when(resultSet.getDouble("MEMORYIDLECOST")).thenAnswer((Answer<Double>) invocation -> 0.0);
    when(resultSet.getInt("TOTALNAMESPACES")).thenAnswer((Answer<Integer>) invocation -> 0);
    when(resultSet.getInt("TOTALWORKLOADS")).thenAnswer((Answer<Integer>) invocation -> 0);
    when(resultSet.getString("APPID")).thenAnswer((Answer<String>) invocation -> APP1_ID_ACCOUNT1);
    when(resultSet.getString("WORKLOADNAME")).thenAnswer((Answer<String>) invocation -> WORKLOAD_NAME_ACCOUNT1);
    when(resultSet.getString("WORKLOADTYPE")).thenAnswer((Answer<String>) invocation -> WORKLOAD_TYPE_ACCOUNT1);
    when(resultSet.getString("CLUSTERTYPE")).thenAnswer((Answer<String>) invocation -> CLUSTER_TYPE1);
    when(resultSet.getString("CLUSTERID")).thenAnswer((Answer<String>) invocation -> CLUSTER1_ID);
    when(resultSet.getString("CLUSTERNAME")).thenAnswer((Answer<String>) invocation -> CLUSTER1_NAME);
    when(resultSet.getString("REGION")).thenAnswer((Answer<String>) invocation -> REGION1);
    when(resultSet.getTimestamp(BillingDataMetaDataFields.STARTTIME.getFieldName(), utils.getDefaultCalendar()))
        .thenAnswer((Answer<Timestamp>) invocation -> {
          calendar[0] = calendar[0] + 3600000;
          return new Timestamp(calendar[0]);
        });
    returnResultSet(5);
  }

  private void returnResultSet(int limit) throws SQLException {
    when(resultSet.next()).then((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      return false;
    });
  }

  private void resetValues() {
    count[0] = 0;
    doubleVal[0] = 0;
  }
}
