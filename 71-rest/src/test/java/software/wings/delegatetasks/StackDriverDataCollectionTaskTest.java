package software.wings.delegatetasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.TreeBasedTable;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.time.Timestamp;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.impl.stackdriver.StackDriverMetric;
import software.wings.service.impl.stackdriver.StackDriverNameSpace;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class StackDriverDataCollectionTaskTest extends WingsBaseTest {
  @Mock private DataCollectionExecutorService dataCollectionService;
  @Mock private StackDriverDelegateService stackDriverDelegateService;
  @Mock private DelegateLogService delegateLogService;
  @Mock private GcpHelperService gcpHelperService;
  @Mock private EncryptionService encryptionService;
  @Mock private MetricDataStoreService metricStoreService;
  private StackDriverDataCollectionTask dataCollectionTask;
  private StackDriverDataCollectionInfo dataCollectionInfo;

  private String delegateId = UUID.randomUUID().toString();
  private String appId = UUID.randomUUID().toString();
  private String envId = UUID.randomUUID().toString();
  private String waitId = UUID.randomUUID().toString();
  private String accountId = UUID.randomUUID().toString();
  private String stateExecutionId = UUID.randomUUID().toString();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    TreeBasedTable<String, Long, NewRelicMetricDataRecord> rv = TreeBasedTable.create();
    NewRelicMetricDataRecord record = NewRelicMetricDataRecord.builder().uuid("testdatarecord").build();
    rv.put("dummyHost", Timestamp.currentMinuteBoundary(), record);
    when(dataCollectionService.executeParrallel(any(List.class))).thenReturn(Arrays.asList(Optional.of(rv)));
    when(stackDriverDelegateService.createFilter(
             StackDriverNameSpace.POD_NAME, "kubernetes.io/container/memory/request_utilization", "dummyHost"))
        .thenReturn("testFilter dummyHost");
    when(stackDriverDelegateService.createFilter(
             StackDriverNameSpace.POD_NAME, "kubernetes.io/container/memory/request_utilization", "secondHost"))
        .thenReturn("testFilter secondHost");
    when(metricStoreService.saveNewRelicMetrics(anyString(), anyString(), anyString(), anyString(), any(List.class)))
        .thenReturn(true);
    String infrastructureMappingId = UUID.randomUUID().toString();
    String timeDuration = "10";
    dataCollectionInfo = buildDataCollectionInfo();

    DelegateTask task = DelegateTask.builder()
                            .async(true)
                            .accountId(accountId)
                            .appId(appId)
                            .waitId(waitId)
                            .data(TaskData.builder()
                                      .taskType(TaskType.STACKDRIVER_COLLECT_METRIC_DATA.name())
                                      .parameters(new Object[] {dataCollectionInfo})
                                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                                      .build())
                            .envId(envId)
                            .infrastructureMappingId(infrastructureMappingId)
                            .build();
    task.setUuid(delegateId);
    dataCollectionTask = new StackDriverDataCollectionTask(delegateId, task, null, null);
    FieldUtils.writeField(dataCollectionTask, "dataCollectionService", dataCollectionService, true);
    FieldUtils.writeField(dataCollectionTask, "encryptionService", encryptionService, true);
    FieldUtils.writeField(dataCollectionTask, "stackDriverDelegateService", stackDriverDelegateService, true);
    FieldUtils.writeField(dataCollectionTask, "metricStoreService", metricStoreService, true);
  }

  private StackDriverDataCollectionInfo buildDataCollectionInfo() {
    Map<String, String> groupHostsMap = new HashMap<>();
    groupHostsMap.put("dummyHost", "default");
    groupHostsMap.put("secondHost", "default");

    return StackDriverDataCollectionInfo.builder()
        .collectionTime(10)
        .hosts(groupHostsMap)
        .applicationId(appId)
        .stateExecutionId(stateExecutionId)
        .initialDelayMinutes(0)
        .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
        .podMetrics(Arrays.asList(StackDriverMetric.builder()
                                      .metricName("kubernetes.io/container/memory/request_utilization")
                                      .metric("MemoryRequestUtilization")
                                      .displayName("Memory Request Utilization")
                                      .unit("number")
                                      .kind("VALUE")
                                      .valueType("Int64")
                                      .build()))
        .gcpConfig(GcpConfig.builder().accountId(accountId).build())
        .startTime(Timestamp.currentMinuteBoundary() - TimeUnit.MINUTES.toMillis(2))
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetrics() throws Exception {
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(dataCollectionInfo);
    dataCollectionTask.getDataCollector(taskResult).run();
    ArgumentCaptor<List> taskCaptor = ArgumentCaptor.forClass(List.class);
    verify(dataCollectionService).executeParrallel(taskCaptor.capture());
    assertThat(taskCaptor.getValue().size()).isEqualTo(2);
  }
}
