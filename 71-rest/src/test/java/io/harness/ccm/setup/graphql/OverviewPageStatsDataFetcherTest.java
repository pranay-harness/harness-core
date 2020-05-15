package io.harness.ccm.setup.graphql;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class OverviewPageStatsDataFetcherTest extends AbstractDataFetcherTest {
  @Spy @InjectMocks OverviewPageStatsDataFetcher overviewPageStatsDataFetcher;
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock ResultSet applicationResultSet;
  @Mock ResultSet clusterResultSet;

  final int[] appCount = {0};
  final int[] clusterCount = {0};
  private static final String CONNECTOR_NAME =
      "connectorName_" + OverviewPageStatsDataFetcherTest.class.getSimpleName();
  private static final String UUID = "uuid_" + OverviewPageStatsDataFetcherTest.class.getSimpleName();
  private static String clusterQuery =
      "SELECT count(*) AS count FROM BILLING_DATA WHERE accountid = 'ACCOUNT1_ID' AND clusterid IS NOT NULL AND starttime >= '%s'";
  private static String applicationQuery =
      "SELECT count(*) AS count FROM BILLING_DATA WHERE accountid = 'ACCOUNT1_ID' AND appid IS NOT NULL AND starttime >= '%s'";

  @Before
  public void setup() throws SQLException {
    Instant sevenDaysPriorInstant =
        Instant.ofEpochMilli(Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli() - TimeUnit.DAYS.toMillis(7));
    clusterQuery = String.format(clusterQuery, sevenDaysPriorInstant);
    applicationQuery = String.format(applicationQuery, sevenDaysPriorInstant);

    // Create a Connector
    SettingValue settingValue = CEAwsConfig.builder().build();
    SettingAttribute ceConnector = SettingAttribute.Builder.aSettingAttribute()
                                       .withName(CONNECTOR_NAME)
                                       .withUuid(UUID)
                                       .withValue(settingValue)
                                       .withAccountId(ACCOUNT1_ID)
                                       .withCategory(SettingAttribute.SettingCategory.CE_CONNECTOR)
                                       .build();
    settingValue.setType(SettingVariableTypes.CE_AWS.toString());
    doReturn(Arrays.asList(ceConnector)).when(overviewPageStatsDataFetcher).getCEConnectors(ACCOUNT1_ID);

    Connection mockConnection = mock(Connection.class);
    Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockStatement.executeQuery(applicationQuery)).thenReturn(applicationResultSet);
    when(mockStatement.executeQuery(clusterQuery)).thenReturn(clusterResultSet);
    when(applicationResultSet.getInt("count")).thenAnswer((Answer<Integer>) invocation -> 0);
    when(clusterResultSet.getInt("count")).thenAnswer((Answer<Integer>) invocation -> 1);
    returnResultSet(1, applicationResultSet, appCount);
    returnResultSet(1, clusterResultSet, clusterCount);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetch() {
    QLCEOverviewStatsData data = overviewPageStatsDataFetcher.fetch(null, ACCOUNT1_ID);
    assertThat(data.getCloudConnectorsPresent()).isTrue();
    assertThat(data.getAwsConnectorsPresent()).isTrue();
    assertThat(data.getGcpConnectorsPresent()).isFalse();
    assertThat(data.getApplicationDataPresent()).isFalse();
    assertThat(data.getClusterDataPresent()).isTrue();
  }

  private void returnResultSet(int limit, ResultSet resultSet, int[] count) throws SQLException {
    when(resultSet.next()).then((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      return false;
    });
  }
}