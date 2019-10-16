package software.wings.delegatetasks.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.time.Timestamp;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.MetricDataStoreService;
import software.wings.service.impl.analysis.MetricElement;
import software.wings.service.impl.analysis.MetricsDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MetricDataCollectionTaskTest extends WingsBaseTest {
  private MetricsDataCollectionTask<MetricsDataCollectionInfo> metricsDataCollectionTask;
  @Mock private MetricsDataCollector<MetricsDataCollectionInfo> metricsDataCollector;
  @Inject private DataCollectionExecutorService dataCollectionService;
  @Mock private MetricDataStoreService metricStoreService;

  @Before
  public void setupTests() throws IllegalAccessException {
    initMocks(this);
    metricsDataCollectionTask = mock(MetricsDataCollectionTask.class, Mockito.CALLS_REAL_METHODS);
    when(metricsDataCollector.getHostBatchSize()).thenReturn(1);
    when(metricsDataCollectionTask.getDataCollector()).thenReturn(metricsDataCollector);
    dataCollectionService = spy(dataCollectionService);
    FieldUtils.writeField(metricsDataCollectionTask, "dataCollectionService", dataCollectionService, true);
    FieldUtils.writeField(metricsDataCollectionTask, "metricStoreService", metricStoreService, true);
    when(metricStoreService.saveNewRelicMetrics(any(), any(), any(), any(), any())).thenReturn(true);
  }

  @Test
  @Category(UnitTests.class)
  public void testSavingHeartbeatsForAllHosts() throws DataCollectionException {
    MetricsDataCollectionInfo metricsDataCollectionInfo = createMetricDataCollectionInfo();
    when(metricsDataCollectionInfo.getHosts()).thenReturn(Sets.newHashSet("host1", "host2", "host3", "host4"));
    metricsDataCollectionInfo.getHostsToGroupNameMap().put("host1", "default");
    metricsDataCollectionInfo.getHostsToGroupNameMap().put("host2", "default");
    metricsDataCollectionInfo.getHostsToGroupNameMap().put("host3", "group1");
    metricsDataCollectionInfo.getHostsToGroupNameMap().put("host4", "group2");
    Instant now = Instant.ofEpochMilli(Timestamp.currentMinuteBoundary());
    when(metricsDataCollectionInfo.getStartTime()).thenReturn(now.minus(10, ChronoUnit.MINUTES));
    when(metricsDataCollectionInfo.getEndTime()).thenReturn(now);
    metricsDataCollectionTask.collectAndSaveData(metricsDataCollectionInfo);
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(metricStoreService)
        .saveNewRelicMetrics(anyString(), anyString(), anyString(), anyString(), captor.capture());
    List<NewRelicMetricDataRecord> capturedList = captor.getValue();
    assertThat(capturedList.size()).isEqualTo(3);
    assertThat(capturedList.stream().allMatch(
                   newRelicMetricDataRecord -> newRelicMetricDataRecord.getName().equals("Harness heartbeat metric")))
        .isTrue();
    assertThat(capturedList.stream().allMatch(
                   newRelicMetricDataRecord -> newRelicMetricDataRecord.getLevel() == ClusterLevel.H0))
        .isTrue();
    assertThat(capturedList.stream().allMatch(newRelicMetricDataRecord
                   -> newRelicMetricDataRecord.getTimeStamp() == metricsDataCollectionInfo.getEndTime().toEpochMilli()))
        .isTrue();
    assertThat(capturedList.stream().allMatch(newRelicMetricDataRecord
                   -> newRelicMetricDataRecord.getDataCollectionMinute()
                       == TimeUnit.MILLISECONDS.toMinutes(metricsDataCollectionInfo.getEndTime().toEpochMilli())))
        .isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testIfFetchCalledForEachHostParallelly() throws DataCollectionException {
    MetricsDataCollectionInfo metricsDataCollectionInfo = createMetricDataCollectionInfo();
    when(metricsDataCollectionInfo.getHosts()).thenReturn(Sets.newHashSet("host1", "host2", "host3"));
    Instant now = Instant.now();
    when(metricsDataCollectionInfo.getStartTime()).thenReturn(now.minus(10, ChronoUnit.MINUTES));
    when(metricsDataCollectionInfo.getEndTime()).thenReturn(now);
    metricsDataCollectionTask.collectAndSaveData(metricsDataCollectionInfo);
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(dataCollectionService).executeParrallel(captor.capture());
    assertThat(captor.getValue().size()).isEqualTo(3);
  }

  @Test
  @Category(UnitTests.class)
  public void testIfNewRelicMetricDataRecordsAreSaved() throws DataCollectionException {
    MetricsDataCollectionInfo metricsDataCollectionInfo = createMetricDataCollectionInfo();
    when(metricsDataCollectionInfo.getHosts()).thenReturn(Sets.newHashSet("host1"));
    Instant now = Instant.now();
    when(metricsDataCollectionInfo.getStartTime()).thenReturn(now.minus(10, ChronoUnit.MINUTES));
    when(metricsDataCollectionInfo.getEndTime()).thenReturn(now);
    MetricElement metricElement = MetricElement.builder()
                                      .name("metric1")
                                      .host("host1")
                                      .groupName("default")
                                      .timestamp(System.currentTimeMillis())
                                      .build();
    when(metricsDataCollector.fetchMetrics(any())).thenReturn(Lists.newArrayList(metricElement));
    metricsDataCollectionTask.collectAndSaveData(metricsDataCollectionInfo);

    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(metricStoreService).saveNewRelicMetrics(any(), any(), any(), any(), captor.capture());
    List<NewRelicMetricDataRecord> records = captor.getValue();
    assertThat(records.size()).isEqualTo(1);
    assertThat(records.get(0).getStateExecutionId()).isEqualTo(metricsDataCollectionInfo.getStateExecutionId());
    assertThat(records.get(0).getServiceId()).isEqualTo(metricsDataCollectionInfo.getServiceId());
    assertThat(records.get(0).getHost()).isEqualTo("host1");
    assertThat(records.get(0).getGroupName()).isEqualTo(metricElement.getGroupName());
    assertThat(records.get(0).getName()).isEqualTo(metricElement.getName());
    assertThat(records.get(0).getTimeStamp()).isEqualTo(metricElement.getTimestamp());
    assertThat(records.get(0).getStateType()).isEqualTo(metricsDataCollectionInfo.getStateType());
    assertThat(records.get(0).getDataCollectionMinute())
        .isEqualTo(TimeUnit.MILLISECONDS.toMinutes(metricElement.getTimestamp()));
    assertThat(records.get(0).getCvConfigId()).isEqualTo(metricsDataCollectionInfo.getCvConfigId());
  }

  public MetricsDataCollectionInfo createMetricDataCollectionInfo() {
    StateType stateType = StateType.NEW_RELIC;
    MetricsDataCollectionInfo dataCollectionInfo = mock(MetricsDataCollectionInfo.class);
    when(dataCollectionInfo.getAccountId()).thenReturn(UUID.randomUUID().toString());
    when(dataCollectionInfo.getApplicationId()).thenReturn(UUID.randomUUID().toString());
    when(dataCollectionInfo.getStateExecutionId()).thenReturn(UUID.randomUUID().toString());
    when(dataCollectionInfo.getStateType()).thenReturn(stateType);
    Instant now = Instant.now();
    when(dataCollectionInfo.getStartTime()).thenReturn(now.minus(10, ChronoUnit.MINUTES));
    when(dataCollectionInfo.getEndTime()).thenReturn(now);
    when(dataCollectionInfo.getHostsToGroupNameMap()).thenReturn(Maps.newHashMap());
    return dataCollectionInfo;
  }
}