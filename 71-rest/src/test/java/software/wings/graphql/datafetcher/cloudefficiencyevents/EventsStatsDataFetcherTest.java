package software.wings.graphql.datafetcher.cloudefficiencyevents;

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
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
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

public class EventsStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Mock private DataFetcherUtils utils;
  @Mock TimeScaleDBService timeScaleDBService;
  @Inject @InjectMocks EventsStatsDataFetcher eventsStatsDataFetcher;

  @Mock ResultSet resultSet;
  @Mock Statement statement;

  final int[] count = {0};
  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};

  private static String EVENTDESCRIPTION = "EVENTDESCRIPTION";
  private static String COSTEVENTTYPE = "COSTEVENTTYPE";
  private static String COSTEVENTSOURCE = "COSTEVENTSOURCE";
  private static Double PERCENTAGE_CHANGE = 2.0;

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

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
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEventsDataFetcherWhenDbIsInvalid() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    assertThatThrownBy(()
                           -> eventsStatsDataFetcher.fetch(ACCOUNT1_ID, null, Collections.EMPTY_LIST,
                               Collections.EMPTY_LIST, Collections.EMPTY_LIST))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testEventsDataFetcherWhenQueryThrowsException() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    Statement mockStatement = mock(Statement.class);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(anyString())).thenThrow(new SQLException());

    QLData data = eventsStatsDataFetcher.fetch(
        ACCOUNT1_ID, null, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    assertThat(data).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetchMethodInEventsDataFetcher() {
    String[] clusterValues = new String[] {CLUSTER1_ID};

    List<QLEventsDataFilter> filters = new ArrayList<>();
    filters.add(makeClusterFilter(clusterValues));
    List<QLEventsSortCriteria> sortCriteria = Arrays.asList(makeAscByTimeSortingCriteria());

    QLEventData data =
        (QLEventData) eventsStatsDataFetcher.fetch(ACCOUNT1_ID, null, filters, Collections.emptyList(), sortCriteria);

    List<QLEventsDataPoint> dataPoints = data.getData();
    assertThat(data).isNotNull();
    assertThat(dataPoints.get(0).getDetails()).isEqualTo(EVENTDESCRIPTION);
    assertThat(dataPoints.get(0).getSource()).isEqualTo(COSTEVENTSOURCE);
    assertThat(dataPoints.get(0).getType()).isEqualTo(COSTEVENTTYPE);
    assertThat(dataPoints.get(0).getCostChangePercentage()).isEqualTo(PERCENTAGE_CHANGE);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testFetchMethodInEventsDataFetcherTimeFilter() {
    String[] clusterValues = new String[] {CLUSTER1_ID};

    List<QLEventsDataFilter> filters = new ArrayList<>();
    filters.add(makeClusterFilter(clusterValues));
    filters.add(makeTimeFilter(0L));
    filters.add(makeEndTimeFilter(currentTime + 3600000 * 30));
    List<QLEventsSortCriteria> sortCriteria = Arrays.asList(makeAscByTimeSortingCriteria());

    QLEventData data =
        (QLEventData) eventsStatsDataFetcher.fetch(ACCOUNT1_ID, null, filters, Collections.emptyList(), sortCriteria);

    List<QLEventsDataPoint> dataPoints = data.getData();
    assertThat(data).isNotNull();
    assertThat(dataPoints.get(0).getDetails()).isEqualTo(EVENTDESCRIPTION);
    assertThat(dataPoints.get(0).getSource()).isEqualTo(COSTEVENTSOURCE);
    assertThat(dataPoints.get(0).getType()).isEqualTo(COSTEVENTTYPE);
    assertThat(dataPoints.get(0).getCostChangePercentage()).isEqualTo(PERCENTAGE_CHANGE);
  }

  private QLEventsDataFilter makeTimeFilter(Long filterTime) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(filterTime).build();
    return QLEventsDataFilter.builder().startTime(timeFilter).build();
  }

  private QLEventsDataFilter makeEndTimeFilter(Long filterTime) {
    QLTimeFilter timeFilter = QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(filterTime).build();
    return QLEventsDataFilter.builder().endTime(timeFilter).build();
  }

  private QLEventsSortCriteria makeAscByTimeSortingCriteria() {
    return QLEventsSortCriteria.builder().sortOrder(QLSortOrder.ASCENDING).sortType(QLEventsSortType.Time).build();
  }

  private QLEventsDataFilter makeClusterFilter(String[] values) {
    QLIdFilter clusterFilter = QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(values).build();
    return QLEventsDataFilter.builder().cluster(clusterFilter).build();
  }

  private void mockResultSet() throws SQLException {
    Connection connection = mock(Connection.class);
    statement = mock(Statement.class);
    resultSet = mock(ResultSet.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(anyString())).thenReturn(resultSet);

    when(resultSet.getString("CLUSTERID")).thenAnswer((Answer<String>) invocation -> CLUSTER1_ID);
    when(resultSet.getDouble("COST_CHANGE_PERCENT")).thenAnswer((Answer<Double>) invocation -> PERCENTAGE_CHANGE);
    when(resultSet.getString("EVENTDESCRIPTION")).thenAnswer((Answer<String>) invocation -> EVENTDESCRIPTION);
    when(resultSet.getString("COSTEVENTTYPE")).thenAnswer((Answer<String>) invocation -> COSTEVENTTYPE);
    when(resultSet.getString("COSTEVENTSOURCE")).thenAnswer((Answer<String>) invocation -> COSTEVENTSOURCE);
    when(resultSet.getTimestamp("STARTTIME", utils.getDefaultCalendar())).thenAnswer((Answer<Timestamp>) invocation -> {
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
  }
}