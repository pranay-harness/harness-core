package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
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
  private final String PERPETUAL_TASK_ID = "perpetual-task-id";
  private final String REGION = "region";
  private final String SETTING_ID = "settingId";
  private final String CLUSTER_NAME = "clusterName";
  private final long HEARTBEAT_MILLIS = Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli();
  private final long OLD_HEARTBEAT_MILLIS = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();

  @Before
  public void setup() {}

  @Test
  @Owner(emails = HITESH)
  @Category(UnitTests.class)
  public void testCreateTask() {
    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext(), perpetualTaskSchedule(), false);
    assertThat(taskId).isNotNull();
    String taskIdDuplicate = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext(), perpetualTaskSchedule(), false);
    assertThat(taskIdDuplicate).isEqualTo(taskId);
  }

  @Test
  @Owner(emails = HITESH)
  @Category(UnitTests.class)
  public void testUpdateHeartbeat() {
    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext(), perpetualTaskSchedule(), false);
    boolean updateHeartbeat = perpetualTaskService.updateHeartbeat(taskId, HEARTBEAT_MILLIS);
    assertThat(updateHeartbeat).isTrue();
  }

  @Test
  @Owner(emails = HITESH)
  @Category(UnitTests.class)
  public void shouldNotUpdateHeartbeat() {
    String taskId = perpetualTaskService.createTask(
        PerpetualTaskType.ECS_CLUSTER, ACCOUNT_ID, clientContext(), perpetualTaskSchedule(), false);
    boolean updateHeartbeat = perpetualTaskService.updateHeartbeat(taskId, OLD_HEARTBEAT_MILLIS);
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
}
