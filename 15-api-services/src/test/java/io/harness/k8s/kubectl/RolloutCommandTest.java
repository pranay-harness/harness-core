package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RolloutCommandTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void smokeTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    RolloutCommand rolloutCommand = client.rollout();

    assertThat(rolloutCommand.command()).isEqualTo("kubectl rollout ");
  }
}
