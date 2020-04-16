package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.TaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LogDataCollectionTaskTest extends CategoryTest {
  CustomLogDataCollectionInfo dataCollectionInfo;
  @Mock private LogAnalysisStoreService logAnalysisStoreService;
  @Mock private DelegateLogService delegateLogService;
  @Mock private ScheduledFuture future;
  private LogDataCollectionTask dataCollectionTask;

  public void setup(Map<String, Map<String, ResponseMapper>> logDefinition, Set<String> hosts)
      throws IllegalAccessException {
    String delegateId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String waitId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String infrastructureMappingId = UUID.randomUUID().toString();
    String timeDuration = "10";
    dataCollectionInfo = getDataCollectionInfo(logDefinition, hosts);

    DelegateTask task = DelegateTask.builder()
                            .accountId(accountId)
                            .appId(appId)
                            .waitId(waitId)
                            .data(TaskData.builder()
                                      .async(true)
                                      .taskType(TaskType.CUSTOM_LOG_COLLECTION_TASK.name())
                                      .parameters(new Object[] {dataCollectionInfo})
                                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                                      .build())
                            .envId(envId)
                            .infrastructureMappingId(infrastructureMappingId)
                            .build();
    dataCollectionTask = new LogDataCollectionTask(delegateId, task, null, null);
    MockitoAnnotations.initMocks(this);

    when(future.cancel(anyBoolean())).thenReturn(true);
    FieldUtils.writeField(dataCollectionTask, "future", future, true);
    FieldUtils.writeField(dataCollectionTask, "delegateLogService", delegateLogService, true);
    FieldUtils.writeField(dataCollectionTask, "logAnalysisStoreService", logAnalysisStoreService, true);
  }

  private CustomLogDataCollectionInfo getDataCollectionInfo(
      Map<String, Map<String, ResponseMapper>> logDefinition, Set<String> hosts) {
    Map<String, String> header = new HashMap<>();
    header.put("Content-Type", "application/json");
    return CustomLogDataCollectionInfo.builder()
        .startTime(12312321123L)
        .collectionFrequency(1)
        .hosts(hosts)
        .encryptedDataDetails(new ArrayList<>())
        .startMinute(0)
        .responseDefinition(logDefinition)
        .headers(header)
        .stateExecutionId("12345asdaf")
        .baseUrl("http://ec2-34-227-84-170.compute-1.amazonaws.com:9200/integration-test/")
        .build();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  @Ignore("Ignored until this test is moved to not use ELK server")
  public void testFetchElkLogs() throws IOException, IllegalAccessException {
    // setup

    String searchUrl = "_search?pretty=true&q=*&size=5";
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("hits.hits[*]._source.timestamp"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .jsonPath(Arrays.asList("hits.hits[*]._source.host"))
            .build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("hits.hits[*]._source.title"))
            .build());
    Map<String, Map<String, ResponseMapper>> logDefinition = new HashMap<>();
    logDefinition.put(searchUrl, responseMappers);
    setup(logDefinition, new HashSet<>(Arrays.asList("test.hostname.2", "test.hostname.22", "test.hostname.12")));
    doNothing().when(delegateLogService).save(anyString(), any(ThirdPartyApiCallLog.class));
    when(logAnalysisStoreService.save(any(StateType.class), anyString(), anyString(), anyString(), anyString(),
             anyString(), anyString(), anyString(), anyString(), any(List.class)))
        .thenReturn(true);

    // execute
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(dataCollectionInfo);
    Runnable r = dataCollectionTask.getDataCollector(taskResult);
    r.run();

    // verify
    verify(logAnalysisStoreService, times(1))
        .save(any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), any(List.class));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  @Ignore("Ignored until this test is moved to not use ELK server")
  public void testFetchElkLogsRetry() throws IOException, IllegalAccessException {
    // setup

    String searchUrl = "_search?pretty=true&q=*&size=5";
    Map<String, ResponseMapper> responseMappers = new HashMap<>();
    responseMappers.put("timestamp",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("timestamp")
            .jsonPath(Arrays.asList("hits.hits[*]._source.timestamp"))
            .timestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .build());
    responseMappers.put("host",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("host")
            .jsonPath(Arrays.asList("hits.hits[*]._source.host"))
            .build());
    responseMappers.put("logMessage",
        CustomLogVerificationState.ResponseMapper.builder()
            .fieldName("logMessage")
            .jsonPath(Arrays.asList("hits.hits[*]._source.title"))
            .build());
    Map<String, Map<String, ResponseMapper>> logDefinition = new HashMap<>();
    logDefinition.put(searchUrl, responseMappers);
    setup(logDefinition, new HashSet<>(Arrays.asList("test.hostname.2", "test.hostname.22", "test.hostname.12")));
    doNothing().when(delegateLogService).save(anyString(), any(ThirdPartyApiCallLog.class));
    when(logAnalysisStoreService.save(any(StateType.class), anyString(), anyString(), anyString(), anyString(),
             anyString(), anyString(), anyString(), anyString(), any(List.class)))
        .thenThrow(new IOException("This is bad"))
        .thenReturn(true);

    // execute
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(dataCollectionInfo);
    Runnable r = dataCollectionTask.getDataCollector(taskResult);
    r.run();

    // verify
    verify(logAnalysisStoreService, times(2))
        .save(any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), any(List.class));
  }
}
