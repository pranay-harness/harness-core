package io.harness.perpetualtask.internal;

import static io.harness.perpetualtask.internal.PerpetualTaskRecordHandler.FAIL_TO_ASSIGN_ANY_DELEGATE_TO_PERPETUAL_TASK;
import static io.harness.perpetualtask.internal.PerpetualTaskRecordHandler.NO_DELEGATES_INSTALLED_TO_HANDLE_PERPETUAL_TASK;
import static io.harness.perpetualtask.internal.PerpetualTaskRecordHandler.NO_DELEGATE_AVAILABLE_TO_HANDLE_PERPETUAL_TASK;
import static io.harness.perpetualtask.internal.PerpetualTaskRecordHandler.PERPETUAL_TASK_FAILED_TO_BE_ASSIGNED_TO_ANY_DELEGATE;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.MATT;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.exception.WingsException;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import io.harness.rule.Owner;

import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.PerpetualTaskAlert;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class PerpetualTaskRecordHandlerTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String taskId = "TASK_ID";
  private String delegateId = "DELEGATE_ID";
  private PerpetualTaskRecord record;

  private DelegateTask delegateTask = DelegateTask.builder().build();
  @Mock K8sWatchPerpetualTaskServiceClient k8sWatchPerpetualTaskServiceClient;
  @Mock PerpetualTaskServiceClientRegistry clientRegistry;
  @Mock DelegateService delegateService;
  @Mock PerpetualTaskService perpetualTaskService;
  @Mock PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Mock AlertService alertService;
  @InjectMocks PerpetualTaskRecordHandler perpetualTaskRecordHandler;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws InterruptedException {
    Map<String, String> clientParams = new HashMap<>();
    clientParams.put("test", "test");
    record = PerpetualTaskRecord.builder()
                 .accountId(accountId)
                 .uuid(taskId)
                 .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                 .clientContext(PerpetualTaskClientContext.builder().clientParams(clientParams).build())
                 .build();
    when(clientRegistry.getClient(isA(String.class))).thenReturn(k8sWatchPerpetualTaskServiceClient);
    when(k8sWatchPerpetualTaskServiceClient.getValidationTask(isA(PerpetualTaskClientContext.class), anyString()))
        .thenReturn(delegateTask);

    doNothing().when(perpetualTaskService).appointDelegate(eq(accountId), anyString(), eq(delegateId), anyLong());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testHandle() throws InterruptedException {
    DelegateTaskNotifyResponseData response = AssignmentTaskResponse.builder()
                                                  .delegateId(delegateId)
                                                  .delegateMetaInfo(DelegateMetaInfo.builder().id(delegateId).build())
                                                  .build();
    when(delegateService.executeTask(isA(DelegateTask.class))).thenReturn(response);
    perpetualTaskRecordHandler.assign(record);
    verify(perpetualTaskService).appointDelegate(eq(accountId), anyString(), eq(delegateId), anyLong());
    verify(alertService, times(1))
        .closeAlert(eq(accountId), eq(null), eq(AlertType.PerpetualTaskAlert),
            eq(PerpetualTaskAlert.builder()
                    .accountId(accountId)
                    .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                    .build()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testPerpetualTaskServiceClientGetValidationTask() {
    Map<String, String> clientParams = new HashMap<>();
    clientParams.put("test", "test");
    PerpetualTaskRecord record =
        PerpetualTaskRecord.builder()
            .accountId(accountId)
            .uuid(taskId)
            .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
            .clientContext(PerpetualTaskClientContext.builder().clientParams(clientParams).build())
            .build();

    when(clientRegistry.getClient(isA(String.class))).thenReturn(k8sWatchPerpetualTaskServiceClient);
    when(k8sWatchPerpetualTaskServiceClient.getValidationTask(isA(PerpetualTaskClientContext.class), anyString()))
        .thenReturn(delegateTask);

    DelegateTask realDelegateTask = perpetualTaskRecordHandler.getValidationTask(record);

    assertThat(realDelegateTask).isNotNull();
    assertThat(realDelegateTask).isEqualTo(delegateTask);

    verify(k8sWatchPerpetualTaskServiceClient, atLeastOnce())
        .getValidationTask(isA(PerpetualTaskClientContext.class), anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldNotHandle() throws InterruptedException {
    RemoteMethodReturnValueData response = RemoteMethodReturnValueData.builder().build();
    when(delegateService.executeTask(isA(DelegateTask.class))).thenReturn(response);
    perpetualTaskRecordHandler.assign(record);
    verify(perpetualTaskService, times(0)).appointDelegate(eq(accountId), anyString(), eq(delegateId), anyLong());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotHandle_NoDelegateAvailableToHandlePerpetualTask() throws InterruptedException {
    when(delegateService.executeTask(isA(DelegateTask.class))).thenThrow(new NoAvailableDelegatesException());
    perpetualTaskRecordHandler.assign(record);
    String expectedMessage =
        String.format(NO_DELEGATE_AVAILABLE_TO_HANDLE_PERPETUAL_TASK, record.getPerpetualTaskType());
    verify(alertService, times(1))
        .openAlert(eq(accountId), eq(null), eq(AlertType.PerpetualTaskAlert),
            eq(PerpetualTaskAlert.builder()
                    .accountId(accountId)
                    .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                    .message(expectedMessage)
                    .build()));
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldNotHandle_ServiceUnavailableNoDelegateInstalledToHandlePT() throws InterruptedException {
    when(delegateService.executeTask(isA(DelegateTask.class))).thenThrow(new NoInstalledDelegatesException());
    perpetualTaskRecordHandler.assign(record);
    String expectedMessage =
        String.format(NO_DELEGATES_INSTALLED_TO_HANDLE_PERPETUAL_TASK, record.getPerpetualTaskType());
    verify(alertService, times(1))
        .openAlert(eq(accountId), eq(null), eq(AlertType.PerpetualTaskAlert),
            eq(PerpetualTaskAlert.builder()
                    .accountId(accountId)
                    .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                    .message(expectedMessage)
                    .build()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotHandle_PerpetualTaskFailedToBeAssignedToAnyDelegate() throws InterruptedException {
    when(delegateService.executeTask(isA(DelegateTask.class))).thenThrow(new WingsException(""));
    perpetualTaskRecordHandler.assign(record);
    String expectedMessage =
        String.format(PERPETUAL_TASK_FAILED_TO_BE_ASSIGNED_TO_ANY_DELEGATE, record.getPerpetualTaskType());
    verify(alertService, times(1))
        .openAlert(eq(accountId), eq(null), eq(AlertType.PerpetualTaskAlert),
            eq(PerpetualTaskAlert.builder()
                    .accountId(accountId)
                    .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                    .message(expectedMessage)
                    .build()));
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldNotHandle_FailToAssignAnyDelegateToPerpetualTask() throws InterruptedException {
    when(delegateService.executeTask(isA(DelegateTask.class))).thenThrow(new RuntimeException());
    perpetualTaskRecordHandler.assign(record);
    String expectedMessage =
        String.format(FAIL_TO_ASSIGN_ANY_DELEGATE_TO_PERPETUAL_TASK, record.getPerpetualTaskType());
    verify(alertService, times(1))
        .openAlert(eq(accountId), eq(null), eq(AlertType.PerpetualTaskAlert),
            eq(PerpetualTaskAlert.builder()
                    .accountId(accountId)
                    .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                    .message(expectedMessage)
                    .build()));
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testReassign() {
    record = PerpetualTaskRecord.builder()
                 .accountId(accountId)
                 .delegateId(delegateId)
                 .uuid(taskId)
                 .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                 .clientContext(PerpetualTaskClientContext.builder().build())
                 .build();
    when(delegateService.checkDelegateConnected(accountId, delegateId)).thenReturn(true);
    perpetualTaskRecordHandler.rebalance(record);
    verify(perpetualTaskService).appointDelegate(eq(accountId), eq(taskId), eq(delegateId), anyLong());
  }
}
