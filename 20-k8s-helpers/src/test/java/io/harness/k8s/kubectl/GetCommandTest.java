package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class GetCommandTest {
  @Test
  public void testAllResources() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("all");

    assertEquals("kubectl get all", getCommand.command());
  }

  @Test
  public void testAllPodsInNamespace() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources(ResourceType.pods.toString()).namespace("default").output("yaml");

    assertEquals("kubectl get pods --namespace=default --output=yaml", getCommand.command());
  }

  @Test
  public void testSpecificPod() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods/web-0");

    assertEquals("kubectl get pods/web-0", getCommand.command());
  }

  @Test
  public void testAllPodsAndServices() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods,services");

    assertEquals("kubectl get pods,services", getCommand.command());
  }

  @Test
  public void testGetEvents() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("events").namespace("default").watchOnly(true);

    assertEquals("kubectl get events --namespace=default --watch-only", getCommand.command());
  }
}
