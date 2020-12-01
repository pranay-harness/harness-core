package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.pms.execution.Status;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitCloneStepTest extends CIExecutionTest {
  @Inject private GitCloneStep gitCloneStep;

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldExecuteGitCloneTask() {
    StepResponse stepResponse = gitCloneStep.executeSync(null, GitCloneStepInfo.builder().build(), null, null);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }
}
