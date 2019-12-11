package io.harness.batch.processing.billing.timeseries.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@RunWith(MockitoJUnitRunner.class)
public class K8sUtilizationGranularDataServiceImplTest extends CategoryTest {
  @Inject @InjectMocks private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;
  @Mock private TimeScaleDBService timeScaleDBService;
  @Mock private PreparedStatement statement;
  @Mock private DataFetcherUtils utils;
  @Mock ResultSet instanceIdsResultSet, aggregatedDataResultSet;

  private final Instant NOW = Instant.now();
  private final long START_DATE = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_DATE = NOW.toEpochMilli();
  final int[] count = {0};

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection mockConnection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(k8sUtilizationGranularDataService.INSERT_STATEMENT)).thenReturn(statement);
    when(mockConnection.prepareStatement(k8sUtilizationGranularDataService.UTILIZATION_DATA_QUERY))
        .thenReturn(statement);
    when(mockConnection.prepareStatement(k8sUtilizationGranularDataService.SELECT_DISTINCT_INSTANCEID))
        .thenReturn(statement);
    when(mockConnection.createStatement()).thenReturn(statement);
    when(utils.getDefaultCalendar()).thenReturn(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetDistinctInstantIds() throws SQLException {
    instanceIdsMockResultSet();
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.executeQuery(any())).thenReturn(instanceIdsResultSet);
    List<String> distinctIds = k8sUtilizationGranularDataService.getDistinctInstantIds(START_DATE, END_DATE);
    assertThat(distinctIds.get(0)).isEqualTo("INSTANCEID");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetDistinctInstantIdsInvalidDBService() {
    when(timeScaleDBService.getDBConnection()).thenThrow(SQLException.class);
    assertThat(k8sUtilizationGranularDataService.getDistinctInstantIds(START_DATE, END_DATE)).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetAggregatedUtilizationData() throws SQLException {
    aggregatedUtilizationDataMockResultSet();
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.executeQuery(any())).thenReturn(aggregatedDataResultSet);
    Map<String, InstanceUtilizationData> instanceUtilizationDataMap =
        k8sUtilizationGranularDataService.getAggregatedUtilizationData(
            Collections.singletonList("INSTANCEID"), START_DATE, END_DATE);
    assertThat(instanceUtilizationDataMap.get("INSTANCEID").getSettingId()).isEqualTo("SETTINGID");
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetAggregatedUtilizationDataInvalidDBService() {
    when(timeScaleDBService.getDBConnection()).thenThrow(SQLException.class);
    assertThat(k8sUtilizationGranularDataService.getAggregatedUtilizationData(
                   Collections.singletonList("instance"), START_DATE, END_DATE))
        .isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testCreateBillingData() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenReturn(true);
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationData();
    boolean insert = k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
    assertThat(insert).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testNullCreateBillingData() throws SQLException {
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(statement.execute()).thenThrow(new SQLException());
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationData();
    boolean insert = k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
    assertThat(insert).isFalse();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testInvalidDBService() {
    when(timeScaleDBService.isValid()).thenReturn(false);
    K8sGranularUtilizationData k8sGranularUtilizationData = K8sGranularUtilizationData();
    boolean insert = k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
    assertThat(insert).isFalse();
  }

  private K8sGranularUtilizationData K8sGranularUtilizationData() {
    return K8sGranularUtilizationData.builder()
        .settingId("settingId")
        .instanceType("instanceType")
        .instanceId("instanceId")
        .memory(2)
        .cpu(2)
        .endTimestamp(12000000000L)
        .startTimestamp(10000000000L)
        .build();
  }

  private void instanceIdsMockResultSet() throws SQLException {
    when(instanceIdsResultSet.getString("INSTANCEID")).thenAnswer((Answer<String>) invocation -> "INSTANCEID");
    returnResultSet(1, instanceIdsResultSet);
  }

  private void aggregatedUtilizationDataMockResultSet() throws SQLException {
    when(aggregatedDataResultSet.getString("INSTANCEID")).thenAnswer((Answer<String>) invocation -> "INSTANCEID");
    when(aggregatedDataResultSet.getString("ACCOUNTID")).thenAnswer((Answer<String>) invocation -> "ACCOUNTID");
    when(aggregatedDataResultSet.getString("INSTANCETYPE")).thenAnswer((Answer<String>) invocation -> "INSTANCETYPE");
    when(aggregatedDataResultSet.getString("SETTINGID")).thenAnswer((Answer<String>) invocation -> "SETTINGID");
    when(aggregatedDataResultSet.getString("CPUUTILIZATIONMAX"))
        .thenAnswer((Answer<String>) invocation -> "CPUUTILIZATIONMAX");
    when(aggregatedDataResultSet.getString("MEMORYUTILIZATIONMAX"))
        .thenAnswer((Answer<String>) invocation -> "MEMORYUTILIZATIONMAX");
    when(aggregatedDataResultSet.getString("CPUUTILIZATIONAVG"))
        .thenAnswer((Answer<String>) invocation -> "CPUUTILIZATIONAVG");
    when(aggregatedDataResultSet.getString("MEMORYUTILIZATIONAVG"))
        .thenAnswer((Answer<String>) invocation -> "MEMORYUTILIZATIONAVG");

    returnResultSet(1, aggregatedDataResultSet);
  }

  private void returnResultSet(int limit, ResultSet resultSet) throws SQLException {
    when(resultSet.next()).then((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      count[0] = 0;
      return false;
    });
  }
}
