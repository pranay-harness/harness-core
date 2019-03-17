package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ScaleCommandTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    ScaleCommand scaleCommand = client.scale().resource("Deployment/nginx").replicas(2);

    assertEquals("kubectl scale Deployment/nginx --replicas=2", scaleCommand.command());
  }
}
