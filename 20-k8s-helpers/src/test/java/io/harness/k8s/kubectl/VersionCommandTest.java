package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class VersionCommandTest {
  @Test
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);
    VersionCommand versionCommand = client.version();

    assertEquals("kubectl version", versionCommand.command());
  }
}
