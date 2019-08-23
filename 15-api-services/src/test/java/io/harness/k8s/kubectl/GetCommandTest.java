package io.harness.k8s.kubectl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetCommandTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testAllResources() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("all");

    assertThat(getCommand.command()).isEqualTo("kubectl get all");
  }

  @Test
  @Category(UnitTests.class)
  public void testAllPodsInNamespace() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources(ResourceType.pods.toString()).namespace("default").output("yaml");

    assertThat(getCommand.command()).isEqualTo("kubectl get pods --namespace=default --output=yaml");
  }

  @Test
  @Category(UnitTests.class)
  public void testSpecificPod() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods/web-0");

    assertThat(getCommand.command()).isEqualTo("kubectl get pods/web-0");
  }

  @Test
  @Category(UnitTests.class)
  public void testAllPodsAndServices() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("pods,services");

    assertThat(getCommand.command()).isEqualTo("kubectl get pods,services");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetEvents() {
    Kubectl client = Kubectl.client(null, null);

    GetCommand getCommand = client.get().resources("events").namespace("default").watchOnly(true);

    assertThat(getCommand.command()).isEqualTo("kubectl get events --namespace=default --watch-only");
  }
}
