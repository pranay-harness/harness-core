package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RolloutCommandTest {
  @Test
  public void smokeTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    RolloutCommand rolloutCommand = client.rollout();

    assertEquals("kubectl rollout ", rolloutCommand.command());
  }
}
