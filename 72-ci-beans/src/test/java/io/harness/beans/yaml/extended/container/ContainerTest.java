package io.harness.beans.yaml.extended.container;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ContainerTest extends CategoryTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void TestBuilderDefaults() {
    final Container container = Container.builder().resources(Container.Resources.builder().build()).build();

    assertThat(container.getResources().getLimitMemoryMiB()).isEqualTo(900);

    final RunStepInfo runStepInfo = RunStepInfo.builder().build();
    assertThat(runStepInfo.isRunInBackground()).isFalse();
  }
}