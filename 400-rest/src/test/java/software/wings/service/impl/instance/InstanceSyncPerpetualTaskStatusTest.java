package software.wings.service.impl.instance;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.infrastructure.instance.InstanceSyncPerpetualTaskStatus;
import software.wings.beans.infrastructure.instance.InstanceSyncPerpetualTaskStatus.InstanceSyncPerpetualTaskStatusKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.instancesyncperpetualtaskstatus.InstanceSyncPerpetualTaskStatusService;

import com.google.inject.Inject;
import java.time.Duration;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class InstanceSyncPerpetualTaskStatusTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject InstanceSyncPerpetualTaskStatusService instanceSyncPerpetualTaskStatusService;

  public static final String PERPETUAL_TASK_ID = "perptualTaskId";
  public static final String FAILURE_REASON = "failureReason";
  public static final String FAILURE_REASON_2 = "failureReason2";

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testHandleSyncFailureFirstIteration() {
    boolean status =
        instanceSyncPerpetualTaskStatusService.handlePerpetualTaskFailure(PERPETUAL_TASK_ID, FAILURE_REASON);
    InstanceSyncPerpetualTaskStatus instanceSyncPerpetualTaskStatus = getRecord(PERPETUAL_TASK_ID);

    assertThat(!status);
    assertThat(PERPETUAL_TASK_ID.equals(instanceSyncPerpetualTaskStatus.getPerpetualTaskId()));
    assertThat(FAILURE_REASON.equals(instanceSyncPerpetualTaskStatus.getLastFailureReason()));
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testHandleSyncFailureThresholdIteration() {
    long currTime = System.currentTimeMillis();
    createRecord(currTime - Duration.ofDays(7).toMillis());
    InstanceSyncPerpetualTaskStatus instanceSyncPerpetualTaskStatus = getRecord(PERPETUAL_TASK_ID);
    assertThat(instanceSyncPerpetualTaskStatus).isNotNull();

    boolean status =
        instanceSyncPerpetualTaskStatusService.handlePerpetualTaskFailure(PERPETUAL_TASK_ID, FAILURE_REASON);
    instanceSyncPerpetualTaskStatus = getRecord(PERPETUAL_TASK_ID);
    assertThat(status);
    assertThat(instanceSyncPerpetualTaskStatus).isNull();
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testUpdatePerpetualTaskSuccess() {
    createRecord(System.currentTimeMillis());
    InstanceSyncPerpetualTaskStatus instanceSyncPerpetualTaskStatus = getRecord(PERPETUAL_TASK_ID);
    assertThat(instanceSyncPerpetualTaskStatus).isNotNull();

    instanceSyncPerpetualTaskStatusService.updatePerpetualTaskSuccess(PERPETUAL_TASK_ID);
    instanceSyncPerpetualTaskStatus = getRecord(PERPETUAL_TASK_ID);
    assertThat(instanceSyncPerpetualTaskStatus).isNull();
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testMultipleFailures() {
    boolean status =
        instanceSyncPerpetualTaskStatusService.handlePerpetualTaskFailure(PERPETUAL_TASK_ID, FAILURE_REASON);
    InstanceSyncPerpetualTaskStatus instanceSyncPerpetualTaskStatus = getRecord(PERPETUAL_TASK_ID);

    assertThat(!status);
    assertThat(PERPETUAL_TASK_ID.equals(instanceSyncPerpetualTaskStatus.getPerpetualTaskId()));
    assertThat(FAILURE_REASON.equals(instanceSyncPerpetualTaskStatus.getLastFailureReason()));

    status = instanceSyncPerpetualTaskStatusService.handlePerpetualTaskFailure(PERPETUAL_TASK_ID, FAILURE_REASON_2);
    instanceSyncPerpetualTaskStatus = getRecord(PERPETUAL_TASK_ID);

    assertThat(!status);
    assertThat(PERPETUAL_TASK_ID.equals(instanceSyncPerpetualTaskStatus.getPerpetualTaskId()));
    assertThat(FAILURE_REASON_2.equals(instanceSyncPerpetualTaskStatus.getLastFailureReason()));
  }

  private void createRecord(long firstFailureTimestamp) {
    wingsPersistence.insert(InstanceSyncPerpetualTaskStatus.builder()
                                .perpetualTaskId(PERPETUAL_TASK_ID)
                                .initialFailureAt(firstFailureTimestamp)
                                .lastFailureReason(FAILURE_REASON)
                                .build());
  }

  private InstanceSyncPerpetualTaskStatus getRecord(String perpetualTaskId) {
    return wingsPersistence.createQuery(InstanceSyncPerpetualTaskStatus.class)
        .filter(InstanceSyncPerpetualTaskStatusKeys.perpetualTaskId, perpetualTaskId)
        .get();
  }
}
