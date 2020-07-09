package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.FeatureName.REVALIDATE_WHITELISTED_DELEGATE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import com.sun.tools.javac.util.List;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.tasks.Cd1SetupFields;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.BatchDelegateSelectionLog;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateBuilder;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.TaskType;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DelegateServiceImplTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  @Mock private Broadcaster broadcaster;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private DelegateTaskBroadcastHelper broadcastHelper;
  @InjectMocks @Inject private DelegateServiceImpl delegateService;
  @InjectMocks @Inject private DelegateSyncServiceImpl delegateSyncService;
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateSelectionLogsService delegateSelectionLogsService;
  @InjectMocks @Spy private DelegateServiceImpl spydelegateService;

  @Before
  public void setUp() {
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
  }

  private DelegateBuilder createDelegateBuilder() {
    return Delegate.builder()
        .accountId(ACCOUNT_ID)
        .ip("127.0.0.1")
        .hostName("localhost")
        .version(VERSION)
        .status(Status.ENABLED)
        .lastHeartBeat(System.currentTimeMillis());
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExecuteTask() {
    Delegate delegate = createDelegateBuilder().build();
    wingsPersistence.save(delegate);
    DelegateTask delegateTask = getDelegateTask();
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().build();
    when(delegateSelectionLogsService.createBatch(delegateTask)).thenReturn(batch);
    when(assignDelegateService.canAssign(eq(batch), anyString(), any())).thenReturn(true);
    when(assignDelegateService.retrieveActiveDelegates(
             eq(delegateTask.getAccountId()), any(BatchDelegateSelectionLog.class)))
        .thenReturn(List.of(delegate.getUuid()));
    Thread thread = new Thread(() -> {
      await().atMost(5L, TimeUnit.SECONDS).until(() -> isNotEmpty(delegateSyncService.syncTaskWaitMap));
      DelegateTask task =
          wingsPersistence.createQuery(DelegateTask.class).filter("accountId", delegateTask.getAccountId()).get();
      delegateService.processDelegateResponse(task.getAccountId(), delegate.getUuid(), task.getUuid(),
          DelegateTaskResponse.builder()
              .accountId(task.getAccountId())
              .response(HttpStateExecutionResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build())
              .responseCode(ResponseCode.OK)
              .build());
      new Thread(delegateSyncService).start();
    });
    thread.start();
    ResponseData responseData = delegateService.executeTask(delegateTask);
    assertThat(responseData instanceof HttpStateExecutionResponse).isTrue();
    HttpStateExecutionResponse httpResponse = (HttpStateExecutionResponse) responseData;
    assertThat(httpResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithPreAssignedDelegateId_Sync() {
    DelegateTask delegateTask = getDelegateTask();
    delegateTask.getData().setAsync(false);
    delegateService.saveDelegateTask(delegateTask);
    assertThat(delegateTask.getBroadcastCount()).isEqualTo(0);
    verify(broadcastHelper, times(0)).rebroadcastDelegateTask(any());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithPreAssignedDelegateId_Async() {
    DelegateTask delegateTask = getDelegateTask();
    delegateTask.getData().setAsync(true);
    delegateService.saveDelegateTask(delegateTask);
    assertThat(delegateTask.getBroadcastCount()).isEqualTo(0);
    verify(broadcastHelper, times(0)).rebroadcastDelegateTask(any());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldObtainDelegateName() {
    String delegateId = generateUuid();
    assertThat(delegateService.obtainDelegateName(null, delegateId, true)).isEqualTo("");
    assertThat(delegateService.obtainDelegateName("accountId", delegateId, true)).isEqualTo(delegateId);

    DelegateBuilder delegateBuilder = Delegate.builder();

    delegateService.add(delegateBuilder.uuid(delegateId).build());
    assertThat(delegateService.obtainDelegateName("accountId", delegateId, true)).isEqualTo(delegateId);

    String accountId = generateUuid();
    delegateService.add(delegateBuilder.accountId(accountId).build());
    assertThat(delegateService.obtainDelegateName(accountId, delegateId, true)).isEqualTo(delegateId);

    delegateService.add(delegateBuilder.hostName("hostName").build());
    assertThat(delegateService.obtainDelegateName(accountId, delegateId, true)).isEqualTo("hostName");

    delegateService.add(delegateBuilder.delegateName("delegateName").build());
    assertThat(delegateService.obtainDelegateName(accountId, delegateId, true)).isEqualTo("delegateName");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldAcquireDelegateTaskWhitelistedDelegateAndFFisOFF() {
    Delegate delegate = createDelegateBuilder().build();
    doReturn(delegate).when(spydelegateService).get(ACCOUNT_ID, delegate.getUuid(), false);
    doReturn(false).when(featureFlagService).isEnabled(eq(REVALIDATE_WHITELISTED_DELEGATE), anyString());

    DelegateTask delegateTask = getDelegateTask();
    doReturn(delegateTask).when(spydelegateService).getUnassignedDelegateTask(ACCOUNT_ID, "XYZ", delegate.getUuid());

    doReturn(getDelegateTaskPackage())
        .when(spydelegateService)
        .assignTask(anyString(), anyString(), any(DelegateTask.class));

    when(assignDelegateService.canAssign(any(), anyString(), any())).thenReturn(true);
    when(assignDelegateService.isWhitelisted(any(), anyString())).thenReturn(true);
    when(assignDelegateService.shouldValidate(any(), anyString())).thenReturn(false);
    BatchDelegateSelectionLog batch = BatchDelegateSelectionLog.builder().build();
    when(delegateSelectionLogsService.createBatch(delegateTask)).thenReturn(batch);

    spydelegateService.acquireDelegateTask(ACCOUNT_ID, delegate.getUuid(), "XYZ");

    verify(spydelegateService, times(1)).assignTask(anyString(), anyString(), any(DelegateTask.class));
    verify(delegateSelectionLogsService).save(batch);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldstartTaskValidationForWhitelistedDelegateAndFFisOn() {
    Delegate delegate = createDelegateBuilder().build();
    doReturn(delegate).when(spydelegateService).get(ACCOUNT_ID, delegate.getUuid(), false);
    doReturn(true).when(featureFlagService).isEnabled(eq(REVALIDATE_WHITELISTED_DELEGATE), anyString());

    doReturn(getDelegateTask())
        .when(spydelegateService)
        .getUnassignedDelegateTask(ACCOUNT_ID, "XYZ", delegate.getUuid());

    doNothing().when(spydelegateService).setValidationStarted(anyString(), any(DelegateTask.class));

    when(assignDelegateService.canAssign(any(), anyString(), any())).thenReturn(true);
    when(assignDelegateService.isWhitelisted(any(), anyString())).thenReturn(true);
    when(assignDelegateService.shouldValidate(any(), anyString())).thenReturn(false);

    spydelegateService.acquireDelegateTask(ACCOUNT_ID, delegate.getUuid(), "XYZ");

    verify(spydelegateService, times(1)).setValidationStarted(anyString(), any(DelegateTask.class));
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldstartTaskValidationNotWhitelistedAndFFisOff() {
    Delegate delegate = createDelegateBuilder().build();
    doReturn(delegate).when(spydelegateService).get(ACCOUNT_ID, delegate.getUuid(), false);
    doReturn(true).when(featureFlagService).isEnabled(eq(REVALIDATE_WHITELISTED_DELEGATE), anyString());

    doReturn(getDelegateTask())
        .when(spydelegateService)
        .getUnassignedDelegateTask(ACCOUNT_ID, "XYZ", delegate.getUuid());

    doNothing().when(spydelegateService).setValidationStarted(anyString(), any(DelegateTask.class));

    when(assignDelegateService.canAssign(any(), anyString(), any())).thenReturn(true);
    when(assignDelegateService.isWhitelisted(any(), anyString())).thenReturn(false);
    when(assignDelegateService.shouldValidate(any(), anyString())).thenReturn(true);

    spydelegateService.acquireDelegateTask(ACCOUNT_ID, delegate.getUuid(), "XYZ");

    verify(spydelegateService, times(1)).setValidationStarted(anyString(), any(DelegateTask.class));
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldNotAcquireDelegateTaskIfTaskIsNull() {
    Delegate delegate = createDelegateBuilder().build();
    doReturn(delegate).when(spydelegateService).get(ACCOUNT_ID, delegate.getUuid(), false);
    doReturn(null).when(spydelegateService).getUnassignedDelegateTask(ACCOUNT_ID, "XYZ", delegate.getUuid());
    assertThat(spydelegateService.acquireDelegateTask(ACCOUNT_ID, delegate.getUuid(), "XYZ")).isNull();
  }

  private DelegateTaskPackage getDelegateTaskPackage() {
    DelegateTask delegateTask = getDelegateTask();
    return DelegateTaskPackage.builder().delegateTaskId(delegateTask.getUuid()).delegateTask(delegateTask).build();
  }

  private DelegateTask getDelegateTask() {
    return DelegateTask.builder()
        .uuid(generateUuid())
        .accountId(ACCOUNT_ID)
        .waitId(generateUuid())
        .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, APP_ID)
        .version(VERSION)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .tags(new ArrayList<>())
        .build();
  }
}
