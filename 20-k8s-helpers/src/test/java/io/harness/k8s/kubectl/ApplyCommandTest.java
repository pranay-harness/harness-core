package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

public class ApplyCommandTest {
  @Test
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").dryrun(true).record(true).output("yaml");

    assertEquals("kubectl apply --filename=manifests.yaml --dry-run --record --output=yaml", applyCommand.command());
  }

  @Test
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
  public void testDryRun() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").dryrun(true).output("yaml");

    assertTrue(applyCommand.command().contains("--dry-run"));

    applyCommand.dryrun(false);
    assertFalse(applyCommand.command().contains("--dry-run"));
  }

  @Test
  public void testRecord() {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").record(true).output("yaml");

    assertTrue(applyCommand.command().contains("--record"));

    applyCommand.record(false);
    assertFalse(applyCommand.command().contains("--record"));
  }
}
