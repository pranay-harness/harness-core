package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ScaleCommandTest {
  @Test
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    ScaleCommand scaleCommand = client.scale().resource("Deployment/nginx").replicas(2);

    assertEquals("kubectl scale Deployment/nginx --replicas=2", scaleCommand.command());
  }
}
