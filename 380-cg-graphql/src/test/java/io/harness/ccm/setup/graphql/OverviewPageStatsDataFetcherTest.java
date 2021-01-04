package io.harness.ccm.setup.graphql;

import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.preaggregated.PreAggregateConstants;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.ccm.setup.graphql.QLCEOverviewStatsData.QLCEOverviewStatsDataBuilder;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.app.MainConfiguration;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingVariableTypes;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableId;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;

public class OverviewPageStatsDataFetcherTest extends AbstractDataFetcherTestBase {
  @Spy @InjectMocks OverviewPageStatsDataFetcher overviewPageStatsDataFetcher;
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock ResultSet applicationResultSet;
  @Mock ResultSet clusterResultSet;
  @Mock BigQueryService bigQueryService;
  @Mock MainConfiguration configuration;

  final int[] appCount = {0};
  final int[] clusterCount = {0};
  private static final String CONNECTOR_NAME =
      "connectorName_" + OverviewPageStatsDataFetcherTest.class.getSimpleName();
  private static final String UUID = "uuid_" + OverviewPageStatsDataFetcherTest.class.getSimpleName();
  private static final String GCP_PROJECT_ID = "gcpProjectId";
  private static String clusterQuery =
      "SELECT * FROM BILLING_DATA_HOURLY WHERE accountid = 'ACCOUNT1_ID' AND clusterid IS NOT NULL AND starttime >= '%s' LIMIT 1";
  private static String applicationQuery =
      "SELECT * FROM BILLING_DATA_HOURLY WHERE accountid = 'ACCOUNT1_ID' AND appid IS NOT NULL AND starttime >= '%s' LIMIT 1";

  @Before
  public void setup() throws SQLException, InterruptedException {
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
    doReturn(true).when(overviewPageStatsDataFetcher).getCEEnabledCloudProvider(ACCOUNT1_ID);

    BigQuery mockBigQuery = mock(BigQuery.class);
    when(configuration.getCeSetUpConfig()).thenReturn(CESetUpConfig.builder().gcpProjectId(GCP_PROJECT_ID).build());
    when(bigQueryService.get()).thenReturn(mockBigQuery);
    when(mockBigQuery.getTable(TableId.of("BillingReport_account1_id", "preAggregated"))).thenReturn(null);

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
    assertThat(data.getAwsConnectorsPresent()).isFalse();
    assertThat(data.getGcpConnectorsPresent()).isFalse();
    assertThat(data.getApplicationDataPresent()).isTrue();
    assertThat(data.getClusterDataPresent()).isTrue();
    assertThat(data.getCeEnabledClusterPresent()).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testModifyOverviewStatsBuilder() {
    FieldValueList row = mock(FieldValueList.class);
    FieldValue fieldValue = mock(FieldValue.class);
    when(row.get(PreAggregateConstants.entityCloudProviderConst)).thenReturn(fieldValue);
    when(row.get(PreAggregateConstants.countStringValueConstant)).thenReturn(fieldValue);
    when(fieldValue.getStringValue()).thenReturn("AWS");
    when(fieldValue.getDoubleValue()).thenReturn(1.0);
    QLCEOverviewStatsDataBuilder overviewStatsDataBuilder = QLCEOverviewStatsData.builder();
    overviewPageStatsDataFetcher.modifyOverviewStatsBuilder(row, overviewStatsDataBuilder);
    QLCEOverviewStatsData overviewStatsData = overviewStatsDataBuilder.build();
    assertThat(overviewStatsData.getAwsConnectorsPresent()).isTrue();

    when(fieldValue.getStringValue()).thenReturn("GCP");
    when(fieldValue.getDoubleValue()).thenReturn(0.0);
    overviewStatsDataBuilder = QLCEOverviewStatsData.builder();
    overviewPageStatsDataFetcher.modifyOverviewStatsBuilder(row, overviewStatsDataBuilder);
    overviewStatsData = overviewStatsDataBuilder.build();
    assertThat(overviewStatsData.getGcpConnectorsPresent()).isFalse();
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
