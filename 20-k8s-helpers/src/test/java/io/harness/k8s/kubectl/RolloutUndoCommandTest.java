package io.harness.k8s.kubectl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

public class RolloutUndoCommandTest {
  @Test
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    RolloutUndoCommand rolloutUndoCommand = client.rollout().undo().resource("Deployment/nginx").toRevision("3");

    assertEquals("kubectl rollout undo Deployment/nginx --to-revision=3", rolloutUndoCommand.command());
  }
}
