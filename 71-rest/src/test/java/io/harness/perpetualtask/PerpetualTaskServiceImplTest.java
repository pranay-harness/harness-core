package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class PerpetualTaskServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private PerpetualTaskServiceImpl perpetualTaskService;

  private final String ACCOUNT_ID = "test-account-id";
  private final String REGION = "region";
  private final String DELEGATE_ID = "test-delegate-id";
  private final String SETTING_ID = "settingId";
  private final String CLUSTER_NAME = "clusterName";
  private final long HEARTBEAT_MILLIS = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();
  private final long OLD_HEARTBEAT_MILLIS = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();

  @Before
  public void setup() {}

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

  public PerpetualTaskClientContext clientContext() {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put(REGION, REGION);
    clientParamMap.put(SETTING_ID, SETTING_ID);
    clientParamMap.put(CLUSTER_NAME, CLUSTER_NAME);
    return new PerpetualTaskClientContext(clientParamMap);
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
