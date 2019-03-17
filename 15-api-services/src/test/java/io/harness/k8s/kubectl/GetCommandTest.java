package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetCommandTest {
  @Test
  @Category(UnitTests.class)
  public void testAllResources() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("all");

    assertEquals("kubectl get all", getCommand.command());
  }

  @Test
  @Category(UnitTests.class)
  public void testAllPodsInNamespace() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources(ResourceType.pods.toString()).namespace("default").output("yaml");

    assertEquals("kubectl get pods --namespace=default --output=yaml", getCommand.command());
  }

  @Test
  @Category(UnitTests.class)
  public void testSpecificPod() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods/web-0");

    assertEquals("kubectl get pods/web-0", getCommand.command());
  }

  @Test
  @Category(UnitTests.class)
  public void testAllPodsAndServices() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods,services");

    assertEquals("kubectl get pods,services", getCommand.command());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetEvents() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("events").namespace("default").watchOnly(true);

    assertEquals("kubectl get events --namespace=default --watch-only", getCommand.command());
  }
}
