package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class GetCommandTest {
  @Test
  public void testAllResources() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("all");

    assertEquals("kubectl get all", getCommand.command());
  }

  @Test
  public void testAllPodsInNamespace() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand =
        client.get().resources(ResourceType.pods.toString()).namespace("default").output(OutputFormat.yaml);

    assertEquals("kubectl get pods --namespace=default --output=yaml", getCommand.command());
  }

  @Test
  public void testSpecificPod() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods/web-0");

    assertEquals("kubectl get pods/web-0", getCommand.command());
  }

  @Test
  public void testAllPodsAndServices() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods,services");

    assertEquals("kubectl get pods,services", getCommand.command());
  }
}
