package io.harness.k8s.kubectl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RolloutUndoCommandTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    RolloutUndoCommand rolloutUndoCommand = client.rollout().undo().resource("Deployment/nginx").toRevision("3");

    assertThat(rolloutUndoCommand.command()).isEqualTo("kubectl rollout undo Deployment/nginx --to-revision=3");
  }
}
