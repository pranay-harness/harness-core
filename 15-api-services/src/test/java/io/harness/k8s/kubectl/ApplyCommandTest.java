package io.harness.k8s.kubectl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApplyCommandTest {
  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").dryrun(true).record(true).output("yaml");

    assertEquals("kubectl apply --filename=manifests.yaml --dry-run --record --output=yaml", applyCommand.command());
  }

  @Test
  @Category(UnitTests.class)
  public void kubectlPathTest() {
    Kubectl client = Kubectl.client("/usr/bin/kubectl", null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml");

    assertEquals("/usr/bin/kubectl apply --filename=manifests.yaml", applyCommand.command());

    client = Kubectl.client("C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe", null);

    applyCommand = client.apply().filename("manifests.yaml");

    assertEquals("\"C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe\" apply --filename=manifests.yaml",
        applyCommand.command());
  }

  @Test
  @Category(UnitTests.class)
  public void kubeconfigPathTest() {
    Kubectl client = Kubectl.client("", "config");

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml");

    assertEquals("kubectl --kubeconfig=config apply --filename=manifests.yaml", applyCommand.command());

    client = Kubectl.client("", "c:\\config files\\.kubeconfig");

    applyCommand = client.apply().filename("manifests.yaml");

    assertEquals("kubectl --kubeconfig=\"c:\\config files\\.kubeconfig\" apply --filename=manifests.yaml",
        applyCommand.command());
  }

  @Test
  @Category(UnitTests.class)
  public void testDryRun() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").dryrun(true).output("yaml");

    assertTrue(applyCommand.command().contains("--dry-run"));

    applyCommand.dryrun(false);
    assertFalse(applyCommand.command().contains("--dry-run"));
  }

  @Test
  @Category(UnitTests.class)
  public void testRecord() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").record(true).output("yaml");

    assertTrue(applyCommand.command().contains("--record"));

    applyCommand.record(false);
    assertFalse(applyCommand.command().contains("--record"));
  }
}
