package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RolloutUndoCommandTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    RolloutUndoCommand rolloutUndoCommand = client.rollout().undo().resource("Deployment/nginx").toRevision("3");

    assertThat(rolloutUndoCommand.command()).isEqualTo("kubectl rollout undo Deployment/nginx --to-revision=3");
  }
}
