package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
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
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLEfficiencyStatsData;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class EfficiencyStatsDataFetcherTest extends AbstractDataFetcherTest {
  @InjectMocks @Inject EfficiencyStatsDataFetcher efficiencyStatsDataFetcher;
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock BillingDataHelper billingDataHelper;
  @Mock Statement statement;
  @Mock ResultSet resultSet;

  final int[] count = {0};

  private Double TOTAL_COST = Double.valueOf(200);
  private Double TOTAL_IDLE = Double.valueOf(60);
  private Double TOTAL_UNALLOCATED = Double.valueOf(40);

  private BigDecimal PREV_TOTAL_COST = BigDecimal.valueOf(150);
  private BigDecimal PREV_TOTAL_IDLE = BigDecimal.valueOf(30);
  private BigDecimal PREV_TOTAL_UNALLOCATED = BigDecimal.valueOf(30);

  private Instant END_TIME = Instant.ofEpochMilli(1571509800000l);
  private Instant START_TIME = Instant.ofEpochMilli(1570645800000l);
  private static List<QLBillingDataFilter> filters = new ArrayList<>();

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

    filters = createTimeFilterList();

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData = new HashMap<>();
    StringJoiner entityIdAppender = new StringJoiner(":");
    entityIdToPrevBillingAmountData.put(entityIdAppender.toString(),
        QLBillingAmountData.builder()
            .cost(PREV_TOTAL_COST)
            .idleCost(PREV_TOTAL_IDLE)
            .unallocatedCost(PREV_TOTAL_UNALLOCATED)
            .build());

    doReturn(entityIdToPrevBillingAmountData)
        .when(billingDataHelper)
        .getBillingAmountDataForEntityCostTrend(anyString(), any(), any(), any(), any(), any(), anyBoolean());
    doReturn(Double.valueOf(4.55))
        .when(billingDataHelper)
        .getCostTrendForEntity(resultSet, entityIdToPrevBillingAmountData.get(entityIdAppender.toString()), filters);
    doCallRealMethod().when(billingDataHelper).roundingDoubleFieldValue(any(), anyObject());
    doCallRealMethod().when(billingDataHelper).getRoundedDoubleValue(anyDouble());
    doCallRealMethod().when(billingDataHelper).getRoundedDoubleValue(any());
    doCallRealMethod().when(billingDataHelper).getStartTimeFilter(filters);
    doCallRealMethod().when(billingDataHelper).getEndTimeFilter(filters);
    doCallRealMethod().when(billingDataHelper).isYearRequired(any(), any());
    doCallRealMethod().when(billingDataHelper).getTotalCostFormattedDate(any(), anyBoolean());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataFetcher() {
    List<QLCCMAggregationFunction> aggregationFunction = getAggregationList();
    filters.add(QLBillingDataFilter.builder()
                    .cluster(QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(new String[] {""}).build())
                    .build());
    QLEfficiencyStatsData data = (QLEfficiencyStatsData) efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNotNull();
    assertThat(data.getContext().getTotalCost()).isEqualTo(200.0);
    assertThat(data.getContext().getCostTrend()).isEqualTo(4.55);
    assertThat(data.getEfficiencyBreakdown().getTotal()).isEqualTo(200.0);
    assertThat(data.getEfficiencyBreakdown().getIdle()).isEqualTo(60.0);
    assertThat(data.getEfficiencyBreakdown().getUtilized()).isEqualTo(100.0);
    assertThat(data.getEfficiencyBreakdown().getUnallocated()).isEqualTo(40.0);
    assertThat(data.getEfficiencyData().getTrend()).isEqualTo(-17.39);
    assertThat(data.getEfficiencyData().getEfficiencyScore()).isEqualTo(76);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEfficiencyStatsDataForDBInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    QLData data = efficiencyStatsDataFetcher.fetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testPostFetch() {
    QLData postFetch = efficiencyStatsDataFetcher.postFetch(
        ACCOUNT1_ID, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
    assertThat(postFetch).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEntityType() {
    String entityType = efficiencyStatsDataFetcher.getEntityType();
    assertThat(entityType).isNull();
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
    aggregationFunctionList.add(
        QLCCMAggregationFunction.builder().operationType(QLCCMAggregateOperation.SUM).columnName("idlecost").build());
    aggregationFunctionList.add(QLCCMAggregationFunction.builder()
                                    .operationType(QLCCMAggregateOperation.SUM)
                                    .columnName("unallocatedcost")
                                    .build());
    return aggregationFunctionList;
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);
    when(resultSet.getDouble("COST")).thenAnswer((Answer<Double>) invocation -> TOTAL_COST);
    when(resultSet.getDouble("ACTUALIDLECOST")).thenAnswer((Answer<Double>) invocation -> TOTAL_IDLE);
    when(resultSet.getDouble("UNALLOCATEDCOST")).thenAnswer((Answer<Double>) invocation -> TOTAL_UNALLOCATED);

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