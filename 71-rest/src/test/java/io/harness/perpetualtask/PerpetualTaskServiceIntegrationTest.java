package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.category.element.IntegrationTests;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import io.harness.rule.Owner;
import lombok.val;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class PerpetualTaskServiceIntegrationTest extends BaseIntegrationTest {
  private final long HEARTBEAT_MILLIS = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli();

  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;

  private final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID_" + this.getClass().getSimpleName();

  private final PerpetualTaskType DEFAULT_TASK_TYPE = PerpetualTaskType.ECS_CLUSTER;
  private final PerpetualTaskSchedule TASK_SCHEDULE = PerpetualTaskSchedule.newBuilder()
                                                          .setInterval(Durations.fromSeconds(1))
                                                          .setTimeout(Durations.fromMillis(1000))
                                                          .build();

  private PerpetualTaskClientContext getPerpetualTaskClientContext() {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("region", "default-region");
    return new PerpetualTaskClientContext(clientParamMap);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(IntegrationTests.class)
  public void shouldCreatePerpetualTask() {
    String taskId = perpetualTaskService.createTask(
        DEFAULT_TASK_TYPE, TEST_ACCOUNT_ID, getPerpetualTaskClientContext(), TASK_SCHEDULE, false);
    assertThat(taskId).isNotNull();
    String duplicateTaskId = perpetualTaskService.createTask(
        DEFAULT_TASK_TYPE, TEST_ACCOUNT_ID, getPerpetualTaskClientContext(), TASK_SCHEDULE, false);
    assertThat(duplicateTaskId).isEqualTo(taskId);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(IntegrationTests.class)
  public void shouldCreateDuplicatePerpetualTask() {
    String taskId = perpetualTaskService.createTask(
        DEFAULT_TASK_TYPE, TEST_ACCOUNT_ID, getPerpetualTaskClientContext(), TASK_SCHEDULE, true);
    assertThat(taskId).isNotNull();
    String duplicateTaskId = perpetualTaskService.createTask(
        DEFAULT_TASK_TYPE, TEST_ACCOUNT_ID, getPerpetualTaskClientContext(), TASK_SCHEDULE, true);
    assertThat(duplicateTaskId).isNotEqualTo(taskId);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(IntegrationTests.class)
  public void shouldDeletePerpetualTask() {
    String taskId = perpetualTaskService.createTask(
        DEFAULT_TASK_TYPE, TEST_ACCOUNT_ID, getPerpetualTaskClientContext(), TASK_SCHEDULE, false);
    boolean deleteTask = perpetualTaskService.deleteTask(TEST_ACCOUNT_ID, taskId);
    assertThat(deleteTask).isTrue();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(IntegrationTests.class)
  public void shouldUpdateHeartbeat() {
    String taskId = perpetualTaskService.createTask(
        DEFAULT_TASK_TYPE, TEST_ACCOUNT_ID, getPerpetualTaskClientContext(), TASK_SCHEDULE, false);
    boolean heartbeatUpdated = perpetualTaskService.updateHeartbeat(taskId, HEARTBEAT_MILLIS);
    assertThat(heartbeatUpdated).isTrue();

    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    assertThat(task).isNotNull();
    assertThat(task.getLastHeartbeat()).isEqualTo(HEARTBEAT_MILLIS);
  }

  @After
  public void clearCollection() {
    val ds = wingsPersistence.getDatastore(PerpetualTaskRecord.class);
    ds.delete(ds.createQuery(PerpetualTaskRecord.class).filter(PerpetualTaskRecordKeys.accountId, TEST_ACCOUNT_ID));
  }
}
