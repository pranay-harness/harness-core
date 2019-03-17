package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RolloutCommandTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    RolloutCommand rolloutCommand = client.rollout();

    assertEquals("kubectl rollout ", rolloutCommand.command());
  }
}
