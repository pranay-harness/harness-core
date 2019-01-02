package software.wings.service.impl;

import io.harness.serializer.KryoUtils;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentEvent;

public class DeploymentEventListenerTest extends WingsBaseTest {
  @Test
  public void shouldClone() {
    KryoUtils.clone(DeploymentEvent.builder().build());
  }
}
