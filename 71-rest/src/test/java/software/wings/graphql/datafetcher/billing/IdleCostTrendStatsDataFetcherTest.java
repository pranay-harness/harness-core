package software.wings.graphql.datafetcher.billing;

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
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLIdleCostTrendStats;
import software.wings.security.UserThreadLocal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IdleCostTrendStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock private DataFetcherUtils utils;
  @Mock TimeScaleDBService timeScaleDBService;
  @Inject @InjectMocks IdleCostTrendStatsDataFetcher idleCostTrendStatsDataFetcher;

  @Mock Statement statement;
  @Mock ResultSet resultSet;

  private final BigDecimal TOTAL_COST = new BigDecimal("100.0");
  private final BigDecimal IDLE_COST = new BigDecimal("50.0");
  private final BigDecimal CPU_IDLE_COST = new BigDecimal("20.0");
  private final BigDecimal MEMORY_IDLE_COST = new BigDecimal("30.0");
  private final BigDecimal AVG_CPU_UTILIZATION = new BigDecimal("0.6");
  private final BigDecimal AVG_MEMORY_UTILIZATION = new BigDecimal("0.4");
  private final BigDecimal UNALLOCATED_COST = new BigDecimal("10.0");
  private Instant END_TIME = Instant.ofEpochMilli(1571509800000l);
  private Instant START_TIME = Instant.ofEpochMilli(1570645800000l);
  final int[] count = {0};

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
    mockResultSet();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetBillingTrendWhenDbIsInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    List<QLCCMAggregationFunction> aggregationFunction =
        Arrays.asList(makeBillingAmtAggregation(), makeIdleCostAggregation());
    assertThatThrownBy(()
                           -> idleCostTrendStatsDataFetcher.fetch(ACCOUNT1_ID, aggregationFunction,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetIdleCostTrendStats() {
    List<QLCCMAggregationFunction> aggregationFunction =
        Arrays.asList(makeBillingAmtAggregation(), makeIdleCostAggregation());
    List<QLBillingDataFilter> filters = createFilter();
    QLIdleCostTrendStats data = (QLIdleCostTrendStats) idleCostTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getTotalIdleCost().getStatsValue()).isEqualTo("$50.0");
    assertThat(data.getTotalIdleCost().getStatsLabel())
        .isEqualTo("Total Idle Cost of 09 October, 2019 - 19 October, 2019");
    assertThat(data.getTotalIdleCost().getStatsDescription()).isEqualTo("50.0% of total cost");
    assertThat(data.getCpuIdleCost().getStatsValue()).isEqualTo("-");
    assertThat(data.getCpuIdleCost().getStatsLabel()).isEqualTo("CPU idle Cost");
    assertThat(data.getCpuIdleCost().getStatsDescription()).isEqualTo("-");
    assertThat(data.getMemoryIdleCost().getStatsValue()).isEqualTo("-");
    assertThat(data.getMemoryIdleCost().getStatsLabel()).isEqualTo("Memory idle Cost");
    assertThat(data.getMemoryIdleCost().getStatsDescription()).isEqualTo("-");
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetCpuIdleCostTrendStats() {
    List<QLCCMAggregationFunction> aggregationFunction =
        Arrays.asList(makeCpuIdleCostAggregation(), makeAvgCpuUtilizationAggregation());
    List<QLBillingDataFilter> filters = createFilter();
    QLIdleCostTrendStats data = (QLIdleCostTrendStats) idleCostTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getTotalIdleCost().getStatsValue()).isEqualTo("-");
    assertThat(data.getTotalIdleCost().getStatsLabel())
        .isEqualTo("Total Idle Cost of 09 October, 2019 - 19 October, 2019");
    assertThat(data.getTotalIdleCost().getStatsDescription()).isEqualTo("-");
    assertThat(data.getCpuIdleCost().getStatsValue()).isEqualTo("$20.0");
    assertThat(data.getCpuIdleCost().getStatsLabel()).isEqualTo("CPU idle Cost");
    assertThat(data.getCpuIdleCost().getStatsDescription()).isEqualTo("60.0% avg. utilization");
    assertThat(data.getMemoryIdleCost().getStatsValue()).isEqualTo("-");
    assertThat(data.getMemoryIdleCost().getStatsLabel()).isEqualTo("Memory idle Cost");
    assertThat(data.getMemoryIdleCost().getStatsDescription()).isEqualTo("-");
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetMemoryIdleCostTrendStats() {
    List<QLCCMAggregationFunction> aggregationFunction =
        Arrays.asList(makeMemoryIdleCostAggregation(), makeAvgMemoryUtilizationAggregation());
    List<QLBillingDataFilter> filters = createFilter();
    QLIdleCostTrendStats data = (QLIdleCostTrendStats) idleCostTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getTotalIdleCost().getStatsValue()).isEqualTo("-");
    assertThat(data.getTotalIdleCost().getStatsLabel())
        .isEqualTo("Total Idle Cost of 09 October, 2019 - 19 October, 2019");
    assertThat(data.getTotalIdleCost().getStatsDescription()).isEqualTo("-");
    assertThat(data.getCpuIdleCost().getStatsValue()).isEqualTo("-");
    assertThat(data.getCpuIdleCost().getStatsLabel()).isEqualTo("CPU idle Cost");
    assertThat(data.getCpuIdleCost().getStatsDescription()).isEqualTo("-");
    assertThat(data.getMemoryIdleCost().getStatsValue()).isEqualTo("$30.0");
    assertThat(data.getMemoryIdleCost().getStatsLabel()).isEqualTo("Memory idle Cost");
    assertThat(data.getMemoryIdleCost().getStatsDescription()).isEqualTo("40.0% avg. utilization");
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetAllIdleCostTrendStats() {
    List<QLCCMAggregationFunction> aggregationFunction =
        Arrays.asList(makeBillingAmtAggregation(), makeIdleCostAggregation(), makeCpuIdleCostAggregation(),
            makeAvgCpuUtilizationAggregation(), makeMemoryIdleCostAggregation(), makeAvgMemoryUtilizationAggregation());
    List<QLBillingDataFilter> filters = createFilter();
    QLIdleCostTrendStats data = (QLIdleCostTrendStats) idleCostTrendStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getTotalIdleCost().getStatsValue()).isEqualTo("$50.0");
    assertThat(data.getTotalIdleCost().getStatsLabel())
        .isEqualTo("Total Idle Cost of 09 October, 2019 - 19 October, 2019");
    assertThat(data.getTotalIdleCost().getStatsDescription()).isEqualTo("50.0% of total cost");
    assertThat(data.getCpuIdleCost().getStatsValue()).isEqualTo("$20.0");
    assertThat(data.getCpuIdleCost().getStatsLabel()).isEqualTo("CPU idle Cost");
    assertThat(data.getCpuIdleCost().getStatsDescription()).isEqualTo("60.0% avg. utilization");
    assertThat(data.getMemoryIdleCost().getStatsValue()).isEqualTo("$30.0");
    assertThat(data.getMemoryIdleCost().getStatsLabel()).isEqualTo("Memory idle Cost");
    assertThat(data.getMemoryIdleCost().getStatsDescription()).isEqualTo("40.0% avg. utilization");
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetBillingTrendWhenQueryThrowsException() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    Statement mockStatement = mock(Statement.class);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException());

    List<QLCCMAggregationFunction> aggregationFunction =
        Arrays.asList(makeBillingAmtAggregation(), makeIdleCostAggregation());
    assertThatThrownBy(()
                           -> idleCostTrendStatsDataFetcher.fetch(ACCOUNT1_ID, aggregationFunction,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  private List<QLBillingDataFilter> createFilter() {
    String[] clusterFilterValues = new String[] {""};
    List<QLBillingDataFilter> filters = new ArrayList<>();
    filters.add(makeClusterFilter(clusterFilterValues));
    filters.add(startTimeFilter(START_TIME));
    filters.add(endTimeFilter(END_TIME));
    return filters;
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

  public QLCCMAggregationFunction makeAvgCpuUtilizationAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.AVG)
        .columnName("avgcpuutilization")
        .build();
  }

  public QLCCMAggregationFunction makeAvgMemoryUtilizationAggregation() {
    return QLCCMAggregationFunction.builder()
        .operationType(QLCCMAggregateOperation.AVG)
        .columnName("avgmemoryutilization")
        .build();
  }

  private QLBillingDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(values).build();
    return QLBillingDataFilter.builder().cluster(clusterFilter).build();
  }

  public QLBillingDataFilter startTimeFilter(Instant instant) {
    QLTimeFilter timeFilter =
        QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(instant.toEpochMilli()).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  public QLBillingDataFilter endTimeFilter(Instant instant) {
    QLTimeFilter timeFilter =
        QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(instant.toEpochMilli()).build();
    return QLBillingDataFilter.builder().endTime(timeFilter).build();
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
    when(resultSet.getBigDecimal("CPUIDLECOST")).thenReturn(CPU_IDLE_COST);
    when(resultSet.getBigDecimal("MEMORYIDLECOST")).thenReturn(MEMORY_IDLE_COST);
    when(resultSet.getBigDecimal("AVGCPUUTILIZATION")).thenReturn(AVG_CPU_UTILIZATION);
    when(resultSet.getBigDecimal("AVGMEMORYUTILIZATION")).thenReturn(AVG_MEMORY_UTILIZATION);
    when(resultSet.getTimestamp(BillingDataMetaDataFields.MIN_STARTTIME.getFieldName(), utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(START_TIME.toEpochMilli()));
    when(resultSet.getTimestamp(BillingDataMetaDataFields.MAX_STARTTIME.getFieldName(), utils.getDefaultCalendar()))
        .thenReturn(new Timestamp(END_TIME.toEpochMilli()));
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
}
