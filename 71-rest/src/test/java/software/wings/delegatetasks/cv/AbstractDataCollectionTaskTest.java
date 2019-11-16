package software.wings.delegatetasks.cv;

import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.inject.Injector;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.rule.OwnerRule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response.Builder;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.annotation.EncryptableSetting;
import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.delegatetasks.DelegateCVActivityLogService.Logger;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AbstractDataCollectionTaskTest extends CategoryTest {
  @Mock private DataCollectorFactory dataCollectorFactory;
  @Mock private Injector injector;
  @Mock EncryptionService encryptionService;
  @Mock private DelegateCVActivityLogService delegateCVActivityLogService;
  @Mock private Logger logger;
  @Mock private DelegateLogService delegateLogService;
  private AbstractDataCollectionTask<DataCollectionInfoV2> abstractDataCollectionTask;

  @Before
  public void setupTests() throws IllegalAccessException, DataCollectionException {
    initMocks(this);
    abstractDataCollectionTask = mock(AbstractDataCollectionTask.class, Mockito.CALLS_REAL_METHODS);
    when(delegateCVActivityLogService.getLogger(any(), any(), anyLong(), any(), any(), anyVararg())).thenReturn(logger);
    FieldUtils.writeField(abstractDataCollectionTask, "dataCollectorFactory", dataCollectorFactory, true);
    FieldUtils.writeField(abstractDataCollectionTask, "injector", injector, true);
    FieldUtils.writeField(abstractDataCollectionTask, "cvActivityLogService", delegateCVActivityLogService, true);
    FieldUtils.writeField(abstractDataCollectionTask, "encryptionService", encryptionService, true);
    FieldUtils.writeField(abstractDataCollectionTask, "delegateLogService", delegateLogService, true);
    AbstractDataCollectionTask.RETRY_SLEEP_DURATION = Duration.ofMillis(1); // to run retry based test faster.
  }
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCallToInitAndCollectAndSaveDataWithCorrectParams()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    verify(dataCollector, times(1)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(1)).collectAndSaveData(dataCollectionInfo);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDecryptionOfSettingsIfEncryptableSettingIsPresent() {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    EncryptableSetting encryptableSetting = mock(EncryptableSetting.class);
    List<EncryptedDataDetail> encryptedDataDetails = mock(List.class);
    when(dataCollectionInfo.getEncryptableSetting()).thenReturn(Optional.of(encryptableSetting));
    when(dataCollectionInfo.getEncryptedDataDetails()).thenReturn(encryptedDataDetails);
    abstractDataCollectionTask.run(dataCollectionInfo);
    verify(encryptionService, times(1)).decrypt(eq(encryptableSetting), eq(encryptedDataDetails));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDecryptionIfEncryptableSettingIsNotPresent() {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    when(dataCollectionInfo.getEncryptableSetting()).thenReturn(Optional.empty());
    abstractDataCollectionTask.run(dataCollectionInfo);
    verifyZeroInteractions(encryptionService);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCorrectTaskResultIfNoFailure()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    assertThat(DataCollectionTaskStatus.SUCCESS).isEqualTo(taskResult.getStatus());
    assertThat(StateType.SPLUNKV2).isEqualTo(taskResult.getStateType());
    assertThat(taskResult.getErrorMessage()).isNull();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCorrectTaskResultIfLessThenRetryCountFailures()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    doThrow(new RuntimeException("error message from test")).doNothing().when(dataCollector).init(any(), any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    verify(dataCollector, times(2)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(1)).collectAndSaveData(dataCollectionInfo);
    assertThat(DataCollectionTaskStatus.SUCCESS).isEqualTo(taskResult.getStatus());
    assertThat(StateType.SPLUNKV2).isEqualTo(taskResult.getStateType());
    assertThat("error message from test").isEqualTo(taskResult.getErrorMessage());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testStatusFailureInCaseOfExceptionOnInitWithRetryCount()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    doThrow(new RuntimeException("error message from test")).when(dataCollector).init(any(), any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    verify(dataCollector, times(4)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(0)).collectAndSaveData(dataCollectionInfo);
    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    assertThat(DataCollectionTaskStatus.FAILURE).isEqualTo(taskResult.getStatus());
    assertThat(StateType.SPLUNKV2).isEqualTo(taskResult.getStateType());
    assertThat("error message from test").isEqualTo(taskResult.getErrorMessage());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testStatusFailureInCaseOfExceptionOnCollectAndSaveWithRetryCount()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    doThrow(new RuntimeException("error message from test")).when(abstractDataCollectionTask).collectAndSaveData(any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    verify(dataCollector, times(4)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(4)).collectAndSaveData(dataCollectionInfo);
    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    assertThat(DataCollectionTaskStatus.FAILURE).isEqualTo(taskResult.getStatus());
    assertThat(StateType.SPLUNKV2).isEqualTo(taskResult.getStateType());
    assertThat("error message from test").isEqualTo(taskResult.getErrorMessage());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testActivityLogOnTaskFailure()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    doThrow(new RuntimeException("error message from test")).when(abstractDataCollectionTask).collectAndSaveData(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    verify(logger, times(1)).error(eq("Data collection failed with exception: error message from test"));
    verify(logger, times(4)).warn(eq("[Retrying] Data collection task failed with exception: error message from test"));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testActivityLogOnTaskSuccess()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    verify(logger, times(1)).info(eq("Starting data collection."));
    verify(logger, times(1)).info(eq("Finished data collection with status: SUCCESS"));
  }
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void executeRequestAndGenerateCorrectThirdPartyAPILogs()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             IOException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    ArgumentCaptor<DataCollectionExecutionContext> executionContextArgumentCaptor =
        ArgumentCaptor.forClass(DataCollectionExecutionContext.class);
    verify(dataCollector).init(executionContextArgumentCaptor.capture(), any());
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    when(call.execute()).thenReturn(response);
    String returnedStr = executionContextArgumentCaptor.getValue().executeRequest("title", call);
    assertThat(returnedStr).isEqualTo(responseStr);
    ArgumentCaptor<ThirdPartyApiCallLog> thirdPartyApiCallLogArgumentCaptor =
        ArgumentCaptor.forClass(ThirdPartyApiCallLog.class);
    verify(delegateLogService)
        .save(eq(dataCollectionInfo.getAccountId()), thirdPartyApiCallLogArgumentCaptor.capture());
    ThirdPartyApiCallLog thirdPartyApiCallLog = thirdPartyApiCallLogArgumentCaptor.getValue();
    assertThat(thirdPartyApiCallLog.getTitle()).isEqualTo("title");
    assertThat(thirdPartyApiCallLog.getRequest().get(0).getName()).isEqualTo("Url");
    assertThat(thirdPartyApiCallLog.getRequest().get(0).getValue()).isEqualTo("http://example.com/test");
    assertThat(thirdPartyApiCallLog.getRequest().get(0).getType()).isEqualTo(FieldType.URL);
    assertThat(thirdPartyApiCallLog.getResponse().get(0).getValue()).isEqualTo("200");
    assertThat(thirdPartyApiCallLog.getResponse().get(0).getType()).isEqualTo(FieldType.NUMBER);
    assertThat(thirdPartyApiCallLog.getResponse().get(0).getName()).isEqualTo("Status Code");
    assertThat(thirdPartyApiCallLog.getResponse().get(1).getValue()).isEqualTo(responseStr);
    assertThat(thirdPartyApiCallLog.getResponse().get(1).getType()).isEqualTo(FieldType.JSON);
    assertThat(thirdPartyApiCallLog.getResponse().get(1).getName()).isEqualTo("Response Body");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void executeRequestWithRetryIfException() throws InvocationTargetException, NoSuchMethodException,
                                                          InstantiationException, IllegalAccessException, IOException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    ArgumentCaptor<DataCollectionExecutionContext> executionContextArgumentCaptor =
        ArgumentCaptor.forClass(DataCollectionExecutionContext.class);
    verify(dataCollector).init(executionContextArgumentCaptor.capture(), any());
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    when(call.execute()).thenThrow(new IOException("exception from test"));
    assertThatThrownBy(() -> executionContextArgumentCaptor.getValue().executeRequest("title", call))
        .isInstanceOf(DataCollectionException.class);

    ArgumentCaptor<ThirdPartyApiCallLog> thirdPartyApiCallLogArgumentCaptor =
        ArgumentCaptor.forClass(ThirdPartyApiCallLog.class);
    verify(delegateLogService, times(4))
        .save(eq(dataCollectionInfo.getAccountId()), thirdPartyApiCallLogArgumentCaptor.capture());
    List<ThirdPartyApiCallLog> thirdPartyApiCallLogs = thirdPartyApiCallLogArgumentCaptor.getAllValues();
    assertThat(thirdPartyApiCallLogs.size()).isEqualTo(4);
    for (int i = 0; i < thirdPartyApiCallLogs.size(); i++) {
      ThirdPartyApiCallLog thirdPartyApiCallLog = thirdPartyApiCallLogs.get(i);
      assertThat(thirdPartyApiCallLog.getRequest().get(0).getValue()).isEqualTo("http://example.com/test");
      if (i != 0) {
        assertThat(thirdPartyApiCallLog.getRequest().get(1).getName()).isEqualTo("RETRY");
        assertThat(thirdPartyApiCallLog.getRequest().get(1).getValue()).isEqualTo(String.valueOf(i)); // retry count
        assertThat(thirdPartyApiCallLog.getRequest().get(1).getType()).isEqualTo(FieldType.NUMBER);
      }
      assertThat(thirdPartyApiCallLog.getResponse().get(0).getType()).isEqualTo(FieldType.NUMBER);
      assertThat(thirdPartyApiCallLog.getResponse().get(0).getValue()).isEqualTo("400");
      assertThat(thirdPartyApiCallLog.getResponse().get(0).getName()).isEqualTo("Status Code");
      assertThat(thirdPartyApiCallLog.getResponse().get(1).getType()).isEqualTo(FieldType.TEXT);
      assertThat(thirdPartyApiCallLog.getResponse().get(1).getValue()).contains("exception from test");
      assertThat(thirdPartyApiCallLog.getResponse().get(1).getName()).isEqualTo("Response Body");
    }
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testExecuteRequestRetrySuccessOnRateLimitExceeded()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             IOException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    ArgumentCaptor<DataCollectionExecutionContext> executionContextArgumentCaptor =
        ArgumentCaptor.forClass(DataCollectionExecutionContext.class);
    verify(dataCollector).init(executionContextArgumentCaptor.capture(), any());
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    Response<String> rateLimitResponse = tooManyRequestsResponse(responseStr);
    when(call.execute()).thenReturn(rateLimitResponse).thenReturn(response);
    String returnedStr = executionContextArgumentCaptor.getValue().executeRequest("title", call);
    assertThat(returnedStr).isEqualTo(responseStr);
    ArgumentCaptor<ThirdPartyApiCallLog> thirdPartyApiCallLogArgumentCaptor =
        ArgumentCaptor.forClass(ThirdPartyApiCallLog.class);
    verify(delegateLogService, times(2))
        .save(eq(dataCollectionInfo.getAccountId()), thirdPartyApiCallLogArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testExecuteRequestRetryFailureAfterMaxRetriesOnRateLimitExceeded()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             IOException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    ArgumentCaptor<DataCollectionExecutionContext> executionContextArgumentCaptor =
        ArgumentCaptor.forClass(DataCollectionExecutionContext.class);
    verify(dataCollector).init(executionContextArgumentCaptor.capture(), any());
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> rateLimitResponse = tooManyRequestsResponse(responseStr);
    when(call.execute()).thenReturn(rateLimitResponse);
    assertThatThrownBy(() -> executionContextArgumentCaptor.getValue().executeRequest("title", call))
        .isInstanceOf(DataCollectionException.class);
  }

  private Response tooManyRequestsResponse(String responseStr) {
    return Response.error(ResponseBody.create(MediaType.parse("text/plain"), responseStr),
        new Builder()
            .code(429)
            .protocol(Protocol.HTTP_1_1)
            .message("test")
            .request(new Request.Builder().url("http://localhost/").build())
            .build());
  }

  private DataCollectionInfoV2 createDataCollectionInfo() {
    StateType stateType = StateType.SPLUNKV2;
    DataCollectionInfoV2 dataCollectionInfoV2 = mock(DataCollectionInfoV2.class);
    when(dataCollectionInfoV2.getStateType()).thenReturn(stateType);
    when(dataCollectionInfoV2.getEncryptableSetting()).thenReturn(Optional.empty());
    when(dataCollectionInfoV2.getEncryptedDataDetails()).thenReturn(Lists.newArrayList());
    when(dataCollectionInfoV2.getAccountId()).thenReturn(UUID.randomUUID().toString());
    when(dataCollectionInfoV2.getStateExecutionId()).thenReturn(UUID.randomUUID().toString());
    when(dataCollectionInfoV2.getStartTime()).thenReturn(Instant.now().minus(5, ChronoUnit.MINUTES));
    when(dataCollectionInfoV2.getEndTime()).thenReturn(Instant.now());
    return dataCollectionInfoV2;
  }

  // TODO: write test for saving and create third party call logs.
}
