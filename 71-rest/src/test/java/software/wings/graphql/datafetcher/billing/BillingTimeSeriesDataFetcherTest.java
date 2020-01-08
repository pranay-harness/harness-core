package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.ROHIT;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
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
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLAggregationKind;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLData;
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
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLTimeGroupType;
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

public class BillingTimeSeriesDataFetcherTest extends AbstractDataFetcherTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock private DataFetcherUtils utils;
  @Mock QLBillingStatsHelper statsHelper;
  @Inject @InjectMocks BillingStatsTimeSeriesDataFetcher billingStatsTimeSeriesDataFetcher;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  final int[] count = {0};
  final double[] doubleVal = {0};
  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};
  private List<QLCCMAggregationFunction> aggregationFunction;

  private static String IDLECOST = "idlecost";
  private static String CPUIDLECOST = "cpuidlecost";
  private static String MEMORYIDLECOST = "memoryidlecost";
  private static String MAXCPUUTILIZATION = "maxcpuutilization";
  private static String MAXMEMORYUTILIZATION = "maxmemoryutilization";
  private static String AVGCPUUTILIZATION = "avgcpuutilization";
  private static String AVGMEMORYUTILIZATION = "avgmemoryutilization";

  private static double IDLECOST_VALUE = 2.0;
  private static double CPUIDLECOST_VALUE = 2.0;
  private static double MEMORYIDLECOST_VALUE = 2.0;
  private static double MAXCPUUTILIZATION_VALUE = 0.5;
  private static double MAXMEMORYUTILIZATION_VALUE = 0.5;
  private static double AVGCPUUTILIZATION_VALUE = 0.5;
  private static double AVGMEMORYTILIZATION_VALUE = 0.5;

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);
    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
    aggregationFunction = Collections.singletonList(makeBillingAmtAggregation());

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
    assertThatThrownBy(()
                           -> billingStatsTimeSeriesDataFetcher.fetch(ACCOUNT1_ID, aggregationFunction,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetBillingTrendWhenQueryThrowsException() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    Statement mockStatement = mock(Statement.class);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException());

    QLData data = billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcher() {
    String[] appIdFilterValues = new String[] {APP1_ID_ACCOUNT1};

    List<QLBillingDataFilter> filters = Arrays.asList(makeApplicationFilter(appIdFilterValues), makeTimeFilter(0L));
    List<QLCCMGroupBy> groupBy = Arrays.asList(makeApplicationEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(filters.get(0).getApplication().getValues()).isEqualTo(appIdFilterValues);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("APPID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetchMethodWithMultipleAggregationInBillingTimeSeriesDataFetcher() {
    String[] appIdFilterValues = new String[] {APP1_ID_ACCOUNT1};
    List<QLCCMAggregationFunction> multiAggregationFunctionForIdleCost =
        Arrays.asList(makeIdleCostAggregation(), makeCpuIdleCostAggregation(), makeMemoryIdleCostAggregation(),
            makeMaxCpuUtilizationAggregation(), makeAvgCpuUtilizationAggregation(),
            makeMaxMemoryUtilizationAggregation(), makeAvgMemoryUtilizationAggregation());
    List<QLBillingDataFilter> filters = Arrays.asList(makeApplicationFilter(appIdFilterValues), makeTimeFilter(0L));
    List<QLCCMGroupBy> groupBy = Arrays.asList(makeApplicationEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList();

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, multiAggregationFunctionForIdleCost, filters, groupBy, sortCriteria);

    assertThat(multiAggregationFunctionForIdleCost.get(0).getColumnName()).isEqualTo("idlecost");
    assertThat(multiAggregationFunctionForIdleCost.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(filters.get(0).getApplication().getValues()).isEqualTo(appIdFilterValues);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("APPID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherForClusterInsight() {
    String[] cloudServiceNameFilterValues = new String[] {CLOUD_SERVICE_NAME_ACCOUNT1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeCloudServiceNameEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters =
        Collections.singletonList(makeCloudServiceNameFilter(cloudServiceNameFilterValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(CLOUD_SERVICE_NAME_ACCOUNT1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("CLOUDSERVICENAME");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherServiceQuery() {
    String[] serviceValues = new String[] {SERVICE1_ID_APP1_ACCOUNT1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeServiceEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = Collections.singletonList(makeServiceFilter(serviceValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(SERVICE1_ID_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("SERVICEID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherEnvironmentQuery() {
    String[] environmentValues = new String[] {SERVICE1_ID_APP1_ACCOUNT1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeEnvironmentEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = Collections.singletonList(makeEnvironmentFilter(environmentValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(ENV1_ID_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("ENVID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherClusterQuery() {
    String[] clusterValues = new String[] {CLUSTER1_ID};
    String[] workloadNameValues = new String[] {WORKLOAD_NAME_ACCOUNT1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeClusterEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeClusterFilter(clusterValues));
    filters.add(makeWorkloadNameFilter(workloadNameValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(CLUSTER1_ID);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("CLUSTERID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testNoneGroupByInClusterViewCharts() {
    List<QLCCMGroupBy> groupBy = Collections.singletonList(makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeNotNullClusterFilter());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherInstanceIdQuery() {
    String[] instanceIdValues = new String[] {INSTANCE1_SERVICE1_ENV1_APP1_ACCOUNT1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeInstanceIdEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = Collections.singletonList(makeTaskIdFilter(instanceIdValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId())
        .isEqualTo(INSTANCE1_SERVICE1_ENV1_APP1_ACCOUNT1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("TASKID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherLaunchTypeQuery() {
    String[] launchTypeValues = new String[] {LAUNCH_TYPE1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeLaunchTypeEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = Collections.singletonList(makeLaunchTypeFilter(launchTypeValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(LAUNCH_TYPE1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("LAUNCHTYPE");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherRegionQuery() {
    String[] instanceTypeValues = new String[] {INSTANCE_TYPE1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeRegionEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = Collections.singletonList(makeInstanceTypeFilter(instanceTypeValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(REGION1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("REGION");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherNamespaceQuery() {
    String[] namespaceValues = new String[] {NAMESPACE1};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeNamespaceEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = Collections.singletonList(makeNamespaceFilter(namespaceValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(NAMESPACE1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("NAMESPACE");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherClusterTypeQuery() {
    String[] clusterIdValues = new String[] {CLUSTER1_ID};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeClusterTypeEntityGroupBy(), makeStartTimeEntityGroupBy());
    List<QLBillingDataFilter> filters = Collections.singletonList(makeNamespaceFilter(clusterIdValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(CLUSTER_TYPE1);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("CLUSTERTYPE");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherWithMonthlyTimeAggregation() {
    String[] clusterValues = new String[] {CLUSTER1_ID};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeClusterEntityGroupBy(), makeMonthlyTimeAggregationGroupBy());
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeClusterFilter(clusterValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(CLUSTER1_ID);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("CLUSTERID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingTimeSeriesDataFetcherWithDailyTimeAggregation() {
    String[] clusterValues = new String[] {CLUSTER1_ID};

    List<QLCCMGroupBy> groupBy = Arrays.asList(makeClusterEntityGroupBy(), makeDailyTimeAggregationGroupBy());
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeClusterFilter(clusterValues));
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeAscByAmountSortingCriteria());

    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) billingStatsTimeSeriesDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.get(0).getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.get(0).getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
    assertThat(groupBy.get(0).getEntityGroupBy().getAggregationKind()).isEqualTo(QLAggregationKind.SIMPLE);
    assertThat(sortCriteria.get(0).getSortType()).isEqualTo(QLBillingSortType.Amount);
    assertThat(sortCriteria.get(0).getSortOrder()).isEqualTo(QLSortOrder.ASCENDING);
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getValues().get(0).getKey().getId()).isEqualTo(CLUSTER1_ID);
    assertThat(data.getData().get(0).getValues().get(0).getKey().getType()).isEqualTo("CLUSTERID");
    assertThat(data.getData().get(0).getValues().get(0).getValue()).isEqualTo(17.0);
  }

  private QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  private QLCCMAggregationFunction makeIdleCostAggregation() {
    return QLCCMAggregationFunction.builder().operationType(QLCCMAggregateOperation.SUM).columnName(IDLECOST).build();
  }

  private QLCCMAggregationFunction makeCpuIdleCostAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(CPUIDLECOST)
        .build();
  }

  private QLCCMAggregationFunction makeMemoryIdleCostAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(MEMORYIDLECOST)
        .build();
  }

  private QLCCMAggregationFunction makeMaxCpuUtilizationAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(MAXCPUUTILIZATION)
        .build();
  }

  private QLCCMAggregationFunction makeAvgCpuUtilizationAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(AVGCPUUTILIZATION)
        .build();
  }

  private QLCCMAggregationFunction makeMaxMemoryUtilizationAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(MAXMEMORYUTILIZATION)
        .build();
  }

  private QLCCMAggregationFunction makeAvgMemoryUtilizationAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName(AVGMEMORYUTILIZATION)
        .build();
  }

  private QLBillingSortCriteria makeAscByAmountSortingCriteria() {
    return QLBillingSortCriteria.builder().sortOrder(QLSortOrder.ASCENDING).sortType(QLBillingSortType.Amount).build();
  }

  private QLCCMGroupBy makeMonthlyTimeAggregationGroupBy() {
    QLCCMTimeSeriesAggregation timeSeriesAggregation =
        QLCCMTimeSeriesAggregation.builder().timeGroupType(QLTimeGroupType.MONTH).build();
    return QLCCMGroupBy.builder().timeAggregation(timeSeriesAggregation).build();
  }

  private QLCCMGroupBy makeDailyTimeAggregationGroupBy() {
    QLCCMTimeSeriesAggregation timeSeriesAggregation =
        QLCCMTimeSeriesAggregation.builder().timeGroupType(QLTimeGroupType.DAY).build();
    return QLCCMGroupBy.builder().timeAggregation(timeSeriesAggregation).build();
  }

  private QLCCMGroupBy makeStartTimeEntityGroupBy() {
    QLCCMEntityGroupBy startTimeGroupBy = QLCCMEntityGroupBy.StartTime;
    return QLCCMGroupBy.builder().entityGroupBy(startTimeGroupBy).build();
  }

  private QLCCMGroupBy makeApplicationEntityGroupBy() {
    QLCCMEntityGroupBy applicationGroupBy = QLCCMEntityGroupBy.Application;
    return QLCCMGroupBy.builder().entityGroupBy(applicationGroupBy).build();
  }

  private QLCCMGroupBy makeCloudServiceNameEntityGroupBy() {
    QLCCMEntityGroupBy cloudServiceNameGroupBy = QLCCMEntityGroupBy.CloudServiceName;
    return QLCCMGroupBy.builder().entityGroupBy(cloudServiceNameGroupBy).build();
  }

  private QLCCMGroupBy makeServiceEntityGroupBy() {
    QLCCMEntityGroupBy serviceGroupBy = QLCCMEntityGroupBy.Service;
    return QLCCMGroupBy.builder().entityGroupBy(serviceGroupBy).build();
  }

  private QLCCMGroupBy makeRegionEntityGroupBy() {
    QLCCMEntityGroupBy regionGroupBy = QLCCMEntityGroupBy.Region;
    return QLCCMGroupBy.builder().entityGroupBy(regionGroupBy).build();
  }

  private QLCCMGroupBy makeClusterEntityGroupBy() {
    QLCCMEntityGroupBy clusterGroupBy = QLCCMEntityGroupBy.Cluster;
    return QLCCMGroupBy.builder().entityGroupBy(clusterGroupBy).build();
  }

  private QLCCMGroupBy makeInstanceIdEntityGroupBy() {
    QLCCMEntityGroupBy instanceIdGroupBy = QLCCMEntityGroupBy.TaskId;
    return QLCCMGroupBy.builder().entityGroupBy(instanceIdGroupBy).build();
  }

  private QLCCMGroupBy makeEnvironmentEntityGroupBy() {
    QLCCMEntityGroupBy environmentGroupBy = QLCCMEntityGroupBy.Environment;
    return QLCCMGroupBy.builder().entityGroupBy(environmentGroupBy).build();
  }

  private QLCCMGroupBy makeLaunchTypeEntityGroupBy() {
    QLCCMEntityGroupBy launchTypeGroupBy = QLCCMEntityGroupBy.LaunchType;
    return QLCCMGroupBy.builder().entityGroupBy(launchTypeGroupBy).build();
  }

  private QLCCMGroupBy makeNamespaceEntityGroupBy() {
    QLCCMEntityGroupBy namespaceGroupBy = QLCCMEntityGroupBy.Namespace;
    return QLCCMGroupBy.builder().entityGroupBy(namespaceGroupBy).build();
  }

  private QLCCMGroupBy makeClusterTypeEntityGroupBy() {
    QLCCMEntityGroupBy clusterTypeGroupBy = QLCCMEntityGroupBy.ClusterType;
    return QLCCMGroupBy.builder().entityGroupBy(clusterTypeGroupBy).build();
  }

  private QLBillingDataFilter makeApplicationFilter(String[] values) {
    QLIdFilter applicationFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().application(applicationFilter).build();
  }

  private QLBillingDataFilter makeTaskIdFilter(String[] values) {
    QLIdFilter instanceIdFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().taskId(instanceIdFilter).build();
  }

  private QLBillingDataFilter makeLaunchTypeFilter(String[] values) {
    QLIdFilter launchTypeFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().launchType(launchTypeFilter).build();
  }

  private QLBillingDataFilter makeTimeFilter(Long filterTime) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  private QLBillingDataFilter makeServiceFilter(String[] values) {
    QLIdFilter serviceFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().service(serviceFilter).build();
  }

  private QLBillingDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  private QLBillingDataFilter makeNotNullClusterFilter() {
    String[] values = new String[] {""};
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  private QLBillingDataFilter makeEnvironmentFilter(String[] values) {
    QLIdFilter environmentFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().service(environmentFilter).build();
  }

  private QLBillingDataFilter makeCloudServiceNameFilter(String[] values) {
    QLIdFilter cloudServiceNameFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().cloudServiceName(cloudServiceNameFilter).build();
  }

  private QLBillingDataFilter makeInstanceTypeFilter(String[] values) {
    QLIdFilter instanceTypeFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().instanceType(instanceTypeFilter).build();
  }

  private QLBillingDataFilter makeNamespaceFilter(String[] values) {
    QLIdFilter namespaceFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().namespace(namespaceFilter).build();
  }

  private QLBillingDataFilter makeWorkloadNameFilter(String[] values) {
    QLIdFilter workloadNameFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLBillingDataFilter.builder().workloadName(workloadNameFilter).build();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getDouble(anyString())).thenAnswer((Answer<Double>) invocation -> 10.0 + doubleVal[0]++);

    when(resultSet.getString("APPID")).thenAnswer((Answer<String>) invocation -> APP1_ID_ACCOUNT1);
    when(resultSet.getString("CLOUDSERVICENAME"))
        .thenAnswer((Answer<String>) invocation -> CLOUD_SERVICE_NAME_ACCOUNT1);
    when(resultSet.getString("SERVICEID")).thenAnswer((Answer<String>) invocation -> SERVICE1_ID_APP1_ACCOUNT1);
    when(resultSet.getString("CLUSTERID")).thenAnswer((Answer<String>) invocation -> CLUSTER1_ID);
    when(resultSet.getString("REGION")).thenAnswer((Answer<String>) invocation -> REGION1);
    when(resultSet.getString("LAUNCHTYPE")).thenAnswer((Answer<String>) invocation -> LAUNCH_TYPE1);
    when(resultSet.getString("ENVID")).thenAnswer((Answer<String>) invocation -> ENV1_ID_APP1_ACCOUNT1);
    when(resultSet.getString("NAMESPACE")).thenAnswer((Answer<String>) invocation -> NAMESPACE1);
    when(resultSet.getString("REGION")).thenAnswer((Answer<String>) invocation -> REGION1);
    when(resultSet.getString("CLUSTERTYPE")).thenAnswer((Answer<String>) invocation -> CLUSTER_TYPE1);
    when(resultSet.getString("IDLECOST")).thenAnswer((Answer<String>) invocation -> IDLECOST);
    when(resultSet.getDouble("IDLECOST")).thenAnswer((Answer<Double>) invocation -> IDLECOST_VALUE);
    when(resultSet.getDouble("CPUIDLECOST")).thenAnswer((Answer<Double>) invocation -> CPUIDLECOST_VALUE);
    when(resultSet.getDouble("MEMORYIDLECOST")).thenAnswer((Answer<Double>) invocation -> MEMORYIDLECOST_VALUE);
    when(resultSet.getDouble("MAXCPUUTILIZATION")).thenAnswer((Answer<Double>) invocation -> MAXCPUUTILIZATION_VALUE);
    when(resultSet.getDouble("MAXMEMORYUTILIZATION"))
        .thenAnswer((Answer<Double>) invocation -> MAXMEMORYUTILIZATION_VALUE);
    when(resultSet.getDouble("AVGCPUUTILIZATION")).thenAnswer((Answer<Double>) invocation -> AVGCPUUTILIZATION_VALUE);
    when(resultSet.getDouble("AVGMEMORYTILIZATION"))
        .thenAnswer((Answer<Double>) invocation -> AVGMEMORYTILIZATION_VALUE);

    when(resultSet.getString("TASKID"))
        .thenAnswer((Answer<String>) invocation -> INSTANCE1_SERVICE1_ENV1_APP1_ACCOUNT1);
    when(resultSet.getTimestamp("STARTTIME", utils.getDefaultCalendar())).thenAnswer((Answer<Timestamp>) invocation -> {
      calendar[0] = calendar[0] + 3600000;
      return new Timestamp(calendar[0]);
    });
    when(resultSet.getTimestamp("STARTTIMEBUCKET", utils.getDefaultCalendar()))
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
