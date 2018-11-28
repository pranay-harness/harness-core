package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class DescribeCommandTest {
  @Test
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);
    DescribeCommand describeCommand = client.describe().filename("manifests.yaml");

    assertEquals("kubectl describe --filename=manifests.yaml", describeCommand.command());
  }
}
