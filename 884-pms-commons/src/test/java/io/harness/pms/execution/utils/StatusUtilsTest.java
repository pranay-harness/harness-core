package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.Status;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class StatusUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testNodeAllowedSatusInCaseOfTimeout() {
    EnumSet<Status> statuses = StatusUtils.nodeAllowedStartSet(Status.DISCONTINUING);
    assertThat(statuses.contains(Status.EXPIRED)).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusSucceeded() {
    List<Status> statuses = Arrays.asList(
        Status.SUCCEEDED, Status.SKIPPED, Status.SUCCEEDED, Status.SUSPENDED, Status.SKIPPED, Status.SKIPPED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusIgnoreFailed() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED,
        Status.SUSPENDED, Status.SKIPPED, Status.SKIPPED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.IGNORE_FAILED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusAborted() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.ABORTED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.ABORTED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusFailed() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.FAILED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.FAILED);

    statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.FAILED, Status.SUSPENDED,
        Status.RUNNING, Status.INTERVENTION_WAITING);
    status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusErrored() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.ERRORED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.ERRORED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusExpired() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.EXPIRED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.EXPIRED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusRunningNotPaused() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.PAUSED,
        Status.SUSPENDED, Status.RUNNING, Status.RUNNING);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.RUNNING);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testCalculateStatusPaused() {
    List<Status> statuses =
        Arrays.asList(Status.SUCCEEDED, Status.SUCCEEDED, Status.IGNORE_FAILED, Status.PAUSED, Status.SUSPENDED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.PAUSED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusQueued() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.QUEUED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusInterventionWaiting() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED,
        Status.SUSPENDED, Status.RUNNING, Status.TASK_WAITING, Status.INTERVENTION_WAITING);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.INTERVENTION_WAITING);

    // If a node is queued, and other is in waiting, so final status should be waiting.
    statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED, Status.SUSPENDED,
        Status.RUNNING, Status.QUEUED, Status.INTERVENTION_WAITING);
    status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.INTERVENTION_WAITING);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusApprovalWaiting() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED,
        Status.SUSPENDED, Status.RUNNING, Status.TASK_WAITING, Status.APPROVAL_WAITING);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.APPROVAL_WAITING);

    // If a node is queued, and other is in waiting, so final status should be waiting.
    statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED, Status.SUSPENDED,
        Status.RUNNING, Status.QUEUED, Status.APPROVAL_WAITING);
    status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.APPROVAL_WAITING);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateStatusRunning() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED,
        Status.SUSPENDED, Status.RUNNING, Status.TASK_WAITING, Status.SUCCEEDED);

    Status status = StatusUtils.calculateStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.RUNNING);
  }
}