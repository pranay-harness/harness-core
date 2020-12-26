package io.harness.beans.steps.stepinfo;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CiBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepInfoCreationTest extends CiBeansTestBase {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testsBuilderDefaultValues() {
    GitCloneStepInfo gitCloneStepInfo = GitCloneStepInfo.builder().build();
    assertThat(gitCloneStepInfo.getRetry()).isEqualTo(GitCloneStepInfo.DEFAULT_RETRY);
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testsClassInitialization() {
    GitCloneStepInfo gitCloneStepInfo =
        new GitCloneStepInfo(null, null, GitCloneStepInfo.DEFAULT_RETRY, null, null, null);
    assertThat(gitCloneStepInfo.getRetry()).isEqualTo(GitCloneStepInfo.DEFAULT_RETRY);
  }
}
