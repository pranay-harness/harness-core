package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HITESH;
import static io.harness.rule.OwnerRule.VUK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.AbortTaskRequest;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPerpetualTaskParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PerpetualTaskServiceImplTest extends WingsBaseTest {
  @Mock private PerpetualTaskServiceClient client;

  @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;

  @InjectMocks @Inject private PerpetualTaskServiceImpl perpetualTaskService;

  private final String ACCOUNT_ID = "test-account-id";
  private final String REGION = "region";
  private final String DELEGATE_ID = "test-delegate-id";
  private final String SETTING_ID = "settingId";
  private final String CLUSTER_NAME = "clusterName";
  private final long HEARTBEAT_MILLIS = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();
  private final long OLD_HEARTBEAT_MILLIS = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
  private Set<Pair<String, String>> testBroadcastAggregateSet = new ConcurrentHashSet<>();

  @Before
  public void setup() throws IllegalAccessException {
    PerpetualTaskServiceClientRegistry clientRegistry = new PerpetualTaskServiceClientRegistry();
    clientRegistry.registerClient(PerpetualTaskType.ECS_CLUSTER, new EcsPerpetualTaskServiceClient());
    clientRegistry.registerClient(PerpetualTaskType.K8S_WATCH, client);
    FieldUtils.writeField(perpetualTaskService, "broadcastAggregateSet", testBroadcastAggregateSet, true);
    FieldUtils.writeField(perpetualTaskService, "clientRegistry", clientRegistry, true);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testCreateTask() {
    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);

    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext, perpetualTaskSchedule(), false);
    assertThat(taskId).isNotNull();
    String taskIdDuplicate = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext, perpetualTaskSchedule(), false);
    assertThat(taskIdDuplicate).isEqualTo(taskId);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testResetTaskWithoutTaskParams() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateId = UUIDGenerator.generateUuid();
    String taskId = UUIDGenerator.generateUuid();

    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);
    perpetualTaskRecord.setAccountId(accountId);
    perpetualTaskRecord.setDelegateId(delegateId);
    perpetualTaskRecord.setUuid(taskId);
    perpetualTaskRecord.setState(PerpetualTaskState.TASK_ASSIGNED.name());
    perpetualTaskRecordDao.save(perpetualTaskRecord);

    boolean resetTask = perpetualTaskService.resetTask(accountId, taskId, null);

    PerpetualTaskRecord record = perpetualTaskService.getTaskRecord(taskId);

    assertThat(record).isNotNull();
    assertThat(record.getState()).isEqualTo(PerpetualTaskState.TASK_UNASSIGNED.name());
    assertThat(resetTask).isTrue();
    assertThat(record.getClientContext().getClientParams()).isEqualTo(clientContext.getClientParams());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testResetTaskWithTaskParams() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateId = UUIDGenerator.generateUuid();
    String taskId = UUIDGenerator.generateUuid();

    PerpetualTaskClientContext clientContext = clientContextWithTaskParams();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);
    perpetualTaskRecord.setAccountId(accountId);
    perpetualTaskRecord.setDelegateId(delegateId);
    perpetualTaskRecord.setUuid(taskId);
    perpetualTaskRecord.setState(PerpetualTaskState.TASK_ASSIGNED.name());
    perpetualTaskRecordDao.save(perpetualTaskRecord);

    PerpetualTaskExecutionBundle taskExecutionBundle =
        PerpetualTaskExecutionBundle.newBuilder()
            .setTaskParams(Any.pack(AwsSshInstanceSyncPerpetualTaskParams.getDefaultInstance()))
            .build();

    boolean resetTask = perpetualTaskService.resetTask(accountId, taskId, taskExecutionBundle);

    PerpetualTaskRecord record = perpetualTaskService.getTaskRecord(taskId);

    assertThat(record).isNotNull();
    assertThat(record.getState()).isEqualTo(PerpetualTaskState.TASK_UNASSIGNED.name());
    assertThat(resetTask).isTrue();
    assertThat(record.getClientContext().getExecutionBundle()).isEqualTo(taskExecutionBundle.toByteArray());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testResetTaskWithClientContextNull() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateId = UUIDGenerator.generateUuid();
    String taskId = UUIDGenerator.generateUuid();

    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setAccountId(accountId);
    perpetualTaskRecord.setDelegateId(delegateId);
    perpetualTaskRecord.setUuid(taskId);
    perpetualTaskRecord.setState(PerpetualTaskState.TASK_ASSIGNED.name());
    perpetualTaskRecordDao.save(perpetualTaskRecord);

    boolean resetTask = perpetualTaskService.resetTask(accountId, taskId, null);

    PerpetualTaskRecord record = perpetualTaskService.getTaskRecord(taskId);

    assertThat(record).isNotNull();
    assertThat(record.getState()).isEqualTo(PerpetualTaskState.TASK_UNASSIGNED.name());
    assertThat(resetTask).isTrue();
    assertThat(record.getClientContext()).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testDeleteTask() {
    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);

    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext, perpetualTaskSchedule(), false);

    boolean deletedTask = perpetualTaskService.deleteTask(ACCOUNT_ID, taskId);

    assertThat(taskId).isNotNull();
    assertThat(deletedTask).isTrue();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testSetTaskState() {
    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);

    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext, perpetualTaskSchedule(), false);

    perpetualTaskService.setTaskState(ACCOUNT_ID, taskId);

    assertThat(taskId).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetTaskRecord() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateId = UUIDGenerator.generateUuid();
    String taskId = UUIDGenerator.generateUuid();

    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);
    perpetualTaskRecord.setAccountId(accountId);
    perpetualTaskRecord.setDelegateId(delegateId);
    perpetualTaskRecord.setUuid(taskId);
    perpetualTaskRecordDao.save(perpetualTaskRecord);

    PerpetualTaskRecord record = perpetualTaskService.getTaskRecord(taskId);

    assertThat(record).isNotNull();
    assertThat(record.getAccountId()).isEqualTo(accountId);
    assertThat(record.getDelegateId()).isEqualTo(delegateId);
    assertThat(record.getClientContext()).isEqualTo(clientContext);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testPerpetualTaskType() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateId = UUIDGenerator.generateUuid();
    String taskId = UUIDGenerator.generateUuid();
    String perpetualTaskType = UUIDGenerator.generateUuid();

    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);
    perpetualTaskRecord.setAccountId(accountId);
    perpetualTaskRecord.setDelegateId(delegateId);
    perpetualTaskRecord.setUuid(taskId);
    perpetualTaskRecord.setPerpetualTaskType(perpetualTaskType);
    perpetualTaskRecordDao.save(perpetualTaskRecord);

    String perpetualTaskTypeResult = perpetualTaskService.getPerpetualTaskType(taskId);

    assertThat(perpetualTaskTypeResult).isNotNull();
    assertThat(perpetualTaskTypeResult).isEqualTo(perpetualTaskType);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testUpdateHeartbeat() {
    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext(), perpetualTaskSchedule(), false);
    boolean updateHeartbeat = perpetualTaskService.triggerCallback(taskId, HEARTBEAT_MILLIS, perpetualTaskResponse());
    assertThat(updateHeartbeat).isTrue();
  }

  private PerpetualTaskResponse perpetualTaskResponse() {
    return PerpetualTaskResponse.builder().perpetualTaskState(PerpetualTaskState.TASK_RUN_SUCCEEDED).build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldNotUpdateHeartbeat() {
    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext(), perpetualTaskSchedule(), false);
    perpetualTaskService.triggerCallback(taskId, HEARTBEAT_MILLIS, perpetualTaskResponse());
    boolean updateHeartbeat =
        perpetualTaskService.triggerCallback(taskId, OLD_HEARTBEAT_MILLIS, perpetualTaskResponse());
    assertThat(updateHeartbeat).isFalse();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testAppointDelegate() {
    String accountId = UUIDGenerator.generateUuid();
    String delegateId = UUIDGenerator.generateUuid();
    String taskId = UUIDGenerator.generateUuid();

    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecord();
    perpetualTaskRecord.setClientContext(clientContext);
    perpetualTaskRecord.setAccountId(accountId);
    perpetualTaskRecord.setDelegateId(null);
    perpetualTaskRecord.setUuid(taskId);
    perpetualTaskRecordDao.save(perpetualTaskRecord);

    perpetualTaskService.appointDelegate(accountId, taskId, delegateId, 1L);

    PerpetualTaskRecord record = perpetualTaskRecordDao.getTask(taskId);
    assertThat(record).isNotNull();
    assertThat(record.getDelegateId()).isEqualTo(delegateId);
    assertThat(record.getClientContext().getLastContextUpdated()).isEqualTo(1L);

    String delegateId2 = UUIDGenerator.generateUuid();

    perpetualTaskService.appointDelegate(accountId, taskId, delegateId, 1L);
    perpetualTaskService.appointDelegate(accountId, taskId, delegateId2, 1L);
    assertThat(testBroadcastAggregateSet).isNotNull();
    assertThat(testBroadcastAggregateSet).size().isEqualTo(2);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testPerpetualTaskContext() {
    PerpetualTaskClientContext clientContext = clientContext();
    PerpetualTaskRecord perpetualTaskRecord = PerpetualTaskRecord.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
                                                  .clientContext(clientContext())
                                                  .delegateId(DELEGATE_ID)
                                                  .intervalSeconds(1L)
                                                  .timeoutMillis(2L)
                                                  .build();
    perpetualTaskRecord.setClientContext(clientContext);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(perpetualTaskRecord.getIntervalSeconds()))
                                         .setTimeout(Durations.fromMillis(perpetualTaskRecord.getTimeoutMillis()))
                                         .build();

    Message perpetualTaskParams = AbortTaskRequest.newBuilder().build();
    when(client.getTaskParams(clientContext)).thenReturn(perpetualTaskParams);

    String taskId = perpetualTaskRecordDao.save(perpetualTaskRecord);

    PerpetualTaskExecutionContext perpetualTaskContext = perpetualTaskService.perpetualTaskContext(taskId);

    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);

    assertThat(task).isNotNull();
    assertThat(perpetualTaskContext).isNotNull();
    assertThat(task.getUuid()).isEqualTo(taskId);
    assertThat(perpetualTaskContext.getTaskSchedule()).isEqualTo(schedule);
  }

  public PerpetualTaskClientContext clientContext() {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(REGION, REGION);
    clientParamMap.put(SETTING_ID, SETTING_ID);
    clientParamMap.put(CLUSTER_NAME, CLUSTER_NAME);
    return PerpetualTaskClientContext.builder().clientParams(clientParamMap).build();
  }

  public PerpetualTaskClientContext clientContextWithTaskParams() {
    byte[] taskParameters = new byte[] {1, 2, 3, 4};
    return PerpetualTaskClientContext.builder().executionBundle(taskParameters).build();
  }

  public PerpetualTaskSchedule perpetualTaskSchedule() {
    return PerpetualTaskSchedule.newBuilder()
        .setInterval(Durations.fromSeconds(600))
        .setTimeout(Durations.fromMillis(180000))
        .build();
  }

  public PerpetualTaskRecord perpetualTaskRecord() {
    return PerpetualTaskRecord.builder()
        .accountId(ACCOUNT_ID)
        .perpetualTaskType(PerpetualTaskType.K8S_WATCH)
        .clientContext(clientContext())
        .delegateId(DELEGATE_ID)
        .build();
  }
}
