package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.Status;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StatusUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateEndStatusSucceeded() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.SUCCEEDED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateEndStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.SUCCEEDED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateEndStatusAborted() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.ABORTED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateEndStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.ABORTED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateEndStatusFailed() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.FAILED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateEndStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateEndStatusErrored() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.ERRORED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateEndStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.ERRORED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCalculateEndStatusExpired() {
    List<Status> statuses = Arrays.asList(Status.SUCCEEDED, Status.SKIPPED, Status.IGNORE_FAILED, Status.EXPIRED,
        Status.SUSPENDED, Status.RUNNING, Status.QUEUED);

    Status status = StatusUtils.calculateEndStatus(statuses, "PLAN_EXECUTION_ID");
    assertThat(status).isEqualTo(Status.EXPIRED);
  }
}