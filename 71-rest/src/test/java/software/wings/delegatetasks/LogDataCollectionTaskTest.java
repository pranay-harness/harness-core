package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.logs.LogResponseParserTest;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.io.IOException;
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
  @Mock private EncryptionService encryptionService;
  @Mock private RequestExecutor requestExecutor;
  private LogDataCollectionTask dataCollectionTask;

  public void setup(Map<String, Map<String, ResponseMapper>> logDefinition, Set<String> hosts) throws Exception {
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
    FieldUtils.writeField(dataCollectionTask, "encryptionService", encryptionService, true);
    FieldUtils.writeField(dataCollectionTask, "requestExecutor", requestExecutor, true);

    when(encryptionService.getDecryptedValue(any())).thenReturn("decryptedApiKey".toCharArray());
  }

  private CustomLogDataCollectionInfo getDataCollectionInfo(
      Map<String, Map<String, ResponseMapper>> logDefinition, Set<String> hosts) {
    Map<String, String> header = new HashMap<>();
    header.put("Content-Type", "application/json");
    return CustomLogDataCollectionInfo.builder()
        .startTime(12312321123L)
        .collectionFrequency(1)
        .hosts(hosts)
        .encryptedDataDetails(Arrays.asList(EncryptedDataDetail.builder().fieldName("apiKey").build()))
        .startMinute(0)
        .responseDefinition(logDefinition)
        .headers(header)
        .stateExecutionId("12345asdaf")
        .baseUrl("http://ec2-34-227-84-170.compute-1.amazonaws.com:9200/integration-test/")
        .shouldDoHostBasedFiltering(true)
        .build();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testFetchElkLogs() throws Exception {
    // setup
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/elkMultipleHitsResponse.json"), Charsets.UTF_8);
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
    when(requestExecutor.executeRequest(any(), any(), any())).thenReturn(textLoad);
    // execute
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(dataCollectionInfo);
    Runnable r = dataCollectionTask.getDataCollector(taskResult);
    r.run();

    // verify
    // verify
    ArgumentCaptor<Map> maskPatternsCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Call> requestCaptor = ArgumentCaptor.forClass(Call.class);

    verify(requestExecutor, times(3))
        .executeRequest(any(ThirdPartyApiCallLog.class), requestCaptor.capture(), maskPatternsCaptor.capture());
    List<Map> maskPatterns = maskPatternsCaptor.getAllValues();
    List<Call> callsList = requestCaptor.getAllValues();
    maskPatterns.forEach(
        maskPatternsMap -> assertThat(((Map<String, String>) maskPatternsMap).containsKey("decryptedApiKey")).isTrue());
    callsList.forEach(call -> assertThat(call.request().url().toString().contains("apiKey=decryptedApiKey")));

    verify(logAnalysisStoreService, times(1))
        .save(any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), any(List.class));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testFetchLogs_shouldNotInspectHosts() throws Exception {
    // setup
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/elkMultipleHitsResponse.json"), Charsets.UTF_8);
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
    when(requestExecutor.executeRequest(any(), any(), any())).thenReturn(textLoad);
    dataCollectionInfo.setShouldDoHostBasedFiltering(false);
    // execute
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(dataCollectionInfo);
    Runnable r = dataCollectionTask.getDataCollector(taskResult);
    r.run();

    // verify
    // verify
    ArgumentCaptor<Map> maskPatternsCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Call> requestCaptor = ArgumentCaptor.forClass(Call.class);

    // since the flag is set in datacollectionInfo, the number of invocations should be 1 only.
    verify(requestExecutor, times(1))
        .executeRequest(any(ThirdPartyApiCallLog.class), requestCaptor.capture(), maskPatternsCaptor.capture());
    List<Map> maskPatterns = maskPatternsCaptor.getAllValues();
    List<Call> callsList = requestCaptor.getAllValues();
    maskPatterns.forEach(
        maskPatternsMap -> assertThat(((Map<String, String>) maskPatternsMap).containsKey("decryptedApiKey")).isTrue());
    callsList.forEach(call -> assertThat(call.request().url().toString().contains("apiKey=decryptedApiKey")));

    verify(logAnalysisStoreService, times(1))
        .save(any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), any(List.class));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  //@Ignore("Ignored until this test is moved to not use ELK server")
  public void testFetchElkLogsRetry() throws Exception {
    String textLoad = Resources.toString(
        LogResponseParserTest.class.getResource("/apm/elkMultipleHitsResponse.json"), Charsets.UTF_8);

    // setup

    String searchUrl = "_search?pretty=true&q=*&size=5&apiKey=${apiKey}";
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
    when(requestExecutor.executeRequest(any(), any(), any())).thenReturn(textLoad);
    // execute
    DataCollectionTaskResult taskResult = dataCollectionTask.initDataCollection(dataCollectionInfo);
    Runnable r = dataCollectionTask.getDataCollector(taskResult);
    r.run();

    // verify
    ArgumentCaptor<Map> maskPatternsCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Call> requestCaptor = ArgumentCaptor.forClass(Call.class);

    verify(requestExecutor, times(6))
        .executeRequest(any(ThirdPartyApiCallLog.class), requestCaptor.capture(), maskPatternsCaptor.capture());
    List<Map> maskPatterns = maskPatternsCaptor.getAllValues();
    List<Call> callsList = requestCaptor.getAllValues();
    maskPatterns.forEach(
        maskPatternsMap -> assertThat(((Map<String, String>) maskPatternsMap).containsKey("decryptedApiKey")).isTrue());
    callsList.forEach(call -> assertThat(call.request().url().toString().contains("apiKey=decryptedApiKey")));

    verify(logAnalysisStoreService, times(2))
        .save(any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), any(List.class));
  }
}
