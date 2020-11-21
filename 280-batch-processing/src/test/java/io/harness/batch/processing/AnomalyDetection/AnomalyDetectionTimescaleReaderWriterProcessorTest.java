package io.harness.batch.processing.AnomalyDetection;

import static io.harness.rule.OwnerRule.SANDESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.BatchProcessingBaseTest;
import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionConstants;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.TimeSeriesSpec;
import io.harness.batch.processing.anomalydetection.processor.AnomalyDetectionStatsModelProcessor;
import io.harness.batch.processing.anomalydetection.reader.AnomalyDetectionClusterTimescaleReader;
import io.harness.batch.processing.anomalydetection.service.impl.AnomalyDetectionTimescaleDataServiceImpl;
import io.harness.batch.processing.anomalydetection.types.AnomalyDetectionModel;
import io.harness.batch.processing.anomalydetection.types.AnomalyType;
import io.harness.batch.processing.anomalydetection.types.EntityType;
import io.harness.batch.processing.anomalydetection.types.TimeGranularity;
import io.harness.batch.processing.anomalydetection.writer.AnomalyDetectionTimeScaleWriter;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

@RunWith(MockitoJUnitRunner.class)
public class AnomalyDetectionTimescaleReaderWriterProcessorTest extends BatchProcessingBaseTest {
  @InjectMocks private AnomalyDetectionClusterTimescaleReader timescaleReader;
  @InjectMocks private AnomalyDetectionStatsModelProcessor statsModelProcessor;
  @InjectMocks private AnomalyDetectionTimeScaleWriter timeScaleWriter;
  @Mock private AnomalyDetectionTimescaleDataServiceImpl dataService;
  @Mock private JobParameters parameters;

  private final Instant NOW = Instant.now();
  private final Instant END_TIME = NOW.truncatedTo(ChronoUnit.DAYS);
  private final Instant START_TIME = END_TIME.minus(1, ChronoUnit.DAYS);

  private final String CLUSTER_ID = "clusterId";
  private final String CLUSTER_NAME = "clusterName";
  private final String ACCOUNT_ID = "accountId";
  private List<Anomaly> mockedAnomaliesList;
  List<AnomalyDetectionTimeSeries> mockedAnomalyDetectionTimeSeriesList;

  final Instant currentTime = Instant.ofEpochMilli(System.currentTimeMillis()).truncatedTo(ChronoUnit.DAYS);
  final long[] calendar = {currentTime.toEpochMilli()};
  final int[] count = {0};

  @Captor private ArgumentCaptor<List<Anomaly>> anomalyCaptor;

  @Before
  public void setup() {
    TimeSeriesSpec timeSeriesSpec = TimeSeriesSpec.builder()
                                        .accountId(ACCOUNT_ID)
                                        .trainStart(currentTime.minus(14, ChronoUnit.DAYS))
                                        .trainEnd(currentTime.minus(1, ChronoUnit.DAYS))
                                        .testStart(currentTime.minus(1, ChronoUnit.DAYS))
                                        .testEnd(currentTime)
                                        .build();
    when(dataService.readData(timeSeriesSpec)).thenReturn(mockedAnomalyDetectionTimeSeriesList);
    mockAnomaliesList();
    mockTimeSeriesList();
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void AnomalyDetectionReaderTest() {
    StepExecution stepExecution = mock(StepExecution.class);
    JobParameters parameters = mock(JobParameters.class);
    JobExecution jobExecution = mock(JobExecution.class);
    when(stepExecution.getJobExecution()).thenReturn(jobExecution);
    when(jobExecution.getJobParameters()).thenReturn(parameters);

    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME.toEpochMilli()));
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME.toEpochMilli()));
    when(parameters.getString(CCMJobConstants.BATCH_JOB_TYPE)).thenReturn("ANOMALY_DETECTION");

    timescaleReader.beforeStep(stepExecution);

    AnomalyDetectionTimeSeries data = timescaleReader.read();
    while (data != null) {
      assertThat(data).isNotNull();
      assertThat(data.getTimeGranularity()).isEqualTo(TimeGranularity.DAILY);
      assertThat(data.getAccountId()).isEqualTo(ACCOUNT_ID);
      assertThat(data.getEntityId()).isEqualTo(CLUSTER_ID);
      assertThat(data.getClusterName()).isEqualTo(CLUSTER_NAME);
      assertThat(data.getClusterId()).isEqualTo(CLUSTER_ID);
      assertThat(data.getTrainDataSize()).isEqualTo(5);
      assertThat(data.getTestDataSize()).isEqualTo(1);
      data = timescaleReader.read();
    }
    ExitStatus repeatStatus = timescaleReader.afterStep(stepExecution);
    assertThat(repeatStatus).isNotNull();
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void shouldProcessTimeSeries() throws Exception {
    TimeSeriesSpec timeSeriesSpec = TimeSeriesSpec.builder()
                                        .accountId(ACCOUNT_ID)
                                        .trainStart(END_TIME.minus(14, ChronoUnit.DAYS))
                                        .trainEnd(END_TIME.minus(1, ChronoUnit.DAYS))
                                        .testStart(END_TIME.minus(1, ChronoUnit.DAYS))
                                        .testEnd(END_TIME)
                                        .timeGranularity(TimeGranularity.DAILY)
                                        .entityType(EntityType.CLUSTER)
                                        .build();

    Anomaly data = statsModelProcessor.process(getMockDailyTimeSeries(timeSeriesSpec));
    assertThat(data).isNotNull();
    assertThat(data.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(data.getEntityId()).isEqualTo(CLUSTER_ID);
    assertThat(data.getClusterId()).isEqualTo(CLUSTER_ID);
    // assertThat(data.isAnomaly()).isNotNull();
    // assertThat(data.isRelativeThreshold()).isNotNull();
    // assertThat(data.isAbsoluteThreshold()).isNotNull();
    // assertThat(data.isProbabilisticThreshold()).isNotNull();
    assertThat(data.getAnomalyScore()).isZero();
  }

  @Test
  @Owner(developers = SANDESH)
  @Category(UnitTests.class)
  public void shouldWriteAnomaliesIntoTimeScale() throws Exception {
    timeScaleWriter.write(mockedAnomaliesList);
    verify(dataService).writeAnomaliesToTimescale(anomalyCaptor.capture());
    assertThat(anomalyCaptor.getValue().get(0).getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(anomalyCaptor.getValue().get(0).getEntityId()).isEqualTo(CLUSTER_ID);
    assertThat(anomalyCaptor.getValue().get(0).getEntityType()).isEqualTo(EntityType.CLUSTER);
    assertThat(anomalyCaptor.getValue().get(0).getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(anomalyCaptor.getValue().get(0).isRelativeThreshold()).isTrue();
    assertThat(anomalyCaptor.getValue().get(0).isAbsoluteThreshold()).isTrue();
    assertThat(anomalyCaptor.getValue().get(0).isProbabilisticThreshold()).isTrue();
  }

  private void mockAnomaliesList() {
    mockedAnomaliesList = new ArrayList<>();
    mockedAnomaliesList.add(Anomaly.builder()
                                .time(NOW.minus(2, ChronoUnit.HOURS))
                                .accountId(ACCOUNT_ID)
                                .entityId(CLUSTER_ID)
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
  }

  private void mockTimeSeriesList() {
    mockedAnomalyDetectionTimeSeriesList = new ArrayList<>();
    TimeSeriesSpec timeSeriesSpec =
        TimeSeriesSpec.builder()
            .accountId(ACCOUNT_ID)
            .trainStart(END_TIME.minus(AnomalyDetectionConstants.DAYS_TO_CONSIDER, ChronoUnit.DAYS))
            .trainEnd(END_TIME.minus(1, ChronoUnit.DAYS))
            .testStart(END_TIME.minus(1, ChronoUnit.DAYS))
            .testEnd(END_TIME)
            .timeGranularity(TimeGranularity.DAILY)
            .entityType(EntityType.CLUSTER)
            .build();
    mockedAnomalyDetectionTimeSeriesList.add(getMockDailyTimeSeries(timeSeriesSpec));
  }

  private AnomalyDetectionTimeSeries getMockDailyTimeSeries(TimeSeriesSpec timeSeriesSpec) {
    AnomalyDetectionTimeSeries anomalyDetectionTimeSeries = AnomalyDetectionTimeSeries.builder()
                                                                .accountId(timeSeriesSpec.getAccountId())
                                                                .timeGranularity(TimeGranularity.DAILY)
                                                                .entityId(CLUSTER_ID)
                                                                .entityType(EntityType.CLUSTER)
                                                                .clusterId(CLUSTER_ID)
                                                                .clusterName(CLUSTER_NAME)
                                                                .build();

    anomalyDetectionTimeSeries.initialiseTrainData(
        timeSeriesSpec.getTrainStart(), timeSeriesSpec.getTrainEnd(), ChronoUnit.DAYS);
    anomalyDetectionTimeSeries.initialiseTestData(
        timeSeriesSpec.getTestStart(), timeSeriesSpec.getTestEnd(), ChronoUnit.DAYS);

    for (int i = 0; i < 14; i++) {
      anomalyDetectionTimeSeries.insert(timeSeriesSpec.getTrainStart().plus(i, ChronoUnit.DAYS), 10.0 + Math.random());
    }
    anomalyDetectionTimeSeries.insert(timeSeriesSpec.getTrainStart().plus(14, ChronoUnit.DAYS), 10.0 + Math.random());

    return anomalyDetectionTimeSeries;
  }
}
