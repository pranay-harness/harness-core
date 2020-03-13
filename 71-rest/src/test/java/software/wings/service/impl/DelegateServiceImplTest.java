package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.rule.Owner;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateBuilder;
import software.wings.beans.Delegate.Status;
import software.wings.beans.TaskType;
import software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DelegateServiceImplTest extends WingsBaseTest {
  private static final String VERSION = "1.0.0";
  @Mock private Broadcaster broadcaster;
  @Mock private BroadcasterFactory broadcasterFactory;
  @Mock private DelegateTaskBroadcastHelper broadcastHelper;
  @InjectMocks @Inject private DelegateServiceImpl delegateService;

  @Before
  public void setUp() {
    when(broadcasterFactory.lookup(anyString(), anyBoolean())).thenReturn(broadcaster);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldExecuteTask() {
    Delegate delegate = Delegate.builder()
                            .accountId(ACCOUNT_ID)
                            .ip("127.0.0.1")
                            .hostName("localhost")
                            .version(VERSION)
                            .status(Status.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    wingsPersistence.save(delegate);
    DelegateTask delegateTask = getDelegateTask();
    Thread thread = new Thread(() -> {
      await().atMost(5L, TimeUnit.SECONDS).until(() -> isNotEmpty(delegateService.syncTaskWaitMap));
      DelegateTask task =
          wingsPersistence.createQuery(DelegateTask.class).filter("accountId", delegateTask.getAccountId()).get();
      delegateService.processDelegateResponse(task.getAccountId(), delegate.getUuid(), task.getUuid(),
          DelegateTaskResponse.builder()
              .accountId(task.getAccountId())
              .response(HttpStateExecutionResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build())
              .responseCode(ResponseCode.OK)
              .build());
      new Thread(delegateService).start();
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
    delegateTask.setAsync(false);
    delegateService.saveDelegateTask(delegateTask);
    assertThat(delegateTask.getBroadcastCount()).isEqualTo(0);
    verify(broadcastHelper, times(0)).rebroadcastDelegateTask(any());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void shouldSaveDelegateTaskWithPreAssignedDelegateId_Async() {
    DelegateTask delegateTask = getDelegateTask();
    delegateTask.setAsync(true);
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

  private DelegateTask getDelegateTask() {
    return DelegateTask.builder()
        .async(false)
        .accountId(ACCOUNT_ID)
        .waitId(generateUuid())
        .appId(APP_ID)
        .version(VERSION)
        .data(TaskData.builder()
                  .taskType(TaskType.HTTP.name())
                  .parameters(new Object[] {HttpTaskParameters.builder().url("https://www.google.com").build()})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .build())
        .tags(new ArrayList<>())
        .build();
  }
}
