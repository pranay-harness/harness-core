package software.wings.service.impl;

import static io.harness.beans.FeatureName.REVALIDATE_WHITELISTED_DELEGATE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.UTSAV;
import static io.harness.rule.OwnerRule.VUK;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateStringResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.ff.FeatureFlagService;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;
import io.harness.rule.Owner;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.serializer.KryoSerializer;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateCallbackService;
import io.harness.tasks.Cd1SetupFields;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.CEDelegateStatus;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateBuilder;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.beans.TaskType;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class DelegateServiceImplTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  @Mock private UsageLimitedFeature delegatesFeature;
  @Mock private Broadcaster broadcaster;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private DelegateTaskBroadcastHelper broadcastHelper;
  @Mock private DelegateCallbackRegistry delegateCallbackRegistry;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private AccountService accountService;
  @Mock private Account account;
  @InjectMocks @Inject private DelegateServiceImpl delegateService;
  @InjectMocks @Inject private DelegateSyncServiceImpl delegateSyncService;
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateSelectionLogsService delegateSelectionLogsService;
  @Mock private SettingsService settingsService;

  @InjectMocks @Spy private DelegateServiceImpl spydelegateService;
  @Inject private KryoSerializer kryoSerializer;

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
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testRetrieveLogStreamingAccountToken() {
    String accountId = generateUuid();

    try {
      delegateService.retrieveLogStreamingAccountToken(accountId);
    } catch (Exception ex) {
      fail("Unexpected failure while retrieving streaming log token");
    }
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
        .thenReturn(Arrays.asList(delegate.getUuid()));
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
    DelegateResponseData responseData = delegateService.executeTask(delegateTask);
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
    delegateService.saveDelegateTask(delegateTask, DelegateTask.Status.QUEUED);
    assertThat(delegateTask.getBroadcastCount()).isEqualTo(0);
    verify(broadcastHelper, times(0)).rebroadcastDelegateTask(any());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithPreAssignedDelegateId_Async() {
    DelegateTask delegateTask = getDelegateTask();
    delegateTask.getData().setAsync(true);
    delegateService.saveDelegateTask(delegateTask, DelegateTask.Status.QUEUED);
    assertThat(delegateTask.getBroadcastCount()).isEqualTo(0);
    verify(broadcastHelper, times(0)).rebroadcastDelegateTask(any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithPreAssignedDelegateIdSetToMustExecuteOn() {
    String delegateId = generateUuid();
    String taskId = generateUuid();

    DelegateTask delegateTask = getDelegateTask();
    delegateTask.getData().setAsync(false);
    delegateTask.setMustExecuteOnDelegateId(delegateId);
    delegateTask.setUuid(taskId);

    delegateService.saveDelegateTask(delegateTask, DelegateTask.Status.QUEUED);
    assertThat(wingsPersistence.get(DelegateTask.class, taskId).getPreAssignedDelegateId()).isEqualTo(delegateId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithoutPreAssignedDelegateIdSetToMustExecuteOn() {
    String delegateId = generateUuid();
    String taskId = generateUuid();

    DelegateTask delegateTask = getDelegateTask();
    delegateTask.getData().setAsync(false);
    delegateTask.setUuid(taskId);

    delegateService.saveDelegateTask(delegateTask, DelegateTask.Status.QUEUED);
    assertThat(wingsPersistence.get(DelegateTask.class, taskId).getPreAssignedDelegateId()).isNotEqualTo(delegateId);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldObtainDelegateName() {
    String accountId = generateUuid();
    when(delegatesFeature.getMaxUsageAllowedForAccount(anyString())).thenReturn(Integer.MAX_VALUE);

    String delegateId = generateUuid();
    assertThat(delegateService.obtainDelegateName(null, delegateId, true)).isEqualTo("");
    assertThat(delegateService.obtainDelegateName("accountId", delegateId, true)).isEqualTo(delegateId);

    DelegateBuilder delegateBuilder = Delegate.builder();

    delegateService.add(delegateBuilder.uuid(delegateId).build());
    assertThat(delegateService.obtainDelegateName("accountId", delegateId, true)).isEqualTo(delegateId);

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
  public void shouldStartTaskValidationForWhitelistedDelegateAndFFisOn() {
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
  public void shouldStartTaskValidationNotWhitelistedAndFFisOff() {
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
    return DelegateTaskPackage.builder().delegateTaskId(delegateTask.getUuid()).data(delegateTask.getData()).build();
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

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testValidateThatDelegateNameIsUnique() {
    String delegateName = "delegateName";
    Delegate delegate = createDelegateBuilder().build();
    delegate.setDelegateName(delegateName);
    wingsPersistence.save(delegate);
    boolean validationResult = delegateService.validateThatDelegateNameIsUnique(ACCOUNT_ID, delegateName);
    assertThat(validationResult).isFalse();

    // If the delegate doesn't exists
    boolean checkingValidationForRandomName =
        delegateService.validateThatDelegateNameIsUnique(ACCOUNT_ID, String.valueOf(System.currentTimeMillis()));
    assertThat(checkingValidationForRandomName).isTrue();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidateCEDelegate() {
    long lastHeartBeart = DateTime.now().getMillis();
    Delegate delegate =
        createDelegateBuilder().accountId(ACCOUNT_ID).delegateName(DELEGATE_NAME).uuid(generateUuid()).build();
    wingsPersistence.save(delegate);

    DelegateConnection delegateConnection = DelegateConnection.builder()
                                                .accountId(ACCOUNT_ID)
                                                .delegateId(delegate.getUuid())
                                                .lastHeartbeat(lastHeartBeart)
                                                .disconnected(false)
                                                .build();
    wingsPersistence.save(delegateConnection);

    when(settingsService.validateCEDelegateSetting(any(), any()))
        .thenReturn(CEK8sDelegatePrerequisite.builder().build());

    CEDelegateStatus ceDelegateStatus = delegateService.validateCEDelegate(ACCOUNT_ID, DELEGATE_NAME);

    verify(settingsService, times(1)).validateCEDelegateSetting(eq(ACCOUNT_ID), eq(DELEGATE_NAME));

    assertThat(ceDelegateStatus).isNotNull();
    assertThat(ceDelegateStatus.getFound()).isTrue();
    assertThat(ceDelegateStatus.getUuid()).isEqualTo(delegate.getUuid());
    assertThat(ceDelegateStatus.getMetricsServerCheck()).isNull();
    assertThat(ceDelegateStatus.getPermissionRuleList()).isNull();
    assertThat(ceDelegateStatus.getLastHeartBeat()).isGreaterThanOrEqualTo(lastHeartBeart);
    assertThat(ceDelegateStatus.getDelegateName()).isEqualTo(DELEGATE_NAME);

    assertThat(ceDelegateStatus.getConnections()).hasSizeGreaterThan(0);
    assertThat(ceDelegateStatus.getConnections().get(0).getLastHeartbeat()).isEqualTo(lastHeartBeart);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleDriverResponseWithoutArguments() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .uuid(generateUuid())
                                    .accountId(generateUuid())
                                    .driverId(generateUuid())
                                    .data(TaskData.builder().async(false).build())
                                    .build();

    DelegateTaskResponse delegateTaskResponse =
        DelegateTaskResponse.builder().response(DelegateStringResponseData.builder().data("OK").build()).build();

    delegateService.handleDriverResponse(null, delegateTaskResponse);
    verify(delegateCallbackRegistry, never()).obtainDelegateCallbackService(any());

    delegateService.handleDriverResponse(delegateTask, null);
    verify(delegateCallbackRegistry, never()).obtainDelegateCallbackService(any());

    delegateService.handleDriverResponse(null, null);
    verify(delegateCallbackRegistry, never()).obtainDelegateCallbackService(any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleDriverResponseWithNonExistingDriver() {
    DelegateTask delegateTask = mock(DelegateTask.class);
    DelegateTaskResponse delegateTaskResponse = mock(DelegateTaskResponse.class);

    when(delegateCallbackRegistry.obtainDelegateCallbackService(delegateTask.getDriverId())).thenReturn(null);

    delegateService.handleDriverResponse(delegateTask, delegateTaskResponse);

    verify(delegateTask, never()).getUuid();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleDriverSyncResponse() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .uuid(generateUuid())
                                    .accountId(generateUuid())
                                    .driverId(generateUuid())
                                    .data(TaskData.builder().async(false).build())
                                    .build();

    DelegateTaskResponse delegateTaskResponse =
        DelegateTaskResponse.builder().response(DelegateStringResponseData.builder().data("OK").build()).build();

    DelegateCallbackService delegateCallbackService = mock(DelegateCallbackService.class);
    when(delegateCallbackRegistry.obtainDelegateCallbackService(delegateTask.getDriverId()))
        .thenReturn(delegateCallbackService);
    byte[] responseData = kryoSerializer.asDeflatedBytes(delegateTaskResponse.getResponse());

    delegateService.handleDriverResponse(delegateTask, delegateTaskResponse);

    verify(delegateCallbackService).publishSyncTaskResponse(delegateTask.getUuid(), responseData);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleDriverAsyncResponse() {
    DelegateTask delegateTask = DelegateTask.builder()
                                    .uuid(generateUuid())
                                    .accountId(generateUuid())
                                    .driverId(generateUuid())
                                    .data(TaskData.builder().async(true).build())
                                    .build();

    DelegateTaskResponse delegateTaskResponse =
        DelegateTaskResponse.builder().response(DelegateStringResponseData.builder().data("OK").build()).build();

    DelegateCallbackService delegateCallbackService = mock(DelegateCallbackService.class);
    when(delegateCallbackRegistry.obtainDelegateCallbackService(delegateTask.getDriverId()))
        .thenReturn(delegateCallbackService);
    byte[] responseData = kryoSerializer.asDeflatedBytes(delegateTaskResponse.getResponse());

    delegateService.handleDriverResponse(delegateTask, delegateTaskResponse);

    verify(delegateCallbackService).publishAsyncTaskResponse(delegateTask.getUuid(), responseData);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testDelegateDisconnected() {
    DelegateObserver delegateObserver = mock(DelegateObserver.class);
    delegateService.getSubject().register(delegateObserver);

    String delegateId = generateUuid();
    String delegateConnectionId = generateUuid();
    String accountId = generateUuid();

    DelegateConnection delegateConnection = DelegateConnection.builder()
                                                .accountId(accountId)
                                                .uuid(delegateConnectionId)
                                                .delegateId(delegateId)
                                                .disconnected(false)
                                                .build();

    wingsPersistence.save(delegateConnection);

    delegateService.delegateDisconnected(accountId, delegateId, delegateConnectionId);

    DelegateConnection retrievedDelegateConnection =
        wingsPersistence.createQuery(DelegateConnection.class)
            .filter(DelegateConnectionKeys.uuid, delegateConnection.getUuid())
            .get();

    assertThat(retrievedDelegateConnection).isNotNull();
    assertThat(retrievedDelegateConnection.getDelegateId()).isEqualTo(delegateId);
    assertThat(retrievedDelegateConnection.getAccountId()).isEqualTo(accountId);
    assertThat(retrievedDelegateConnection.isDisconnected()).isTrue();

    verify(delegateObserver).onDisconnected(accountId, delegateId);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSelectDelegateToRetain() {
    Delegate delegate1 = Delegate.builder()
                             .accountId(ACCOUNT_ID)
                             .ip("127.0.0.1")
                             .hostName("localhost")
                             .version(VERSION)
                             .lastHeartBeat(System.currentTimeMillis())
                             .build();

    Delegate delegate2 = Delegate.builder()
                             .accountId(ACCOUNT_ID)
                             .ip("127.0.0.1")
                             .hostName("localhost")
                             .version(VERSION)
                             .lastHeartBeat(0)
                             .build();

    wingsPersistence.save(delegate1);
    wingsPersistence.save(delegate2);

    when(delegatesFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(1);

    delegateService.deleteAllDelegatesExceptOne(ACCOUNT_ID, 1);

    Delegate delegateToRetain1 =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegate1.getUuid()).get();
    Delegate delegateToRetain2 =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegate2.getUuid()).get();

    assertThat(delegateToRetain1).isNotNull();
    assertThat(delegateToRetain2).isNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldSelectDelegateToRetainSendEmailAboutDelegatesOverUsage() {
    Delegate delegate1 = Delegate.builder()
                             .accountId(ACCOUNT_ID)
                             .ip("127.0.0.1")
                             .hostName("localhost")
                             .version(VERSION)
                             .lastHeartBeat(System.currentTimeMillis())
                             .build();

    Delegate delegate2 = Delegate.builder()
                             .accountId(ACCOUNT_ID)
                             .ip("127.0.0.1")
                             .hostName("localhost")
                             .version(VERSION)
                             .lastHeartBeat(0)
                             .build();

    wingsPersistence.save(delegate1);
    wingsPersistence.save(delegate2);

    EmailData emailData =
        EmailData.builder()
            .hasHtml(false)
            .body(
                "Account is using more than [0] delegates. Account Id : [ACCOUNT_ID], Company Name : [NCR], Account Name : [testAccountName], Delegate Count : [1]")
            .subject("Found account with more than 0 delegates")
            .to(Lists.newArrayList("support@harness.io"))
            .build();

    when(accountService.get(ACCOUNT_ID)).thenReturn(account);
    when(account.getCompanyName()).thenReturn("NCR");
    when(account.getAccountName()).thenReturn("testAccountName");
    when(delegatesFeature.getMaxUsageAllowedForAccount(ACCOUNT_ID)).thenReturn(0);

    when(emailNotificationService.send(emailData)).thenReturn(true);

    delegateService.deleteAllDelegatesExceptOne(ACCOUNT_ID, 1);

    Delegate delegateToRetain1 =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegate1.getUuid()).get();
    Delegate delegateToRetain2 =
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegate2.getUuid()).get();

    assertThat(delegateToRetain1).isNotNull();
    assertThat(delegateToRetain2).isNull();

    verify(emailNotificationService, atLeastOnce()).send(emailData);
  }
}
