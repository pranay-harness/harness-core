package io.harness.kubectl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.junit.Test;

public class ApplyCommandTest {
  @Test
  public void smokeTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand =
        client.apply().filename("manifests.yaml").dryrun(true).record(true).output(OutputFormat.yaml);

    assertEquals("kubectl apply --filename=manifests.yaml --dry-run --record --output=yaml", applyCommand.command());
  }

  @Test
  public void testDryRun() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").dryrun(true).output(OutputFormat.yaml);

    assertTrue(applyCommand.command().contains("--dry-run"));

    applyCommand.dryrun(false);
    assertFalse(applyCommand.command().contains("--dry-run"));
  }

  @Test
  public void testRecord() throws Exception {
    Kubectl client = Kubectl.client(null, null);

    ApplyCommand applyCommand = client.apply().filename("manifests.yaml").record(true).output(OutputFormat.yaml);

    assertTrue(applyCommand.command().contains("--record"));

    applyCommand.record(false);
    assertFalse(applyCommand.command().contains("--record"));
  }
}
