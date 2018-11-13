package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class RolloutHistoryCommandTest {
  @Test
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    RolloutHistoryCommand rolloutHistoryCommand = client.rollout().history().resource("Deployment/nginx");

    assertEquals("kubectl rollout history Deployment/nginx", rolloutHistoryCommand.command());
  }
}
