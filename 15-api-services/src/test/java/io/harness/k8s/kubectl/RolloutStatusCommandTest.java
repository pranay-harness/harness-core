package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RolloutStatusCommandTest {
  @Test
  public void smokeTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    RolloutStatusCommand rolloutStatusCommand = client.rollout().status().resource("Deployment/nginx").watch(true);

    assertEquals("kubectl rollout status Deployment/nginx --watch=true", rolloutStatusCommand.command());
  }

  @Test
  public void watchFalseTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    RolloutStatusCommand rolloutStatusCommand = client.rollout().status().resource("Deployment/nginx").watch(false);

    assertEquals("kubectl rollout status Deployment/nginx --watch=false", rolloutStatusCommand.command());
  }
}
