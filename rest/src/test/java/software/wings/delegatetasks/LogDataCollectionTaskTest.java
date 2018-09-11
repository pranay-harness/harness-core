package software.wings.delegatetasks;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
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
import java.util.concurrent.TimeUnit;

public class LogDataCollectionTaskTest {
  private static final Logger logger = LoggerFactory.getLogger(LogDataCollectionTaskTest.class);

  CustomLogDataCollectionInfo dataCollectionInfo;
  @Mock private LogAnalysisStoreService logAnalysisStoreService;
  @Mock private DelegateLogService delegateLogService;
  private LogDataCollectionTask dataCollectionTask;

  public void setup(Map<String, Map<String, ResponseMapper>> logDefinition, Set<String> hosts) {
    String delegateId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String waitId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String infrastructureMappingId = UUID.randomUUID().toString();
    String timeDuration = "10";
    dataCollectionInfo = getDataCollectionInfo(logDefinition, hosts);

    DelegateTask task = aDelegateTask()
                            .withTaskType(TaskType.CUSTOM_LOG_COLLECTION_TASK)
                            .withAccountId(accountId)
                            .withAppId(appId)
                            .withWaitId(waitId)
                            .withParameters(new Object[] {dataCollectionInfo})
                            .withEnvId(envId)
                            .withInfrastructureMappingId(infrastructureMappingId)
                            .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                            .build();
    dataCollectionTask = new LogDataCollectionTask(delegateId, task, null, null);
    MockitoAnnotations.initMocks(this);
    setInternalState(dataCollectionTask, "delegateLogService", delegateLogService);
    setInternalState(dataCollectionTask, "logAnalysisStoreService", logAnalysisStoreService);
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
  public void testFetchElkLogs() throws IOException {
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
             anyString(), anyString(), anyString(), any(List.class)))
        .thenReturn(true);

    // execute
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(new Object[] {dataCollectionInfo});
    Runnable r = dataCollectionTask.getDataCollector(taskResult);
    r.run();

    // verify
    verify(logAnalysisStoreService, times(1))
        .save(any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(List.class));
  }

  @Test
  public void testFetchElkLogsRetry() throws IOException {
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
             anyString(), anyString(), anyString(), any(List.class)))
        .thenThrow(new IOException("This is bad"))
        .thenReturn(true);

    // execute
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(new Object[] {dataCollectionInfo});
    Runnable r = dataCollectionTask.getDataCollector(taskResult);
    r.run();

    // verify
    verify(logAnalysisStoreService, times(2))
        .save(any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), any(List.class));
  }
}
