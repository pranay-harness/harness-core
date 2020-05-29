package io.harness.beans.steps;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.CIBeansTest;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StepInfoCreationTest extends CIBeansTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testsBuilderDefaultValues() {
    GitCloneStepInfo gitCloneStepInfo = GitCloneStepInfo.builder().build();
    assertThat(gitCloneStepInfo.getRetry()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_RETRY);
    assertThat(gitCloneStepInfo.getTimeout()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_TIMEOUT);
    assertThat(gitCloneStepInfo.getStepMetadata().getRetry()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_RETRY);
    assertThat(gitCloneStepInfo.getStepMetadata().getTimeout()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_TIMEOUT);
    assertThat(gitCloneStepInfo.getStepMetadata().getUuid()).isNotEmpty();
  }
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testsClassInitialization() {
    GitCloneStepInfo gitCloneStepInfo = new GitCloneStepInfo();
    assertThat(gitCloneStepInfo.getRetry()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_RETRY);
    assertThat(gitCloneStepInfo.getTimeout()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_TIMEOUT);
    assertThat(gitCloneStepInfo.getStepMetadata().getRetry()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_RETRY);
    assertThat(gitCloneStepInfo.getStepMetadata().getTimeout()).isEqualTo(AbstractStepWithMetaInfo.DEFAULT_TIMEOUT);
    assertThat(gitCloneStepInfo.getStepMetadata().getUuid()).isNotEmpty();
  }
}