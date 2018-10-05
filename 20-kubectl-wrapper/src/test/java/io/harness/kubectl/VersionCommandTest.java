package io.harness.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class VersionCommandTest {
  @Test
  public void smokeTest() throws Exception {
    Kubectl client = Kubectl.client(null, null);
    VersionCommand versionCommand = client.version();

    assertEquals("kubectl version", versionCommand.command());
  }
}
