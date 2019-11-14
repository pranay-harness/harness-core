package software.wings.graphql.datafetcher.billing;

import static io.harness.rule.OwnerRule.UNKNOWN;
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
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetBillingTrendWhenDbIsInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    QLCCMAggregationFunction aggregationFunction = makeBillingAmtAggregation();
    assertThatThrownBy(()
                           -> billingStatsEntityDataFetcher.fetch(ACCOUNT1_ID, aggregationFunction,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testFetchMethodInBillingEntitySeriesDataFetcher() {
    Long filterTime = 0L;

    QLCCMAggregationFunction aggregationFunction = makeBillingAmtAggregation();
    List<QLBillingDataFilter> filters = Arrays.asList(makeTimeFilter(filterTime));
    List<QLCCMGroupBy> groupBy = Arrays.asList(makeStartTimeEntityGroupBy(), makeApplicationEntityGroupBy());
    List<QLBillingSortCriteria> sortCriteria = Arrays.asList(makeDescByTimeSortingCriteria());

    QLEntityTableListData data = (QLEntityTableListData) billingStatsEntityDataFetcher.fetch(
        ACCOUNT1_ID, aggregationFunction, filters, groupBy, sortCriteria);

    assertThat(aggregationFunction.getColumnName()).isEqualTo("billingamount");
    assertThat(aggregationFunction.getOperationType()).isEqualTo(QLCCMAggregateOperation.SUM);
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
    assertThat(data.getData().get(0).getTotalCost()).isEqualTo(10.0);
    assertThat(data.getData().get(1).getTotalCost()).isEqualTo(11.0);
    assertThat(data.getData().get(2).getTotalCost()).isEqualTo(12.0);
    assertThat(data.getData().get(3).getTotalCost()).isEqualTo(13.0);
    assertThat(data.getData().get(4).getTotalCost()).isEqualTo(14.0);
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

  public QLCCMGroupBy makeStartTimeEntityGroupBy() {
    QLCCMEntityGroupBy startTimeGroupBy = QLCCMEntityGroupBy.StartTime;
    return QLCCMGroupBy.builder().entityGroupBy(startTimeGroupBy).build();
  }

  public QLCCMGroupBy makeApplicationEntityGroupBy() {
    QLCCMEntityGroupBy applicationGroupBy = QLCCMEntityGroupBy.Application;
    return QLCCMGroupBy.builder().entityGroupBy(applicationGroupBy).build();
  }

  public QLBillingDataFilter makeTimeFilter(Long filterTime) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build();
    return QLBillingDataFilter.builder().startTime(timeFilter).build();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getDouble(anyString())).thenAnswer((Answer<Double>) invocation -> 10.0 + doubleVal[0]++);

    when(resultSet.getString(anyString())).thenAnswer((Answer<String>) invocation -> APP1_ID_ACCOUNT1);
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
