package io.harness.batch.processing.AnomalyDetection;

import static io.harness.rule.OwnerRule.SANDESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.TimeSeriesSpec;
import io.harness.batch.processing.anomalydetection.service.impl.AnomalyDetectionTimescaleDataServiceImpl;
import io.harness.batch.processing.anomalydetection.types.AnomalyDetectionModel;
import io.harness.batch.processing.anomalydetection.types.AnomalyType;
import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class AnomalyDetectionTimescaleDataServiceTest extends CategoryTest {
  @InjectMocks private AnomalyDetectionTimescaleDataServiceImpl dataService;
  @Mock private TimeScaleDBService timeScaleDBService;

  @Mock Statement statement;
  @Mock ResultSet clusterDataResultSet;

  @Mock private PreparedStatement preparedStatement;

  private final Instant NOW = Instant.now();
  private final Instant START_DATE = NOW.minus(1, ChronoUnit.HOURS);
  private final Instant END_DATE = NOW;
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String CLUSTER_NAME = "clusterName";

  final long currentTime = System.currentTimeMillis();
  final long[] calendar = {currentTime};
  final int[] count = {0};

  private List<Anomaly> mockedAnomaliesList;

  @Before
  public void setup() throws SQLException {
    MockitoAnnotations.initMocks(this);
    Connection mockConnection = mock(Connection.class);
    // Statement mockStatement = mock(Statement.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(mockConnection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(mockConnection.createStatement()).thenReturn(statement);
    when(mockConnection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(statement.executeQuery(anyString())).thenReturn(clusterDataResultSet);
    resetValues();
    mockClusterDataResultSet();
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void shouldReadTimeSeriesFromResultSet() throws SQLException {
    TimeSeriesSpec timeSeriesSpec = TimeSeriesSpec.builder()
                                        .accountId(ACCOUNT_ID)
                                        .trainStart(END_DATE.minus(14, ChronoUnit.DAYS))
                                        .trainEnd(END_DATE.minus(1, ChronoUnit.DAYS))
                                        .testStart(END_DATE.minus(1, ChronoUnit.DAYS))
                                        .testEnd(END_DATE)
                                        .entityType(EntityType.CLUSTER)
                                        .timeGranularity(TimeGranularity.DAILY)
                                        .entityIdentifier("CLUSTERID")
                                        .build();

    AnomalyDetectionTimeSeries anomalyDetectionTimeSeries =
        dataService.readNextTimeSeries(clusterDataResultSet, timeSeriesSpec);
    assertThat(anomalyDetectionTimeSeries).isNotNull();
    assertThat(anomalyDetectionTimeSeries.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(anomalyDetectionTimeSeries.getTestDataSize()).isEqualTo(1);
    assertThat(anomalyDetectionTimeSeries.getTrainDataSize()).isEqualTo(13);
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  @Ignore("need to change test according to query builder")
  public void shouldWriteAnomaliesIntoTimeScale() throws SQLException {
    mockAnomaliesList();
    when(preparedStatement.executeBatch()).thenReturn(new int[] {1, 1, 1, 1, 1});
    boolean insert = dataService.writeAnomaliesToTimescale(mockedAnomaliesList);
    assertThat(insert).isTrue();
  }

  private void mockAnomaliesList() {
    mockedAnomaliesList = generateSampleAnomalies();
  }

  private List<Anomaly> generateSampleAnomalies() {
    List<Anomaly> anomaliesList = new ArrayList<>();
    int index = 0;
    int anomalyCount = 5;
    while (index < anomalyCount) {
      anomaliesList.add(Anomaly.builder()
                            .time(NOW.minus(index, ChronoUnit.HOURS))
                            .accountId(ACCOUNT_ID)
                            .timeGranularity(TimeGranularity.DAILY)
                            .entityId("TEST_ANOMALY" + index)
                            .entityType(EntityType.CLUSTER)
                            .clusterId(CLUSTER_ID)
                            .clusterName(CLUSTER_NAME)
                            .workloadName(null)
                            .workloadType(null)
                            .isAnomaly(true)
                            .absoluteThreshold(true)
                            .relativeThreshold(true)
                            .probabilisticThreshold(true)
                            .anomalyScore(90.1 + Math.random())
                            .anomalyType(AnomalyType.SPIKE)
                            .reportedBy(AnomalyDetectionModel.STATISTICAL)
                            .build());
      index++;
    }
    return anomaliesList;
  }

  private void resetValues() {
    count[0] = 0;
  }

  private void mockClusterDataResultSet() throws SQLException {
    when(clusterDataResultSet.getTimestamp("STARTTIME")).thenAnswer((Answer<Timestamp>) invocation -> {
      calendar[0] = calendar[0] + 3600000;
      return new Timestamp(calendar[0]);
    });
    when(clusterDataResultSet.getDouble("COST")).thenAnswer((Answer<Double>) invocation -> 10.0 + Math.random());
    when(clusterDataResultSet.getString("CLUSTERID")).thenReturn(CLUSTER_ID);
    when(clusterDataResultSet.getString("CLUSTERNAME")).thenReturn(CLUSTER_NAME);
    returnResultSet(14, clusterDataResultSet);
  }

  private void returnResultSet(int limit, ResultSet resultSet) throws SQLException {
    when(resultSet.next()).thenAnswer((Answer<Boolean>) invocation -> {
      if (count[0] < limit) {
        count[0]++;
        return true;
      }
      count[0] = 0;
      return false;
    });
  }
}
