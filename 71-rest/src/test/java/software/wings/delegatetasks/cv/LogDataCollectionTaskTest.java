package software.wings.delegatetasks.cv;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
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
import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.sm.StateType;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LogDataCollectionTaskTest extends WingsBaseTest {
  private LogDataCollectionTask<LogDataCollectionInfoV2> logDataCollectionTask;
  @Mock private LogDataCollector<LogDataCollectionInfoV2> logDataCollector;
  @Mock private LogAnalysisStoreService logAnalysisStoreService;
  @Inject private DataCollectionExecutorService dataCollectionService;
  @Before
  public void setupTests() throws IllegalAccessException, IOException {
    initMocks(this);
    logDataCollectionTask = mock(LogDataCollectionTask.class, Mockito.CALLS_REAL_METHODS);
    when(logDataCollector.getHostBatchSize()).thenReturn(1);
    when(logDataCollectionTask.getDataCollector()).thenReturn(logDataCollector);
    dataCollectionService = spy(dataCollectionService);
    FieldUtils.writeField(logDataCollectionTask, "dataCollectionService", dataCollectionService, true);
    FieldUtils.writeField(logDataCollectionTask, "logAnalysisStoreService", logAnalysisStoreService, true);
    when(logAnalysisStoreService.save(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(true);
  }
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testSavingHeartbeatsForAllHosts() throws DataCollectionException, IOException {
    LogDataCollectionInfoV2 logDataCollectionInfo = createLogDataCollectionInfo();
    when(logDataCollectionInfo.getHosts()).thenReturn(Sets.newHashSet("host1", "host2", "host3", "host4"));
    Instant now = Instant.ofEpochMilli(Timestamp.currentMinuteBoundary());
    when(logDataCollectionInfo.getStartTime()).thenReturn(now.minus(10, ChronoUnit.MINUTES));
    when(logDataCollectionInfo.getEndTime()).thenReturn(now);
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    logDataCollectionTask.collectAndSaveData(logDataCollectionInfo);
    verify(logAnalysisStoreService)
        .save(eq(logDataCollectionInfo.getStateType()), any(), any(), any(), any(), any(), any(), any(), any(),
            captor.capture());
    List<LogElement> capturedList = captor.getValue();
    assertThat(capturedList.size()).isEqualTo(40);
    assertThat(capturedList.stream().allMatch(
                   logElement -> logElement.getClusterLabel().equals(ClusterLevel.H2.getLevel() + "")))
        .isTrue();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testIfFetchCalledForEachHostParallelly() throws DataCollectionException, IOException {
    LogDataCollectionInfoV2 logDataCollectionInfo = createLogDataCollectionInfo();
    when(logDataCollectionInfo.getHosts()).thenReturn(Sets.newHashSet("host1", "host2", "host3"));
    Instant now = Instant.now();
    when(logDataCollectionInfo.getStartTime()).thenReturn(now.minus(10, ChronoUnit.MINUTES));
    when(logDataCollectionInfo.getEndTime()).thenReturn(now);
    logDataCollectionTask.collectAndSaveData(logDataCollectionInfo);
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(dataCollectionService).executeParrallel(captor.capture());
    assertThat(captor.getValue().size()).isEqualTo(3);
  }

  public LogDataCollectionInfoV2 createLogDataCollectionInfo() {
    StateType stateType = StateType.SPLUNKV2;
    LogDataCollectionInfoV2 dataCollectionInfoV2 = mock(LogDataCollectionInfoV2.class);
    when(dataCollectionInfoV2.getStateType()).thenReturn(stateType);
    Instant now = Instant.now();
    when(dataCollectionInfoV2.getStartTime()).thenReturn(now.minus(10, ChronoUnit.MINUTES));
    when(dataCollectionInfoV2.getEndTime()).thenReturn(now);
    return dataCollectionInfoV2;
  }
}
