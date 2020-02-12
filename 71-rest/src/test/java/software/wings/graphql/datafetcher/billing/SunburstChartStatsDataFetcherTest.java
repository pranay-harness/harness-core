package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.mockito.stubbing.Answer;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartData;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstChartDataPoint;
import software.wings.graphql.schema.type.aggregation.billing.QLSunburstGridDataPoint;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SunburstChartStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Inject @InjectMocks SunburstChartStatsDataFetcher sunburstChartStatsDataFetcher;
  @Mock Statement statement;
  @Mock ResultSet resultSet;

  private final BigDecimal TOTAL_COST = new BigDecimal("100.0");
  private final BigDecimal IDLE_COST = new BigDecimal("50.0");
  private final String ROOT_PARENT_ID = "ROOT_PARENT_ID";
  private final String ROOT_PARENT = "";
  private long END_TIME = 1571509800000l;
  private long START_TIME = 1570645800000l;
  final int[] count = {0};

  private static String IDLE_COST_COLUMN = "idlecost";
  private static String TOTAL_COST_COLUMN = "billingamount";

  List<QLCCMAggregationFunction> aggregateFunction;
  List<QLBillingSortCriteria> sort;

  @Before
  public void setup() throws SQLException {
    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenReturn(resultSet);
    resetValues();
    mockResultSet();

    aggregateFunction = Arrays.asList(makeBillingAmtAggregation(), makeIdleCostAggregation());
    sort = Arrays.asList(makeDescTotalCostSort());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetSunburstChartDataForDBInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    assertThatThrownBy(()
                           -> sunburstChartStatsDataFetcher.fetch(ACCOUNT1_ID, Collections.EMPTY_LIST,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetSunburstChartDataLandingPage() {
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeStartTimeFilter(START_TIME));
    filters.add(makeEndTimeFilter(END_TIME));
    List<QLCCMGroupBy> groupBy = Arrays.asList(makeClusterEntityGroupBy(), makeNamespaceEntityGroupBy(),
        makeWorkloadNameEntityGroupBy(), makeClusterTypeEntityGroupBy());

    List<QLSunburstChartDataPoint> sunburstChartData = sunburstChartStatsDataFetcher.getSunburstChartData(
        ACCOUNT1_ID, aggregateFunction, filters, groupBy, sort, true, Collections.emptyMap());
    assertThat(sunburstChartData.get(0).getId()).isEqualTo(ROOT_PARENT_ID);
    assertThat(sunburstChartData.get(1).getId())
        .isEqualTo(CLUSTER1_ID + ":" + NAMESPACE1 + ":" + WORKLOAD_NAME_ACCOUNT1);
    assertThat(sunburstChartData.get(1).getValue()).isEqualTo(TOTAL_COST.doubleValue());
    assertThat(sunburstChartData.get(2).getId()).isEqualTo(CLUSTER1_ID + ":" + NAMESPACE1);
    assertThat(sunburstChartData.get(3).getId()).isEqualTo(CLUSTER1_ID);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetSunburstChartDataClusterView() throws SQLException {
    resetValues();
    mockResultSet();
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeStartTimeFilter(START_TIME));
    filters.add(makeEndTimeFilter(END_TIME));
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeClusterEntityGroupBy(), makeNamespaceEntityGroupBy(), makeWorkloadNameEntityGroupBy(),
            makeCloudServiceNameEntityGroupBy(), makeTaskIdEntityGroupBy(), makeClusterTypeEntityGroupBy());
    QLSunburstChartData sunburstChartData = (QLSunburstChartData) sunburstChartStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregateFunction, filters, groupBy, sort);
    List<QLSunburstChartDataPoint> sunburstChartDataPoints = sunburstChartData.getData();
    assertThat(sunburstChartDataPoints.get(0).getId()).isEqualTo(ROOT_PARENT_ID);
    assertThat(sunburstChartDataPoints.get(0).getParent()).isEqualTo(ROOT_PARENT);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetSunburstApplicationLanding() throws SQLException {
    resetValues();
    mockResultSet();
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeStartTimeFilter(START_TIME));
    filters.add(makeEndTimeFilter(END_TIME));
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeApplicationGroupBy(), makeEnvironmentGroupBy(), makeServiceGroupBy());
    QLSunburstChartData sunburstChartDataValue = (QLSunburstChartData) sunburstChartStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregateFunction, filters, groupBy, sort);
    List<QLSunburstChartDataPoint> sunburstChartData = sunburstChartDataValue.getData();
    assertThat(sunburstChartData.get(0).getId()).isEqualTo(ROOT_PARENT_ID);
    assertThat(sunburstChartData.get(0).getParent()).isEqualTo(ROOT_PARENT);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetSunburstChartData() throws SQLException {
    resetValues();
    mockResultSet();
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeStartTimeFilter(START_TIME));
    filters.add(makeEndTimeFilter(END_TIME));
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeApplicationGroupBy(), makeEnvironmentGroupBy(), makeServiceGroupBy());

    List<QLSunburstChartDataPoint> sunburstChartData = sunburstChartStatsDataFetcher.getSunburstChartData(
        ACCOUNT1_ID, aggregateFunction, filters, groupBy, sort, true, Collections.emptyMap());
    assertThat(sunburstChartData.get(0).getId()).isEqualTo(ROOT_PARENT_ID);
    assertThat(sunburstChartData.get(0).getParent()).isEqualTo(ROOT_PARENT);
    assertThat(sunburstChartData.get(1).getId())
        .isEqualTo(APP1_ID_ACCOUNT1 + ":" + ENV1_ID_APP1_ACCOUNT1 + ":" + SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(sunburstChartData.get(1).getName()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(sunburstChartData.get(1).getParent()).isEqualTo(APP1_ID_ACCOUNT1 + ":" + ENV1_ID_APP1_ACCOUNT1);
    assertThat(sunburstChartData.get(2).getId()).isEqualTo(APP1_ID_ACCOUNT1 + ":" + ENV1_ID_APP1_ACCOUNT1);
    assertThat(sunburstChartData.get(2).getName()).isEqualTo(ENV1_ID_APP1_ACCOUNT1);
    assertThat(sunburstChartData.get(2).getParent()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(sunburstChartData.get(3).getId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(sunburstChartData.get(3).getName()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(sunburstChartData.get(3).getParent()).isEqualTo(ROOT_PARENT_ID);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetSunburstGridData() throws SQLException {
    resetValues();
    mockResultSet();
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeStartTimeFilter(START_TIME));
    filters.add(makeEndTimeFilter(END_TIME));
    List<QLCCMGroupBy> groupBy =
        Arrays.asList(makeClusterEntityGroupBy(), makeNamespaceEntityGroupBy(), makeWorkloadNameEntityGroupBy());
    List<QLSunburstGridDataPoint> sunburstGridData = sunburstChartStatsDataFetcher.getSunburstGridData(
        ACCOUNT1_ID, aggregateFunction, filters, groupBy, sort, false);
    assertThat(sunburstGridData.get(0).getId())
        .isEqualTo(CLUSTER1_ID + ":" + NAMESPACE1 + ":" + WORKLOAD_NAME_ACCOUNT1);
  }

  private QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(TOTAL_COST_COLUMN)
        .build();
  }

  private QLCCMAggregationFunction makeIdleCostAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(IDLE_COST_COLUMN)
        .build();
  }

  private QLCCMGroupBy makeApplicationGroupBy() {
    QLCCMEntityGroupBy appGroupBy = QLCCMEntityGroupBy.Application;
    return QLCCMGroupBy.builder().entityGroupBy(appGroupBy).build();
  }

  private QLCCMGroupBy makeEnvironmentGroupBy() {
    QLCCMEntityGroupBy enveGroupBy = QLCCMEntityGroupBy.Environment;
    return QLCCMGroupBy.builder().entityGroupBy(enveGroupBy).build();
  }

  private QLCCMGroupBy makeServiceGroupBy() {
    QLCCMEntityGroupBy serviceGroupBy = QLCCMEntityGroupBy.Service;
    return QLCCMGroupBy.builder().entityGroupBy(serviceGroupBy).build();
  }

  private QLCCMGroupBy makeNamespaceEntityGroupBy() {
    QLCCMEntityGroupBy namespaceGroupBy = QLCCMEntityGroupBy.Namespace;
    return QLCCMGroupBy.builder().entityGroupBy(namespaceGroupBy).build();
  }

  private QLCCMGroupBy makeClusterEntityGroupBy() {
    QLCCMEntityGroupBy clusterGroupBy = QLCCMEntityGroupBy.Cluster;
    return QLCCMGroupBy.builder().entityGroupBy(clusterGroupBy).build();
  }

  private QLCCMGroupBy makeTaskIdEntityGroupBy() {
    QLCCMEntityGroupBy taskIdGroupBy = QLCCMEntityGroupBy.TaskId;
    return QLCCMGroupBy.builder().entityGroupBy(taskIdGroupBy).build();
  }

  private QLCCMGroupBy makeWorkloadNameEntityGroupBy() {
    QLCCMEntityGroupBy workloadNameGroupBy = QLCCMEntityGroupBy.WorkloadName;
    return QLCCMGroupBy.builder().entityGroupBy(workloadNameGroupBy).build();
  }

  private QLCCMGroupBy makeCloudServiceNameEntityGroupBy() {
    QLCCMEntityGroupBy cloudServiceNameGroupBy = QLCCMEntityGroupBy.CloudServiceName;
    return QLCCMGroupBy.builder().entityGroupBy(cloudServiceNameGroupBy).build();
  }

  private QLCCMGroupBy makeClusterTypeEntityGroupBy() {
    QLCCMEntityGroupBy clusterTypeGroupBy = QLCCMEntityGroupBy.ClusterType;
    return QLCCMGroupBy.builder().entityGroupBy(clusterTypeGroupBy).build();
  }

  private QLBillingDataFilter makeStartTimeFilter(Long filterTime) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  private QLBillingDataFilter makeEndTimeFilter(Long filterTime) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(filterTime).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  private QLBillingSortCriteria makeDescTotalCostSort() {
    return QLBillingSortCriteria.builder().sortOrder(QLSortOrder.DESCENDING).sortType(QLBillingSortType.Amount).build();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getBigDecimal("COST")).thenReturn(TOTAL_COST);
    when(resultSet.getBigDecimal("IDLECOST")).thenReturn(IDLE_COST);
    when(resultSet.getDouble("COST")).thenReturn(TOTAL_COST.doubleValue());
    when(resultSet.getDouble("IDLECOST")).thenReturn(IDLE_COST.doubleValue());
    when(resultSet.getString("CLUSTERID")).thenReturn(CLUSTER1_ID);
    when(resultSet.getString("CLOUDSERVICENAME")).thenReturn(CLOUD_SERVICE_NAME_ACCOUNT1);
    when(resultSet.getString("TASKID")).thenReturn("TASK_ID");
    when(resultSet.getString("NAMESPACE")).thenReturn(NAMESPACE1);
    when(resultSet.getString("WORKLOADNAME")).thenReturn(WORKLOAD_NAME_ACCOUNT1);
    when(resultSet.getString("APPID")).thenAnswer((Answer<String>) invocation -> APP1_ID_ACCOUNT1);
    when(resultSet.getString("ENVID")).thenAnswer((Answer<String>) invocation -> ENV1_ID_APP1_ACCOUNT1);
    when(resultSet.getString("SERVICEID")).thenAnswer((Answer<String>) invocation -> SERVICE1_ID_APP1_ACCOUNT1);
    when(resultSet.next()).thenReturn(true);

    returnResultSet(1);
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
  }
}