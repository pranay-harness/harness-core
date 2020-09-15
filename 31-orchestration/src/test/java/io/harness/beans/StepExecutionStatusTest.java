package io.harness.beans;

import static io.harness.rule.OwnerRule.POOJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationModuleListProvider;
import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.runners.GuiceRunner;
import io.harness.runners.ModuleProvider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(GuiceRunner.class)
@ModuleProvider(OrchestrationModuleListProvider.class)
public class StepExecutionStatusTest extends OrchestrationTest {
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void getStatusCategory() {
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.SUCCESS)).isEqualTo(ExecutionStatusCategory.SUCCEEDED);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.SKIPPED)).isEqualTo(ExecutionStatusCategory.SUCCEEDED);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.RUNNING)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.FAILED)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.ERROR)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.EXPIRED)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.REJECTED)).isEqualTo(ExecutionStatusCategory.ERROR);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.PAUSED)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.PAUSING)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.QUEUED)).isEqualTo(ExecutionStatusCategory.ACTIVE);
    assertThat(ExecutionStatus.getStatusCategory(ExecutionStatus.NEW)).isEqualTo(ExecutionStatusCategory.ACTIVE);
  }
}