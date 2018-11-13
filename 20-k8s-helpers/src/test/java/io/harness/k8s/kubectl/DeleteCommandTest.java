package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class DeleteCommandTest {
  @Test
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    DeleteCommand deleteCommand = client.delete().resources("Deployment/nginx ConfigMap/config");

    assertEquals("kubectl delete Deployment/nginx ConfigMap/config", deleteCommand.command());
  }
}
