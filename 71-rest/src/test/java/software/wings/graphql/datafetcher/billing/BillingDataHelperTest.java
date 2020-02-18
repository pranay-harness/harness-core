package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
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
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.security.UserThreadLocal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BillingDataHelperTest extends AbstractDataFetcherTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock private DataFetcherUtils utils;
  @Inject @InjectMocks BillingDataHelper billingDataHelper;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  final int[] count = {0};
  final double[] doubleVal = {0};
  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};
  private static final long ONE_DAY_MILLIS = 86400000;

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
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetBillingAmountDataForEntityCostTrend() {
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLCCMEntityGroupBy> groupBy = Arrays.asList(QLCCMEntityGroupBy.Cluster);
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeEndTimeFilter(currentTime));
    filters.add(makeStartTimeFilter(currentTime - ONE_DAY_MILLIS));

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            ACCOUNT1_ID, aggregationFunction, filters, groupBy, null, null);
    assertThat(entityIdToPrevBillingAmountData).isNotNull();
    assertThat(entityIdToPrevBillingAmountData.containsKey(CLUSTER1_ID)).isTrue();
    assertThat(entityIdToPrevBillingAmountData.get(CLUSTER1_ID).getCost()).isEqualTo(BigDecimal.valueOf(14.0));
    assertThat(entityIdToPrevBillingAmountData.get(CLUSTER1_ID).getMinStartTime())
        .isEqualTo(currentTime - ONE_DAY_MILLIS);
    assertThat(entityIdToPrevBillingAmountData.get(CLUSTER1_ID).getMaxStartTime()).isEqualTo(currentTime);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetCostTrendForEntity() throws SQLException {
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLCCMEntityGroupBy> groupBy = Arrays.asList(QLCCMEntityGroupBy.Cluster);
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeEndTimeFilter(currentTime));
    filters.add(makeStartTimeFilter(currentTime - ONE_DAY_MILLIS));

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            ACCOUNT1_ID, aggregationFunction, filters, groupBy, null, null);
    assertThat(entityIdToPrevBillingAmountData).isNotNull();
    assertThat(entityIdToPrevBillingAmountData.containsKey(CLUSTER1_ID)).isTrue();

    double costTrend =
        billingDataHelper.getCostTrendForEntity(resultSet, entityIdToPrevBillingAmountData.get(CLUSTER1_ID), filters);
    assertThat(costTrend).isEqualTo(0.0);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetCostTrendForEntityForNamespace() throws SQLException {
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLCCMEntityGroupBy> groupBy = Arrays.asList(QLCCMEntityGroupBy.Namespace);
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeEndTimeFilter(currentTime));
    filters.add(makeStartTimeFilter(currentTime - ONE_DAY_MILLIS));

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            ACCOUNT1_ID, aggregationFunction, filters, groupBy, null, null);
    assertThat(entityIdToPrevBillingAmountData).isNotNull();
    assertThat(entityIdToPrevBillingAmountData.containsKey(NAMESPACE1)).isTrue();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetCostTrendForEntityForWorkload() throws SQLException {
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLCCMEntityGroupBy> groupBy = new ArrayList<>();
    groupBy.add(QLCCMEntityGroupBy.WorkloadName);
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeEndTimeFilter(currentTime));
    filters.add(makeStartTimeFilter(currentTime - ONE_DAY_MILLIS));

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            ACCOUNT1_ID, aggregationFunction, filters, groupBy, null, null);
    assertThat(entityIdToPrevBillingAmountData).isNotNull();
    assertThat(entityIdToPrevBillingAmountData.containsKey(WORKLOAD_NAME_ACCOUNT1)).isTrue();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetCostTrendForEntityForApplication() throws SQLException {
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLCCMEntityGroupBy> groupBy = Arrays.asList(QLCCMEntityGroupBy.Application);
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeEndTimeFilter(currentTime));
    filters.add(makeStartTimeFilter(currentTime - ONE_DAY_MILLIS));

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            ACCOUNT1_ID, aggregationFunction, filters, groupBy, null, null);
    assertThat(entityIdToPrevBillingAmountData).isNotNull();
    assertThat(entityIdToPrevBillingAmountData.containsKey(APP1_ID_ACCOUNT1)).isTrue();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetCostTrendForEntityForService() throws SQLException {
    List<QLCCMAggregationFunction> aggregationFunction = Arrays.asList(makeBillingAmtAggregation());
    List<QLCCMEntityGroupBy> groupBy = Arrays.asList(QLCCMEntityGroupBy.Service);
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeEndTimeFilter(currentTime));
    filters.add(makeStartTimeFilter(currentTime - ONE_DAY_MILLIS));

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            ACCOUNT1_ID, aggregationFunction, filters, groupBy, null, null);
    assertThat(entityIdToPrevBillingAmountData).isNotNull();
    assertThat(entityIdToPrevBillingAmountData.containsKey(SERVICE1_ID_APP1_ACCOUNT1)).isTrue();
  }

  public QLCCMAggregationFunction makeBillingAmtAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.SUM)
        .columnName("billingamount")
        .build();
  }

  public QLBillingDataFilter makeStartTimeFilter(long value) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(value).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  public QLBillingDataFilter makeEndTimeFilter(long value) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(value).build();
    return QLBillingDataFilter.builder().endTime(timeFilter).build();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getDouble("COST")).thenAnswer((Answer<Double>) invocation -> 10.0 + doubleVal[0]++);
    when(resultSet.getBigDecimal("COST"))
        .thenAnswer((Answer<BigDecimal>) invocation -> BigDecimal.TEN.add(BigDecimal.valueOf(doubleVal[0]++)));
    when(resultSet.getString("APPID")).thenAnswer((Answer<String>) invocation -> APP1_ID_ACCOUNT1);
    when(resultSet.getString("SERVICEID")).thenAnswer((Answer<String>) invocation -> SERVICE1_ID_APP1_ACCOUNT1);
    when(resultSet.getString("ENVID")).thenAnswer((Answer<String>) invocation -> ENV1_ID_APP1_ACCOUNT1);
    when(resultSet.getString("WORKLOADNAME")).thenAnswer((Answer<String>) invocation -> WORKLOAD_NAME_ACCOUNT1);
    when(resultSet.getString("CLUSTERID")).thenAnswer((Answer<String>) invocation -> CLUSTER1_ID);
    when(resultSet.getString("NAMESPACE")).thenAnswer((Answer<String>) invocation -> NAMESPACE1);
    when(resultSet.getTimestamp(BillingDataQueryMetadata.BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(),
             utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(currentTime - ONE_DAY_MILLIS));
    when(resultSet.getTimestamp(BillingDataQueryMetadata.BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(),
             utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(currentTime));

    when(resultSet.getTimestamp(
             BillingDataQueryMetadata.BillingDataMetaDataFields.STARTTIME.getFieldName(), utils.getDefaultCalendar()))
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
